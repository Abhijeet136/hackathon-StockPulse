package com.stockpulse.repository;

import com.stockpulse.domain.ReorderSuggestion;
import com.stockpulse.domain.SuggestionStatus;
import com.stockpulse.domain.TriggerReason;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReorderSuggestionRepository extends JpaRepository<ReorderSuggestion, Long> {

    @EntityGraph(attributePaths = "product")
    List<ReorderSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status);

    @EntityGraph(attributePaths = "product")
    List<ReorderSuggestion> findAllByOrderByCreatedAtDesc();

    boolean existsByProductIdAndTriggerReasonAndStatus(Long productId, TriggerReason reason, SuggestionStatus status);
}
