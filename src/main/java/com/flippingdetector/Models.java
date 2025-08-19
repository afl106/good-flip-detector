package com.flippingdetector;

import lombok.Value;

@Value
class FlipCandidate
{
    int itemId;
    String name;
    long buyPrice;
    long sellPrice;
    int quantity;
    long expectedProfit;
    int dailyVolume;
    int buyLimit;
    double volatilityPct;
}
