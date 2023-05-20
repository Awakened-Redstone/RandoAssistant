package com.bawnorton.randoassistant.tracking;

import com.bawnorton.randoassistant.RandoAssistant;
import com.bawnorton.randoassistant.config.Config;
import com.bawnorton.randoassistant.networking.SerializeableCrafting;
import com.bawnorton.randoassistant.networking.SerializeableInteraction;
import com.bawnorton.randoassistant.networking.SerializeableLootTable;
import com.bawnorton.randoassistant.networking.client.Networking;
import com.bawnorton.randoassistant.screen.LootBookWidget;
import com.bawnorton.randoassistant.stat.RandoAssistantStats;
import com.bawnorton.randoassistant.tracking.trackable.Trackable;
import com.bawnorton.randoassistant.tracking.trackable.TrackableCrawler;
import com.bawnorton.randoassistant.util.LootAdvancement;
import com.bawnorton.randoassistant.util.LootCondition;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.minecraft.block.Block;
import net.minecraft.block.CandleBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.recipe.Recipe;
import net.minecraft.registry.Registries;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class Tracker {
    private static Tracker INSTANCE;

    private final TrackableMap<Identifier> TRACKABLE_INTERACTED;
    private final TrackableMap<Identifier> TRACKABLE_LOOTED;
    private final Map<Recipe<?>, Item> TRACKABLE_CRAFTED;

    private final Map<Identifier, Set<Trackable<Identifier>>> TRACKED;
    private final Map<Identifier, Trackable<Identifier>> ID_TO_TRACKABLE;

    public Tracker() {
        TRACKABLE_INTERACTED = new TrackableMap<>();
        TRACKABLE_LOOTED = new TrackableMap<>();
        TRACKABLE_CRAFTED = Maps.newHashMap();
        TRACKED = Maps.newHashMap();
        ID_TO_TRACKABLE = Maps.newHashMap();
    }

    public static Tracker getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new Tracker();
        }
        return INSTANCE;
    }

    public void track(SerializeableLootTable lootTable) {
        Stat<Identifier> stat = RandoAssistantStats.LOOTED.getOrCreateStat(lootTable.getLootTableId());
        Trackable<Identifier> trackable = TRACKABLE_LOOTED.getOrCreate(stat, lootTable.getSourceId());
        ID_TO_TRACKABLE.put(lootTable.getSourceId(), trackable);
        for(Item target : lootTable.getItems()) {
            trackable.addOutput(Registries.ITEM.getId(target), lootTable.getCondition());
            Set<Trackable<Identifier>> tracked = TRACKED.getOrDefault(Registries.ITEM.getId(target), Sets.newHashSet());
            tracked.add(trackable);
            TRACKED.put(Registries.ITEM.getId(target), tracked);
        }
    }
    
    public void track(SerializeableInteraction interaction) {
        Stat<Identifier> stat = RandoAssistantStats.INTERACTED.getOrCreateStat(Registries.BLOCK.getId(interaction.getInput()));
        Trackable<Identifier> trackable = TRACKABLE_INTERACTED.getOrCreate(stat, Registries.BLOCK.getId(interaction.getInput()));
        trackable.addOutput(Registries.BLOCK.getId(interaction.getOutput()), LootCondition.NONE);
        Set<Trackable<Identifier>> tracked = TRACKED.getOrDefault(Registries.BLOCK.getId(interaction.getOutput()), Sets.newHashSet());
        tracked.add(trackable);
        TRACKED.put(Registries.BLOCK.getId(interaction.getOutput()), tracked);
    }

    public void track(SerializeableCrafting crafting) {
        Recipe<?> recipe = crafting.getInput();
        Item outputItem = crafting.getOutput();
        TRACKABLE_CRAFTED.put(recipe, outputItem);
    }

    @Nullable
    public Set<Trackable<Identifier>> getSources(Identifier id) {
        return TRACKED.get(id);
    }

    public Set<Identifier> getEnabled() {
        Set<Identifier> identifiers = Sets.newHashSet();
        getEnabledInteracted().forEach(trackable -> trackable.getOutput().forEach(entry -> identifiers.add(entry.identifier())));
        getEnabledLooted().forEach(trackable -> trackable.getOutput().forEach(entry -> {
            Identifier identifier = entry.identifier();
            if(entry.requiresSilkTouch()) {
                Block block = Registries.BLOCK.get(trackable.getIdentifier());
                if(hasSilkTouched(block)) identifiers.add(identifier);
            } else identifiers.add(identifier);
        }));
        identifiers.addAll(getEnabledCrafted());
        return identifiers;
    }
    
    private Set<Trackable<Identifier>> getEnabledLooted() {
        return TRACKABLE_LOOTED.getEnabled();
    }

    private Set<Trackable<Identifier>> getEnabledInteracted() {
        return TRACKABLE_INTERACTED.getEnabled();
    }

    private Set<Identifier> getEnabledCrafted() {
        Set<Identifier> crafted = Sets.newHashSet();
        TRACKABLE_CRAFTED.forEach((recipe, item) -> {
            if(hasCrafted(recipe)) crafted.add(Registries.ITEM.getId(item));
        });
        return crafted;
    }

    public boolean hasCrafted(Recipe<?> recipe) {
        if(Config.getInstance().enableOverride) return true;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if(player == null) throw new IllegalStateException("Player is null");
        int count = player.getStatHandler().getStat(RandoAssistantStats.CRAFTED.getOrCreateStat(recipe.getId()));
        return count > 0;
    }

    public boolean hasObtained(Item item) {
        if(Config.getInstance().enableOverride) return true;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if(player == null) throw new IllegalStateException("Player is null");
        int count = player.getStatHandler().getStat(Stats.CRAFTED.getOrCreateStat(item));
        count += player.getStatHandler().getStat(Stats.PICKED_UP.getOrCreateStat(item));
        return count > 0;
    }

    private boolean hasSilkTouched(Block block) {
        if(Config.getInstance().enableOverride) return true;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if(player == null) throw new IllegalStateException("Player is null");
        int count = player.getStatHandler().getStat(RandoAssistantStats.SILK_TOUCHED.getOrCreateStat(block));
        return count > 0;
    }

    public void clear() {
        TRACKABLE_INTERACTED.clear();
        TRACKABLE_CRAFTED.clear();
        TRACKABLE_LOOTED.clear();
        TRACKED.clear();
        clearCache();
    }

    public void clearCache() {
        TrackableCrawler.clearCache();
        LootBookWidget.getInstance().clearCache();
    }

    public void debug(Item item) {
        Set<Trackable<Identifier>> tracked = TRACKED.getOrDefault(Registries.ITEM.getId(item), Sets.newHashSet());
        RandoAssistant.LOGGER.info("Tracking " + Registries.ITEM.getId(item));
        for(Trackable<Identifier> trackable : tracked) {
            RandoAssistant.LOGGER.info(trackable.getIdentifier() + " -> " + trackable.getOutput() + " (enabled: " + trackable.isEnabled() + ")");
        }
    }

    public int getDiscoveredBlocksCount() {
        return TRACKABLE_LOOTED.getFiltered(trackable -> Registries.BLOCK.containsId(trackable.getIdentifier()) && trackable.isEnabled()).size();
    }

    public int getTotalBlocksCount() {
        return TRACKABLE_LOOTED.getFiltered(trackable -> Registries.BLOCK.containsId(trackable.getIdentifier())).size() - 6;
    }

    public int getDiscoveredCandlesCount() {
        return TRACKABLE_LOOTED.getFiltered(trackable -> Registries.BLOCK.containsId(trackable.getIdentifier()) && trackable.isEnabled() && Registries.BLOCK.get(trackable.getIdentifier()) instanceof CandleBlock).size();
    }

    public int getTotalCandlesCount() {
        return TRACKABLE_LOOTED.getFiltered(trackable -> Registries.BLOCK.containsId(trackable.getIdentifier()) && Registries.BLOCK.get(trackable.getIdentifier()) instanceof CandleBlock).size();
    }

    public int getDiscoveredEntitiesCount() {
        return TRACKABLE_LOOTED.getFiltered(trackable -> Registries.ENTITY_TYPE.containsId(trackable.getIdentifier()) && trackable.isEnabled()).size();
    }

    public int getTotalEntitiesCount() {
        return TRACKABLE_LOOTED.getFiltered(trackable -> Registries.ENTITY_TYPE.containsId(trackable.getIdentifier())).size() - 2;
    }

    public int getDiscoveredOtherCount() {
        return TRACKABLE_LOOTED.getFiltered(trackable -> !Registries.BLOCK.containsId(trackable.getIdentifier()) && !Registries.ENTITY_TYPE.containsId(trackable.getIdentifier()) && !Registries.ITEM.containsId(trackable.getIdentifier()) && trackable.isEnabled()).size();
    }

    public int getTotalOtherCount() {
        return TRACKABLE_LOOTED.getFiltered(trackable -> !Registries.BLOCK.containsId(trackable.getIdentifier()) && !Registries.ENTITY_TYPE.containsId(trackable.getIdentifier()) && !Registries.ITEM.containsId(trackable.getIdentifier())).size();
    }

    public int getDiscoveredCount() {
        return getDiscoveredBlocksCount() + getDiscoveredEntitiesCount() + getDiscoveredOtherCount();
    }

    public int getTotalCount() {
        return getTotalBlocksCount() + getTotalEntitiesCount() + getTotalOtherCount();
    }

    public void testAll() {
        int discoveredCount = getDiscoveredCount();
        int totalCount = getTotalCount();
        if(discoveredCount >= 50 && discoveredCount < 100) {
            Networking.requestAdvancementUnlock(LootAdvancement.FIFTY);
        } else if (discoveredCount >= 100 && discoveredCount < 200) {
            Networking.requestAdvancementUnlock(LootAdvancement.HUNDRED);
        } else if (discoveredCount >= 200 && discoveredCount < 500) {
            Networking.requestAdvancementUnlock(LootAdvancement.TWO_HUNDRED);
        } else if (discoveredCount >= 500 && discoveredCount < totalCount) {
            Networking.requestAdvancementUnlock(LootAdvancement.FIVE_HUNDRED);
        } else if(discoveredCount >= totalCount) {
            Networking.requestAdvancementUnlock(LootAdvancement.ALL);
        } else if (getDiscoveredBlocksCount() >= getTotalBlocksCount()) {
            Networking.requestAdvancementUnlock(LootAdvancement.ALL_BLOCKS);
        } else if (getDiscoveredEntitiesCount() >= getTotalEntitiesCount()) {
            Networking.requestAdvancementUnlock(LootAdvancement.ALL_ENTITIES);
        } else if (getDiscoveredOtherCount() >= getTotalOtherCount()) {
            Networking.requestAdvancementUnlock(LootAdvancement.ALL_OTHER);
        }
    }

    public boolean isEnabled(Identifier id) {
        return ID_TO_TRACKABLE.containsKey(id) && ID_TO_TRACKABLE.get(id).isEnabled();
    }

    private static class TrackableMap<T> {
        private final HashMap<Stat<T>, Trackable<T>> trackables;

        public TrackableMap() {
            this.trackables = new HashMap<>();
        }

        public Trackable<T> getOrCreate(Stat<T> stat, Identifier sourceId) {
            if(trackables.containsKey(stat)) {
                return trackables.get(stat);
            } else {
                Trackable<T> trackable = new Trackable<>(stat, sourceId);
                trackables.put(stat, trackable);
                return trackable;
            }
        }

        public Set<Trackable<T>> getEnabled() {
            return getFiltered(Trackable::isEnabled);
        }

        public Set<Trackable<T>> getFiltered(Predicate<Trackable<T>> filter) {
            Set<Trackable<T>> filtered = Sets.newHashSet();
            for(Trackable<T> trackable : trackables.values()) {
                if(filter.test(trackable)) {
                    filtered.add(trackable);
                }
            }
            return filtered;
        }

        public void clear() {
            trackables.clear();
        }

        @Override
        public String toString() {
            return "TrackableMap{" +
                    "trackables=" + trackables +
                    '}';
        }
    }
}
