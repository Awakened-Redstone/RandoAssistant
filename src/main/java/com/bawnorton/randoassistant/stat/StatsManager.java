package com.bawnorton.randoassistant.stat;

import com.bawnorton.randoassistant.RandoAssistant;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatType;
import net.minecraft.util.Identifier;

public class StatsManager {
    public static final StatType<Identifier> INTERACTED = Registry.register(Registries.STAT_TYPE, new Identifier("randoassistant", "interacted_with_block"), new StatType<>(Registries.CUSTOM_STAT));
    public static final StatType<Identifier> LOOTED = Registry.register(Registries.STAT_TYPE, new Identifier("randoassistant", "looted_block"), new StatType<>(Registries.CUSTOM_STAT));
    public static final StatType<Identifier> CRAFTED = Registry.register(Registries.STAT_TYPE, new Identifier("randoassistant", "crafted_item"), new StatType<>(Registries.CUSTOM_STAT));
    public static final StatType<Block> SILK_TOUCHED = Registry.register(Registries.STAT_TYPE, new Identifier("randoassistant", "silk_touched_block"), new StatType<>(Registries.BLOCK));

    public static void init() {
        RandoAssistant.LOGGER.debug("Initializing StatsManager");
    }

    public static boolean isCustom(Stat<?> stat) {
        return stat.getType().equals(INTERACTED) || stat.getType().equals(LOOTED) || stat.getType().equals(SILK_TOUCHED) || stat.getType().equals(CRAFTED);
    }

    public static boolean usesIdentifier(Stat<?> stat) {
        return stat.getType().equals(INTERACTED) || stat.getType().equals(LOOTED) || stat.getType().equals(CRAFTED);
    }
}
