package com.stockpulse.event;

import com.stockpulse.config.AsyncConfig;
import com.stockpulse.domain.TriggerReason;
import com.stockpulse.service.SuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InventoryLowListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryLowListener.class);

    private final SuggestionService suggestions;

    public InventoryLowListener(SuggestionService suggestions) {
        this.suggestions = suggestions;
    }

    @Async(AsyncConfig.ADVISOR_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(InventoryLowEvent event) {
        log.info("INVENTORY_LOW received for product {}", event.productId());
        try {
            suggestions.generate(event.productId(), TriggerReason.INVENTORY_LOW);
        } catch (Exception e) {
            log.error("INVENTORY_LOW handling failed for product {}", event.productId(), e);
        }
    }
}
