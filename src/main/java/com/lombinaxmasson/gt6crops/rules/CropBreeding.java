package com.lombinaxmasson.gt6crops.rules;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * CropsNH breeding: a cross copies one mature neighbor, otherwise a
 * deterministic mutation wins, otherwise a shared mutation pool.
 *
 * <p>Stat variation matches the CropsNH defaults, {@code -2..4} around the
 * average of the parents that actually took part. Fertilizer on every
 * participating parent keeps that variation from going negative.</p>
 */
public final class CropBreeding {
    public static final int BREEDING_LOW = -2;
    public static final int BREEDING_HIGH = 4;

    private CropBreeding() {}

    public record Parent(
            String cardId,
            CropRules.Stats stats,
            boolean canCross,
            boolean canBreed,
            boolean fertilized) {}

    public record Outcome(String cardId, CropRules.Stats stats) {}

    /** The change to each stat around the parents' average; {@code high} never drops below {@code low}. */
    public record Variation(int low, int high) {
        public static final Variation DEFAULT = new Variation(BREEDING_LOW, BREEDING_HIGH);

        public Variation {
            high = Math.max(low, high);
        }
    }

    public record Mutation(String child, List<String> parents, boolean machineOnly) {}

    /** One pool in file order, after cards that did not load were removed. */
    public record Pool(String id, List<String> crops) {}

    public static final class Book {
        public static final Book EMPTY = new Book(List.of(), List.of(), Map.of());

        private final List<Mutation> mutations;
        private final List<Pool> pools;
        private final Map<String, Integer> order;

        private Book(
                List<Mutation> mutations,
                List<Pool> pools,
                Map<String, Integer> order) {
            this.mutations = mutations;
            this.pools = pools;
            this.order = order;
        }

        public static Book load(InputStream stream, List<String> loadedIds) {
            FileDto file = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    FileDto.class);
            return from(file, loadedIds);
        }

        public List<Mutation> mutations() {
            return mutations;
        }

        public List<Pool> pools() {
            return pools;
        }

        public Outcome resolve(List<Parent> neighbors, Variation variation, Random random) {
            if (neighbors.isEmpty()) {
                return null;
            }
            if (random.nextBoolean()) {
                List<Parent> crossing = neighbors.stream().filter(Parent::canCross).toList();
                if (!crossing.isEmpty()) {
                    Parent chosen = crossing.get(random.nextInt(crossing.size()));
                    List<Parent> same = crossing.stream()
                            .filter(parent -> parent.cardId().equals(chosen.cardId()))
                            .toList();
                    return outcome(chosen.cardId(), same, variation, random);
                }
            }

            List<Parent> breeding = neighbors.stream().filter(Parent::canBreed).toList();
            if (breeding.size() < 2) {
                return null;
            }
            List<String> distinct = breeding.stream()
                    .map(Parent::cardId)
                    .distinct()
                    .sorted(order())
                    .toList();
            List<Mutation> matches = new ArrayList<>();
            for (Mutation mutation : mutations) {
                if (mutation.machineOnly()) {
                    continue;
                }
                List<String> required = mutation.parents().stream()
                        .distinct()
                        .sorted(order())
                        .toList();
                if (required.size() >= 2
                        && required.size() <= distinct.size()
                        && distinct.subList(0, required.size()).equals(required)) {
                    matches.add(mutation);
                }
            }
            if (!matches.isEmpty()) {
                Mutation chosen = matches.get(random.nextInt(matches.size()));
                Set<String> required = new HashSet<>(chosen.parents());
                List<Parent> used = breeding.stream()
                        .filter(parent -> required.contains(parent.cardId()))
                        .toList();
                return outcome(chosen.child(), used, variation, random);
            }

            List<List<String>> poolHits = new ArrayList<>();
            for (Pool pool : pools) {
                List<String> members = pool.crops();
                Set<String> memberSet = new HashSet<>(members);
                int hits = 0;
                for (Parent parent : breeding) {
                    if (memberSet.contains(parent.cardId()) && ++hits >= 2) {
                        poolHits.add(members);
                        break;
                    }
                }
            }
            if (poolHits.isEmpty()) {
                return null;
            }
            List<String> pool = poolHits.get(random.nextInt(poolHits.size()));
            Set<String> memberSet = new HashSet<>(pool);
            List<Parent> used = breeding.stream()
                    .filter(parent -> memberSet.contains(parent.cardId()))
                    .toList();
            return outcome(pool.get(random.nextInt(pool.size())), used, variation, random);
        }

