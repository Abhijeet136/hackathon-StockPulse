package com.stockpulse.strategy;

public interface ReorderStrategy {
    ReorderResult suggest(ReorderContext ctx);
}
