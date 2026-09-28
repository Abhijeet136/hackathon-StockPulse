package com.stockpulse.repository;

import com.stockpulse.domain.PricingSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PricingSuggestionRepository extends JpaRepository<PricingSuggestion, Long> {

    @EntityGraph(attributePaths = "product")
    List<PricingSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status);

    @EntityGraph(attributePaths = "product")
    List<PricingSuggestion> findAllByOrderByCreatedAtDesc();

    boolean existsByProductIdAndTriggerReasonAndStatus(Long productId, TriggerReason reason, SuggestionStatus status);

    boolean existsByProductIdAndStatus(Long productId, SuggestionStatus status);
}
