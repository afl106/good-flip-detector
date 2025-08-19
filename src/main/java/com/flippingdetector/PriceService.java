package com.flippingdetector;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Singleton
class PriceService
{
    private static final String LATEST_URL = "https://prices.runescape.wiki/api/v1/osrs/latest";
    private static final String MAPPING_URL = "https://prices.runescape.wiki/api/v1/osrs/mapping";

    private final OkHttpClient http;
    private final Gson gson = new Gson();
    private final ItemManager itemManager;
    private final Client client;

    private volatile MarketSnapshot snapshot;

    @Inject
    PriceService(OkHttpClient http, ItemManager itemManager, Client client)
    {
        this.http = http;
        this.itemManager = itemManager;
        this.client = client;
    }

    void start()
    {
        refresh();
    }

    void stop()
    {
        snapshot = null;
    }

    MarketSnapshot getSnapshot()
    {
        if (snapshot == null)
        {
            refresh();
        }
        return snapshot;
    }

    Optional<Long> estimateInventoryCoins(Client client)
    {
        try
        {
            ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
            if (inv == null) return Optional.empty();
            long coins = 0;
            for (Item it : inv.getItems())
            {
                if (it.getId() == 995) // coins id
                {
                    coins += it.getQuantity();
                }
            }
            return Optional.of(coins);
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    private void refresh()
    {
        try
        {
            Map<Integer, ItemInfo> items = new HashMap<>();

            // Fetch latest highs/lows
            JsonObject latest = getJson(LATEST_URL).getAsJsonObject("data");
            // Fetch mapping to get names and buy limits
            // mapping endpoint returns an array; we build id->(name,limit) map
            Map<Integer, MapMeta> meta = new HashMap<>();
            for (JsonElement e : getJsonArray(MAPPING_URL))
            {
                JsonObject o = e.getAsJsonObject();
                int id = o.get("id").getAsInt();
                String name = o.get("name").getAsString();
                int limit = o.has("limit") && !o.get("limit").isJsonNull() ? o.get("limit").getAsInt() : 0;
                meta.put(id, new MapMeta(name, limit));
            }

            int count = 0;
            for (Map.Entry<String, JsonElement> en : latest.entrySet())
            {
                if (count++ > 50000) break; // hard cap
                int id = Integer.parseInt(en.getKey());
                JsonObject o = en.getValue().getAsJsonObject();
                long high = o.has("high") && !o.get("high").isJsonNull() ? o.get("high").getAsLong() : 0;
                long low = o.has("low") && !o.get("low").isJsonNull() ? o.get("low").getAsLong() : 0;
                // daily volume is not present here; we approximate via highTime/lowTime recency
                int dailyVolume = o.has("highTime") && o.has("lowTime") ? 2000 : 0; // placeholder heuristic

                MapMeta m = meta.get(id);
                if (m == null) continue;

                ItemInfo info = new ItemInfo(id, m.name, low, high, dailyVolume, m.limit, 10.0);
                items.put(id, info);
            }

            snapshot = new MarketSnapshot(new ArrayList<>(items.values()));
        }
        catch (Exception e)
        {
            log.warn("Failed to refresh prices", e);
        }
    }

    private JsonObject getJson(String url) throws IOException
    {
        Request req = new Request.Builder().url(url).header("User-Agent", "FlippingDetector/1.0 (RuneLite plugin)").build();
        try (Response resp = http.newCall(req).execute())
        {
            if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code());
            return gson.fromJson(resp.body().charStream(), JsonObject.class);
        }
    }

    private Iterable<JsonElement> getJsonArray(String url) throws IOException
    {
        Request req = new Request.Builder().url(url).header("User-Agent", "FlippingDetector/1.0 (RuneLite plugin)").build();
        try (Response resp = http.newCall(req).execute())
        {
            if (!resp.isSuccessful()) throw new IOException("HTTP " + resp.code());
            JsonElement parsed = gson.fromJson(resp.body().charStream(), JsonElement.class);
            return parsed.getAsJsonArray();
        }
    }

    @Value
    static class MapMeta { String name; int limit; }
}
