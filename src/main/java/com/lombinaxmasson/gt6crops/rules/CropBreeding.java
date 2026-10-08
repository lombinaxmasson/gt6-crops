package com.lombinaxmasson.gt6crops.rules;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

    public record Mutation(String child, List<String> parents, boolean machineOnly) {}

    public static final class Book {
        public static final Book EMPTY = new Book(List.of(), Map.of(), Map.of());

        private final List<Mutation> mutations;
        private final Map<String, List<String>> pools;
        private final Map<String, Integer> order;

        private Book(
                List<Mutation> mutations,
                Map<String, List<String>> pools,
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

        public Outcome resolve(List<Parent> neighbors, Random random) {
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
                    return outcome(chosen.cardId(), same, random);
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
                return outcome(chosen.child(), used, random);
            }

            List<List<String>> poolHits = new ArrayList<>();
            for (List<String> members : pools.values()) {
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
            return outcome(pool.get(random.nextInt(pool.size())), used, random);
        }

        private Outcome outcome(String cardId, List<Parent> used, Random random) {
            boolean onlyUp = !used.isEmpty() && used.stream().allMatch(Parent::fertilized);
            List<CropRules.Stats> stats = used.stream().map(Parent::stats).toList();
            return new Outcome(cardId, variate(stats, onlyUp, random));
        }

        private Comparator<String> order() {
            return Comparator.comparingInt(id -> order.getOrDefault(id, Integer.MAX_VALUE));
        }
    }

    public static CropRules.Stats variate(
            List<CropRules.Stats> parents, boolean onlyUp, Random random) {
        return new CropRules.Stats(
                vary(parents, CropRules.Stats::growth, onlyUp, random),
                vary(parents, CropRules.Stats::gain, onlyUp, random),
                vary(parents, CropRules.Stats::resistance, onlyUp, random));
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
        Map<String, List<String>> pools = new HashMap<>();
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
        return new Book(List.copyOf(mutations), Map.copyOf(pools), Map.copyOf(order));
    }

    private static int vary(
            List<CropRules.Stats> parents,
            java.util.function.ToIntFunction<CropRules.Stats> stat,
            boolean onlyUp,
            Random random) {
        int sum = 0;
        for (CropRules.Stats parent : parents) {
            sum += stat.applyAsInt(parent);
        }
        int average = sum / parents.size();
        int span = BREEDING_HIGH + 1 - BREEDING_LOW;
        int variation = random.nextInt(span) + BREEDING_LOW;
        if (onlyUp && variation < 0) {
            variation = 0;
        }
        return Math.max(0, Math.min(CropRules.MAX_STAT, average + variation));
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
