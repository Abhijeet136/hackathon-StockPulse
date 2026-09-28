package com.stockpulse.service;

import com.stockpulse.domain.*;
import com.stockpulse.exception.ConflictException;
import com.stockpulse.exception.NotFoundException;
import com.stockpulse.repository.PricingSuggestionRepository;
import com.stockpulse.repository.ProductRepository;
import com.stockpulse.repository.ReorderSuggestionRepository;
import com.stockpulse.strategy.PricingResult;
import com.stockpulse.strategy.ReorderResult;
import com.stockpulse.strategy.StrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Coordinates suggestion generation (shared by async listeners and manual endpoints) and human decisions.
 */
@Service
public class SuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);
    private static final int MAX_WRITE_ATTEMPTS = 3;

    private final ProductRepository products;
    private final PricingSuggestionRepository pricingRepo;
    private final ReorderSuggestionRepository reorderRepo;
    private final StrategyRegistry registry;
    private final ContextFactory contexts;
    private final SuggestionWriter writer;
    private final TransactionTemplate readOnlyTx;

    public SuggestionService(ProductRepository products, PricingSuggestionRepository pricingRepo,
                             ReorderSuggestionRepository reorderRepo, StrategyRegistry registry,
                             ContextFactory contexts, SuggestionWriter writer, TransactionTemplate transactionTemplate) {
        this.products = products;
        this.pricingRepo = pricingRepo;
        this.reorderRepo = reorderRepo;
        this.registry = registry;
        this.contexts = contexts;
        this.writer = writer;
        this.readOnlyTx = new TransactionTemplate(transactionTemplate.getTransactionManager());
        this.readOnlyTx.setReadOnly(true);
    }

    /** Signal-driven path: both a pricing and a reorder suggestion, each deduplicated independently. */
    public void generate(Long productId, TriggerReason reason) {
        generatePricing(productId, reason);
        generateReorder(productId, reason);
    }

    public Optional<PricingSuggestion> generatePricing(Long productId, TriggerReason reason) {
        Snapshot<com.stockpulse.strategy.PricingContext> snap = readOnlyTx.execute(tx -> {
            Product p = find(productId);
            boolean pending = pricingRepo.existsByProductIdAndTriggerReasonAndStatus(productId, reason, SuggestionStatus.PENDING);
            return new Snapshot<>(p.getSku(), pending, pending ? null : contexts.pricing(p, reason));
        });
        if (snap.alreadyPending()) {
            log.info("Skip {} pricing for {}: already PENDING", reason, snap.sku());
            return Optional.empty();
        }
        PricingResult result = registry.activePricing().suggest(snap.context());
        Optional<PricingSuggestion> saved = writeSafely(() -> writer.savePricing(productId, reason, result));
        saved.ifPresent(s -> log.info("Saved {} pricing suggestion for {}: {} -> {} ({}, source {})", reason,
                snap.sku(), s.getCurrentPrice(), s.getRecommendedPrice(), s.getDirection(), s.getSource()));
        return saved;
    }

    public Optional<ReorderSuggestion> generateReorder(Long productId, TriggerReason reason) {
        Snapshot<com.stockpulse.strategy.ReorderContext> snap = readOnlyTx.execute(tx -> {
            Product p = find(productId);
            boolean pending = reorderRepo.existsByProductIdAndTriggerReasonAndStatus(productId, reason, SuggestionStatus.PENDING);
            return new Snapshot<>(p.getSku(), pending, pending ? null : contexts.reorder(p, reason));
        });
        if (snap.alreadyPending()) {
            log.info("Skip {} reorder for {}: already PENDING", reason, snap.sku());
            return Optional.empty();
        }
        ReorderResult result = registry.activeReorder().suggest(snap.context());
        Optional<ReorderSuggestion> saved = writeSafely(() -> writer.saveReorder(productId, reason, result));
        saved.ifPresent(s -> log.info("Saved {} reorder suggestion for {}: +{} units (source {})", reason,
                snap.sku(), s.getRecommendedQuantity(), s.getSource()));
        return saved;
    }

    /** Manual endpoints: synchronous, 409 if an identical MANUAL suggestion is still pending. */
    public PricingSuggestion requestPricing(Long productId) {
        return generatePricing(productId, TriggerReason.MANUAL).orElseThrow(() ->
                new ConflictException("A MANUAL pricing suggestion for product " + productId + " is already pending"));
    }

    public ReorderSuggestion requestReorder(Long productId) {
        return generateReorder(productId, TriggerReason.MANUAL).orElseThrow(() ->
                new ConflictException("A MANUAL reorder suggestion for product " + productId + " is already pending"));
    }

    @Transactional(readOnly = true)
    public List<PricingSuggestion> pricingSuggestions(SuggestionStatus status) {
        return status == null ? pricingRepo.findAllByOrderByCreatedAtDesc() : pricingRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public List<ReorderSuggestion> reorderSuggestions(SuggestionStatus status) {
        return status == null ? reorderRepo.findAllByOrderByCreatedAtDesc() : reorderRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    /** Human checkpoint. Accept applies the change; status and value change in one transaction. */
    @Transactional
    public PricingSuggestion decidePricing(Long id, SuggestionStatus decision) {
        PricingSuggestion s = pricingRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Pricing suggestion " + id + " not found"));
        Product p = s.getProduct();
        switch (decision) {
            case ACCEPTED -> {
                s.accept();
                p.updatePrice(s.getRecommendedPrice());
            }
            case REJECTED -> s.reject();
            default -> throw new IllegalArgumentException("status must be ACCEPTED or REJECTED");
        }
        pricingRepo.flush();
        p.recomputeStatus(pricingRepo.existsByProductIdAndStatus(p.getId(), SuggestionStatus.PENDING));
        log.info("Pricing suggestion {} for {} {}", id, p.getSku(), decision);
        return s;
    }

    @Transactional
    public ReorderSuggestion decideReorder(Long id, SuggestionStatus decision) {
        ReorderSuggestion s = reorderRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Reorder suggestion " + id + " not found"));
        Product p = s.getProduct();
        switch (decision) {
            case ACCEPTED -> {
                s.accept();
                p.addStock(s.getRecommendedQuantity()); // simulated shipment
            }
            case REJECTED -> s.reject();
            default -> throw new IllegalArgumentException("status must be ACCEPTED or REJECTED");
        }
        p.recomputeStatus(pricingRepo.existsByProductIdAndStatus(p.getId(), SuggestionStatus.PENDING));
        log.info("Reorder suggestion {} for {} {}", id, p.getSku(), decision);
        return s;
    }

    /** Duplicate = skip; concurrent product update = retry the short write. */
    private <T> Optional<T> writeSafely(Supplier<Optional<T>> write) {
        for (int attempt = 1; ; attempt++) {
            try {
                return write.get();
            } catch (DataIntegrityViolationException e) {
                log.info("Duplicate suggestion blocked by unique dedupe key");
                return Optional.empty();
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt >= MAX_WRITE_ATTEMPTS) {
                    throw e;
                }
                log.info("Product changed concurrently, retrying suggestion write ({}/{})", attempt, MAX_WRITE_ATTEMPTS);
            }
        }
    }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product " + id + " not found"));
    }

    private record Snapshot<C>(String sku, boolean alreadyPending, C context) {
    }
}