        private Outcome outcome(String cardId, List<Parent> used, Variation variation, Random random) {
            boolean onlyUp = !used.isEmpty() && used.stream().allMatch(Parent::fertilized);
            List<CropRules.Stats> stats = used.stream().map(Parent::stats).toList();
            return new Outcome(cardId, variate(stats, onlyUp, variation, random));
        }

        private Comparator<String> order() {
            return Comparator.comparingInt(id -> order.getOrDefault(id, Integer.MAX_VALUE));
        }
    }

    public static CropRules.Stats variate(
            List<CropRules.Stats> parents, boolean onlyUp, Variation variation, Random random) {
        return new CropRules.Stats(
                vary(parents, CropRules.Stats::growth, onlyUp, variation, random),
                vary(parents, CropRules.Stats::gain, onlyUp, variation, random),
                vary(parents, CropRules.Stats::resistance, onlyUp, variation, random));
    }

    private static Book from(FileDto file, List<String> loadedIds) {
        Map<String, Integer> order = new HashMap<>();
        for (int index = 0; index < loadedIds.size(); index++) {
            order.put(loadedIds.get(index), index);
        }
        Set<String> loaded = order.keySet();
        List<Mutation> mutations = new ArrayList<>();
        if (file != null && file.mutations != null) {
            for (MutationDto dto : file.mutations) {
                if (dto.child == null || dto.parents == null || dto.parents.size() < 2) {
                    continue;
                }
                if (!loaded.contains(dto.child) || !loaded.containsAll(dto.parents)) {
                    continue;
                }
                mutations.add(new Mutation(
                        dto.child, List.copyOf(dto.parents), dto.machineOnly));
            }
        }
        Map<String, List<String>> pools = new LinkedHashMap<>();
        if (file != null && file.pools != null) {
            for (PoolDto dto : file.pools) {
                if (dto.id == null || dto.crops == null) {
                    continue;
                }
                List<String> members = dto.crops.stream().filter(loaded::contains).distinct().toList();
                if (members.size() >= 2) {
                    pools.put(dto.id, members);
                }
            }
        }
        List<Pool> poolList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : pools.entrySet()) {
            poolList.add(new Pool(entry.getKey(), entry.getValue()));
        }
        return new Book(List.copyOf(mutations), List.copyOf(poolList), Map.copyOf(order));
    }

    private static int vary(
            List<CropRules.Stats> parents,
            java.util.function.ToIntFunction<CropRules.Stats> stat,
            boolean onlyUp,
            Variation variation,
            Random random) {
        int sum = 0;
        for (CropRules.Stats parent : parents) {
            sum += stat.applyAsInt(parent);
        }
        int average = sum / parents.size();
        int span = variation.high() + 1 - variation.low();
        int change = random.nextInt(span) + variation.low();
        if (onlyUp && change < 0) {
            change = 0;
        }
        return Math.max(0, Math.min(CropRules.MAX_STAT, average + change));
    }

    private static final class FileDto {
        List<MutationDto> mutations;
        List<PoolDto> pools;
    }

    private static final class MutationDto {
        String child;
        List<String> parents;
        @SerializedName("machine_only")
        boolean machineOnly;
    }

    private static final class PoolDto {
        String id;
        List<String> crops;
    }
}
