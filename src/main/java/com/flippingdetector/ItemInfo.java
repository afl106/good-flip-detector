package com.flippingdetector;

import lombok.Value;

@Value
class ItemInfo
{
    int itemId;
    String name;
    long latestLow;
    long latestHigh;
    int dailyVolume;
    int buyLimit;
    double volatilityPct;

    long latestLow() { return latestLow; }
    long latestHigh() { return latestHigh; }
    int dailyVolume() { return dailyVolume; }
    int buyLimit() { return buyLimit; }
    double volatilityPct() { return volatilityPct; }
}
