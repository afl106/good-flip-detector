package com.flippingdetector;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GrandExchangeChanged;

@Singleton
class OfferTracker
{
    private static final Duration WINDOW = Duration.ofHours(4);

    private final Client client;

    private final Map<Integer, OfferRecord> bySlot = new HashMap<>();
    private final Map<Integer, RollingBuy> rolling = new HashMap<>(); // itemId -> buys in window

    @Inject
    OfferTracker(Client client)
    {
        this.client = client;
    }

    void start() {}

    void stop()
    {
        bySlot.clear();
        rolling.clear();
    }

    void onGEChange(GrandExchangeChanged ev)
    {
        int slot = ev.getSlot();
        GrandExchangeOffer of = ev.getOffer();
        bySlot.put(slot, new OfferRecord(slot, of.getItemId(), of.getState(), of.getQuantitySold(), of.getTotalQuantity(), of.getPrice(), Instant.now()));

        if (of.getState() == GrandExchangeOfferState.BOUGHT || of.getState() == GrandExchangeOfferState.BUYING)
        {
            if (of.getQuantitySold() > 0)
            {
                RollingBuy rb = rolling.getOrDefault(of.getItemId(), new RollingBuy(0, Instant.now()));
                int newQty = Math.min(Integer.MAX_VALUE, rb.qty + of.getQuantitySold());
                rolling.put(of.getItemId(), new RollingBuy(newQty, Instant.now()));
            }
        }

        // Expire rolling buys outside the 4h window
        rolling.entrySet().removeIf(e -> Duration.between(e.getValue().lastUpdate, Instant.now()).compareTo(WINDOW) > 0);
    }

    int boughtInWindow(int itemId)
    {
        RollingBuy rb = rolling.get(itemId);
        return rb == null ? 0 : rb.qty;
    }

    boolean isInActiveOffer(int itemId)
    {
        return bySlot.values().stream()
                .anyMatch(r -> r.itemId == itemId && (r.state == GrandExchangeOfferState.BUYING || r.state == GrandExchangeOfferState.SELLING));
    }

    int filledSlotCount()
    {
        int c = 0;
        for (OfferRecord r : bySlot.values())
        {
            if (r.state == GrandExchangeOfferState.BUYING || r.state == GrandExchangeOfferState.SELLING)
                c++;
        }
        return c;
    }

    Map<Integer, OfferRecord> currentOffers()
    {
        return new HashMap<>(bySlot);
    }

    @Value
    static class OfferRecord
    {
        int slot;
        int itemId;
        GrandExchangeOfferState state;
        int quantityTraded;
        int quantityTotal;
        int priceEach;
        Instant timestamp;
    }

    @Value
    static class RollingBuy
    {
        int qty;
        Instant lastUpdate;
    }
}
