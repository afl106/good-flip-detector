package com.flippingdetector;

import java.util.List;
import lombok.Value;

@Value
class MarketSnapshot
{
    List<ItemInfo> items;
}
