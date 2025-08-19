package com.flippingdetector;

import java.util.Optional;

final class FlipMath
{
    static Optional<FlipCandidate> makeCandidate(ItemInfo info, long perSlotBudget, FlippingDetectorConfig cfg)
    {
        long buyPrice = info.latestLow();  // assume we buy at current low
        long sellPrice = info.latestHigh(); // and sell at current high
        if (sellPrice <= buyPrice) return Optional.empty();

        // How many can we afford with this per-slot budget?
        long qtyAffordable = perSlotBudget > 0 ? Math.max(0, perSlotBudget / Math.max(1, buyPrice)) : 0;
        if (qtyAffordable <= 0) return Optional.empty();

        // Respect buy limit
        int qty = (int)Math.min(qtyAffordable, info.buyLimit());
        if (qty <= 0) return Optional.empty();

        long marginEach = sellPrice - buyPrice;
        long expectedProfit = marginEach * qty;

        return Optional.of(new FlipCandidate(
                info.itemId(),
                info.name(),
                buyPrice,
                sellPrice,
                qty,
                expectedProfit,
                info.dailyVolume(),
                info.buyLimit(),
                info.volatilityPct()
        ));
    }
}
