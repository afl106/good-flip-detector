package com.flippingdetector;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("flippingdetector")
public interface FlippingDetectorConfig extends Config
{
    @ConfigItem(
            keyName = "refreshSeconds",
            name = "Refresh interval (sec)",
            description = "How often to refresh live margins",
            position = 1
    )
    default int refreshSeconds() { return 60; }

    @ConfigItem(
            keyName = "minDailyVolume",
            name = "Min daily volume",
            description = "Filter out items traded less than this per day",
            position = 2
    )
    default int minDailyVolume() { return 1000; }

    @ConfigItem(
            keyName = "minMarginPct",
            name = "Min margin %",
            description = "Ignore items with smaller margin than this percentage",
            position = 3
    )
    default double minMarginPct() { return 1.0; }

    @ConfigItem(
            keyName = "maxVolatilityPct",
            name = "Max volatility % (24h)",
            description = "Ignore items with higher 24h volatility than this",
            position = 4
    )
    default double maxVolatilityPct() { return 20.0; }

    @ConfigItem(
            keyName = "excludeLowVolume",
            name = "Exclude low volume items",
            description = "Skip items with very low trade counts",
            position = 5
    )
    default boolean excludeLowVolume() { return true; }

    @ConfigItem(
            keyName = "respectBuyLimits",
            name = "Respect buy limits",
            description = "Avoid items where you have reached the 4h buy limit",
            position = 6
    )
    default boolean respectBuyLimits() { return true; }

    @ConfigItem(
            keyName = "evenlyAllocate",
            name = "Evenly allocate GP across open slots",
            description = "Split inventory GP evenly across your available GE slots",
            position = 7
    )
    default boolean evenlyAllocate() { return true; }

    @ConfigItem(
            keyName = "maxCandidates",
            name = "Max candidates (internal)",
            description = "Hard cap on items to consider per refresh (safety)",
            position = 8
    )
    default int maxCandidates() { return 2000; }
}
