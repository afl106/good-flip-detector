package com.flippingdetector;

import java.util.Objects;

final class Filters
{
    static boolean passes(ItemInfo info, FlippingDetectorConfig cfg, OfferTracker tracker)
    {
        if (info == null) return false;
        if (info.latestHigh() <= 0 || info.latestLow() <= 0) return false;

        // Margin must be positive and above threshold
        double marginPct = 100.0 * (info.latestHigh() - info.latestLow()) / Math.max(1.0, info.latestHigh());
        if (marginPct < cfg.minMarginPct()) return false;

        // Daily volume threshold
        if (cfg.excludeLowVolume() && info.dailyVolume() < cfg.minDailyVolume()) return false;

        // Volatility filter
        if (info.volatilityPct() > cfg.maxVolatilityPct()) return false;

        // Respect buy limits and your rolling 4h progress
        if (cfg.respectBuyLimits())
        {
            int boughtInWindow = tracker.boughtInWindow(info.itemId());
            if (boughtInWindow >= info.buyLimit())
                return false;
        }

        // Avoid recommending items already in your active offers
        if (tracker.isInActiveOffer(info.itemId()))
            return false;

        return true;
    }
}
