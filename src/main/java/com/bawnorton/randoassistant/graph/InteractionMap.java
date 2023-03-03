package com.bawnorton.randoassistant.graph;

import com.bawnorton.randoassistant.RandoAssistant;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class InteractionMap {
    private final Map<Set<String>, Set<String>> serializedInteractionMap;
    private final BiMap<Input, Output> interactionMap;

    private InteractionMap(BiMap<Input, Output> interactionMap) {
        this.interactionMap = interactionMap;
        this.serializedInteractionMap = new HashMap<>();

        LootTableGraph graph = RandoAssistant.lootTableMap.getGraph();
        graph.getDrawer().disable();
        interactionMap.forEach((input, output) -> RandoAssistant.lootTableMap.getGraph().addInteraction(input.content(), output.content()));
        graph.getDrawer().enable();

        initSerializedCraftingMap();
    }

    public InteractionMap() {
        this(HashBiMap.create());
    }

    public static InteractionMap fromSerialized(Map<String, List<String>> serializedInteractionMap) {
        BiMap<Input, Output> interactionMap = HashBiMap.create();
        if (serializedInteractionMap == null) return new InteractionMap();

        for (Map.Entry<String, List<String>> serializedInteraction : serializedInteractionMap.entrySet()) {
            List<String> keys = Arrays.asList(serializedInteraction.getKey().replace("[", "").replace("]", "").replaceAll(" +", "").split(","));
            List<String> values = serializedInteraction.getValue();
            addInteraction(interactionMap, Input.fromSerialized(keys), Output.fromSerialized(values));
        }

        return new InteractionMap(interactionMap);
    }

    private void initSerializedCraftingMap() {
        interactionMap.forEach((input, output) -> serializedInteractionMap.put(input.serialized(), output.serialized()));
    }

    public Set<Map.Entry<Set<Item>, Set<Item>>> getMap() {
        Set<Map.Entry<Set<Item>, Set<Item>>> map = new HashSet<>();
        interactionMap.forEach((input, output) -> map.add(Map.entry(input.content(), output.content())));
        return map;
    }

    public Map<Set<String>, Set<String>> getSerializedInteractionMap() {
        return serializedInteractionMap;
    }

    private static void addInteraction(BiMap<Input, Output> interactionMap, Input input, Output output) {
        try {
            boolean added = false;
            for(Input key: interactionMap.keySet()) {
                if(key.content().equals(input.content())) {
                    Output recorded = interactionMap.get(key);
                    recorded = recorded.merge(output);
                    interactionMap.put(key, recorded);
                    added = true;
                    break;
                }
            }
            for(Output value: interactionMap.values()) {
                if(value.content().equals(output.content())) {
                    Input recorded = interactionMap.inverse().get(value);
                    recorded = recorded.merge(input);
                    interactionMap.inverse().put(value, recorded);
                    added = true;
                    break;
                }
            }
            if(!added) interactionMap.put(input, output);
        } catch (IllegalArgumentException e) {
            RandoAssistant.LOGGER.error("Failed to add interaction: " + input + " -> " + output);
        }
    }

    private void addInteraction(Input input, Output output) {
        addInteraction(interactionMap, input, output);
        serializedInteractionMap.put(input.serialized(), output.serialized());
        RandoAssistant.lootTableMap.getGraph().addInteraction(input.content(), output.content());
    }

    public void addInteraction(Item input, Item output) {
        addInteraction(Input.of(input), Output.of(output));
    }

    public void addInteraction(Block input, Block output) {
        addInteraction(input.asItem(), output.asItem());
    }

    public void addInteraction(List<Item> input, Item output) {
        addInteraction(Input.of(new HashSet<>(input)), Output.of(output));
    }

    public void addInteraction(Item item, List<ItemStack> output) {
        addInteraction(Input.of(item), Output.of(output.stream().map(ItemStack::getItem).collect(Collectors.toSet())));
    }

    private record Input(Set<Item> content) implements Iterable<Item> {
        public static Input of(Set<Item> input) {
            return new Input(input);
        }

        public static Input of(Item input) {
            return new Input(Collections.singleton(input));
        }

        public static Input fromSerialized(List<String> values) {
            Set<Item> input = new HashSet<>();
            values.forEach(item -> input.add(Registries.ITEM.get(new Identifier(item))));
            return new Input(input);
        }

        public Input merge(Input other) {
            Set<Item> merged = new HashSet<>(content);
            merged.addAll(other.content);
            return new Input(merged);
        }

        public Set<String> serialized() {
            Set<String> serialized = new HashSet<>();
            content.forEach(item -> serialized.add(Registries.ITEM.getId(item).toString()));
            return serialized;
        }

        @Override
        public int hashCode() {
            return content.stream().mapToInt(item -> item.getTranslationKey().hashCode()).sum();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Output other)) return false;
            return content.equals(other.content);
        }

        @NotNull
        @Override
        public Iterator<Item> iterator() {
            return content.iterator();
        }
    }

    private record Output(Set<Item> content) implements Iterable<Item> {
        public static Output of(Set<Item> output) {
            return new Output(output);
        }

        public static Output of(Item output) {
            return new Output(Collections.singleton(output));
        }

        public static Output fromSerialized(List<String> serialized) {
            Set<Item> output = new HashSet<>();
            serialized.forEach(item -> output.add(Registries.ITEM.get(new Identifier(item))));
            return new Output(output);
        }

        public Output merge(Output output) {
            if(output == null) return this;
            Set<Item> merged = new HashSet<>(content);
            merged.addAll(output.content);
            return new Output(merged);
        }

        public Set<String> serialized() {
            Set<String> serialized = new HashSet<>();
            content.forEach(item -> serialized.add(Registries.ITEM.getId(item).toString()));
            return serialized;
        }

        @Override
        public int hashCode() {
            return content.stream().mapToInt(item -> item.getTranslationKey().hashCode()).sum();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Output other)) return false;
            return content.equals(other.content);
        }

        @NotNull
        @Override
        public Iterator<Item> iterator() {
            return content.iterator();
        }
    }
}
