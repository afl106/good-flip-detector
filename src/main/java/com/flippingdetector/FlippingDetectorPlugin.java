package com.flippingdetector;

import com.google.inject.Provides;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.events.GrandExchangeChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

@Slf4j
@PluginDescriptor(
        name = "GE Flipper",
        description = "Suggests optimal GE flipping items and monitors your 8 GE slots",
        tags = {"ge", "flipping", "prices", "trading"}
)
public class FlippingDetectorPlugin extends Plugin
{
    static final int GE_SLOTS = 8;

    @Inject private Client client;
    @Inject private FlippingDetectorConfig config;
    @Inject private PriceService priceService;
    @Inject private OfferTracker offerTracker;
    @Inject private PanelController panelController;

    @Inject private ClientToolbar clientToolbar;

    private NavigationButton navButton;
    private Instant lastRefresh = Instant.EPOCH;

    @Provides
    FlippingDetectorConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(FlippingDetectorConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        panelController.init();
        navButton = panelController.buildNavButton(clientToolbar);
        clientToolbar.addNavigation(navButton);
        priceService.start();
        offerTracker.start();
        log.info("GE Flipper started");
    }

    @Override
    protected void shutDown() throws Exception
    {
        priceService.stop();
        offerTracker.stop();
        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
        }
        log.info("GE Flipper stopped");
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        int refreshSec = Math.max(15, config.refreshSeconds());
        if (Duration.between(lastRefresh, Instant.now()).getSeconds() >= refreshSec)
        {
            refreshSuggestions();
            lastRefresh = Instant.now();
        }
    }

    @Subscribe
    public void onGrandExchangeChanged(GrandExchangeChanged ev)
    {
        offerTracker.onGEChange(ev);
        panelController.updateOffers(offerTracker.currentOffers());
        // Mark suggestion list stale so we re-evaluate next tick
        lastRefresh = Instant.EPOCH;
    }

    private void refreshSuggestions()
    {
        try
        {
            // Pull fresh snapshot
            MarketSnapshot snapshot = priceService.getSnapshot();
            if (snapshot == null)
            {
                return;
            }

            // Determine available GP: user input in panel + coins in inventory (best effort)
            long userGp = panelController.getManualGpOverride().orElse(0L);
            if (userGp <= 0)
            {
                userGp = priceService.estimateInventoryCoins(client).orElse(0L);
            }

            // Compute open slots
            int openSlots = GE_SLOTS - offerTracker.filledSlotCount();
            openSlots = Math.max(0, Math.min(GE_SLOTS, openSlots));
            if (openSlots == 0)
            {
                panelController.showSuggestions(List.of(), userGp, 0);
                return;
            }

            long perSlotBudget = config.evenlyAllocate() && userGp > 0 ? userGp / openSlots : userGp;

            // Filter and rank candidates
            List<FlipCandidate> candidates = new ArrayList<>();
            for (ItemInfo info : snapshot.items())
            {
                if (!Filters.passes(info, config, offerTracker))
                    continue;
                Optional<FlipCandidate> cand = FlipMath.makeCandidate(info, perSlotBudget, config);
                cand.ifPresent(candidates::add);
            }

            // Sort by expected profit per slot (descending)
            candidates.sort(Comparator.comparing(FlipCandidate::expectedProfit).reversed());

            // Take top 'openSlots' unique items
            List<FlipCandidate> picks = new ArrayList<>();
            for (FlipCandidate c : candidates)
            {
                if (picks.size() >= openSlots) break;
                picks.add(c);
            }

            panelController.showSuggestions(picks, userGp, openSlots);
        }
        catch (Exception e)
        {
            log.warn("Failed to refresh suggestions", e);
        }
    }
}
