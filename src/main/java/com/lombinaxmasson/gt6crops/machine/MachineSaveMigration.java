package com.lombinaxmasson.gt6crops.machine;

import com.lombinaxmasson.gt6crops.Gt6Crops;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.neoforged.neoforge.event.level.ChunkDataEvent;

/**
 * Rewrites a Crop Manager or Seed Generator saved by the old block entity into the
 * behavior shell's keys. The shell only reads {@code gt.*}, and a behavior cannot see
 * {@code CompoundTag}, so the rewrite happens before the loaded block entity reads itself.
 *
 * <p>The chunk event runs on the thread that just created the block entity, before that
 * chunk is returned. It does not touch any other chunk. A block state that failed to parse
 * has already dropped the block entity, and this cannot bring it back.</p>
 */
public final class MachineSaveMigration {
    private static final String CROP_MANAGER = "gt6crops:crop_manager";
    private static final String SEED_GENERATOR = "gt6crops:seed_generator";

    private MachineSaveMigration() {}

    public static void onChunkLoad(ChunkDataEvent.Load event) {
        if (event.getType() != ChunkType.LEVELCHUNK || !(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        CompoundTag data = event.getData();
        if (!data.contains("block_entities", Tag.TAG_LIST) || chunk.getLevel() == null) {
            return;
        }
        ListTag entities = data.getList("block_entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < entities.size(); i++) {
            CompoundTag tag = entities.getCompound(i);
            if (!migrate(tag)) {
                continue;
            }
            BlockEntity entity = chunk.getBlockEntity(new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")));
            if (entity != null) {
                entity.loadWithComponents(tag, chunk.getLevel().registryAccess());
            }
        }
    }

    /**
     * Turns {@code Energy}, {@code Items}, {@code Tanks}, {@code Toggles}, {@code Overflow}
     * and the seed generator's pending job into {@code gt.*}. Returns whether the tag changed.
     * A tag that already uses the shell keys is left alone.
     */
    public static boolean migrate(CompoundTag tag) {
        String id = tag.getString("id");
        boolean cropManager = CROP_MANAGER.equals(id);
        boolean seedGenerator = SEED_GENERATOR.equals(id);
        if (!cropManager && !seedGenerator) {
            return false;
        }
        if (tag.contains("gt.items") || tag.contains("gt.energy") || tag.contains("gt.fluids")) {
            return false;
        }
        if (!tag.contains("Energy") && !tag.contains("Items") && !tag.contains("Tanks")
                && !tag.contains("Toggles") && !tag.contains("Overflow") && !tag.contains("Pending")) {
            return false;
        }
        tag.putLong("gt.energy", tag.getLong("Energy"));
        if (tag.contains("Items", Tag.TAG_COMPOUND)) {
            tag.put("gt.items", tag.getCompound("Items").copy());
        }
        tag.put("gt.fluids", fluids(tag.getList("Tanks", Tag.TAG_COMPOUND)));
        int toggles = tag.contains("Toggles") ? tag.getInt("Toggles") : cropManager ? 1 : 0;
        tag.putInt("gt.toggles", toggles);
        tag.put("gt.behavior", behavior(tag, seedGenerator));
        if (seedGenerator && tag.contains("TotalWork") && tag.getLong("TotalWork") > 0) {
            tag.putInt("gt.progress", (int) Math.round(tag.getLong("Work") * 1000.0 / tag.getLong("TotalWork")));
        }
        tag.remove("Energy");
        tag.remove("Items");
        tag.remove("Tanks");
        tag.remove("Toggles");
        tag.remove("Overflow");
        tag.remove("Pending");
        tag.remove("Work");
        tag.remove("TotalWork");
        tag.remove("MinimumPower");
        Gt6Crops.LOGGER.debug("gt6crops: migrated a saved {}", id);
        return true;
    }

    private static ListTag fluids(ListTag tanks) {
        ListTag rows = new ListTag();
        for (int i = 0; i < tanks.size(); i++) {
            CompoundTag saved = tanks.getCompound(i);
            CompoundTag row = new CompoundTag();
            String name = "";
            int amount = 0;
            if (saved.contains("Fluid", Tag.TAG_COMPOUND)) {
                CompoundTag fluid = saved.getCompound("Fluid");
                name = firstString(fluid, "id", "Id", "FluidName");
                amount = fluid.contains("amount") ? fluid.getInt("amount") : fluid.getInt("Amount");
            } else if (saved.contains("Fluid", Tag.TAG_STRING)) {
                name = saved.getString("Fluid");
                amount = saved.getInt("Amount");
            }
            if (amount <= 0 && saved.contains("Amount")) {
                amount = saved.getInt("Amount");
            }
            row.putString("Fluid", name);
            row.putInt("Amount", Math.max(0, amount));
            rows.add(row);
        }
        return rows;
    }

    private static CompoundTag behavior(CompoundTag tag, boolean seedGenerator) {
        CompoundTag behavior = new CompoundTag();
        CompoundTag ints = new CompoundTag();
        CompoundTag longs = new CompoundTag();
        CompoundTag items = new CompoundTag();
        if (tag.contains("Overflow", Tag.TAG_LIST)) {
            ListTag overflow = tag.getList("Overflow", Tag.TAG_COMPOUND);
            int kept = 0;
            for (int i = 0; i < overflow.size(); i++) {
                CompoundTag stack = overflow.getCompound(i);
                if (!stack.isEmpty()) {
                    items.put("overflow." + kept, stack.copy());
                    kept++;
                }
            }
            if (kept > 0) {
                ints.putInt("overflow", kept);
            }
        }
        if (seedGenerator && tag.contains("Pending", Tag.TAG_COMPOUND)) {
            items.put("pending", tag.getCompound("Pending").copy());
            longs.putLong("work", tag.getLong("Work"));
            longs.putLong("total", tag.getLong("TotalWork"));
            longs.putLong("minimum", tag.getLong("MinimumPower"));
        }
        behavior.put("i", ints);
        behavior.put("l", longs);
        behavior.put("b", new CompoundTag());
        behavior.put("s", new CompoundTag());
        behavior.put("t", items);
        return behavior;
    }

    private static String firstString(CompoundTag tag, String... keys) {
        for (String key : keys) {
            if (tag.contains(key, Tag.TAG_STRING)) {
                return tag.getString(key);
            }
        }
        return "";
    }
}
