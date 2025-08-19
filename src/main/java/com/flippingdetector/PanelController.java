package com.flippingdetector;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import lombok.Getter;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.PluginPanel;

@Singleton
class PanelController
{
    private final FlippingDetectorConfig config;
    private final PluginPanel panel = new PluginPanel();
    private final JTextField gpOverride = new JTextField();

    @Inject
    PanelController(FlippingDetectorConfig config)
    {
        this.config = config;
    }

    void init()
    {
        panel.setLayout(new BorderLayout());

        JPanel top = new JPanel(new GridLayout(0, 2, 6, 6));
        top.add(new JLabel("Manual GP Override:"));
        top.add(gpOverride);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JLabel("Suggestions will appear here..."), BorderLayout.CENTER);
    }

    NavigationButton buildNavButton(ClientToolbar toolbar)
    {
        return NavigationButton.builder()
                .tooltip("GE Flipper")
                .priority(7)
                .panel(panel)
                .build();
    }

    void showSuggestions(List<FlipCandidate> picks, long userGp, int openSlots)
    {
        JPanel list = new JPanel(new GridLayout(Math.max(1, picks.size() + 1), 1, 4, 4));
        list.add(new JLabel(String.format("Open slots: %d • GP considered: %,d", openSlots, userGp)));
        for (FlipCandidate c : picks)
        {
            list.add(new JLabel(String.format("%s (ID %d): buy %,d x %d → sell %,d | est. profit %,d | vol %d/day | limit %d",
                    c.getName(), c.getItemId(), c.getBuyPrice(), c.getQuantity(), c.getSellPrice(), c.getExpectedProfit(),
                    c.getDailyVolume(), c.getBuyLimit())));
        }
        panel.removeAll();
        panel.setLayout(new BorderLayout());
        panel.add(list, BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
    }

    void updateOffers(Map<Integer, OfferTracker.OfferRecord> offers)
    {
        // Could render offers in a subpanel; keeping minimal for brevity.
    }

    Optional<Long> getManualGpOverride()
    {
        try
        {
            String txt = gpOverride.getText();
            if (txt == null || txt.isBlank()) return Optional.empty();
            return Optional.of(Long.parseLong(txt.replaceAll("[_, ]", "")));
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }
}
