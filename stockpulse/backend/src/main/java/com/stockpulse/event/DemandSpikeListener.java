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
public class DemandSpikeListener {

    private static final Logger log = LoggerFactory.getLogger(DemandSpikeListener.class);

    private final SuggestionService suggestions;

    public DemandSpikeListener(SuggestionService suggestions) {
        this.suggestions = suggestions;
    }

    @Async(AsyncConfig.ADVISOR_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(DemandSpikeEvent event) {
        log.info("DEMAND_SPIKE received for product {}", event.productId());
        try {
            suggestions.generate(event.productId(), TriggerReason.DEMAND_SPIKE);
        } catch (Exception e) {
            log.error("DEMAND_SPIKE handling failed for product {}", event.productId(), e);
        }
    }
}
