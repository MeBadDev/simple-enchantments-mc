package net.mebdadev.mc_enchantments.enchantments;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.mebdadev.mc_enchantments.SimpleEnchantments;
import net.minecraft.component.type.ItemEnchantmentsComponent;

import java.util.*;

/**
 * Vein miner handler: when breaking an ore with the enchant, mines a connected cluster (same block type) with durability cost per block.
 */
public final class VeinMinerHandler {
    private static final int MAX_BLOCKS = 128; // safety cap
    private static final ThreadLocal<Boolean> REENTRANT = ThreadLocal.withInitial(() -> false);

    private VeinMinerHandler() {}

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register(VeinMinerHandler::beforeBreak);
    }

    private static boolean beforeBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity be) {
        if (world.isClient) return true;
        if (!(world instanceof ServerWorld serverWorld)) return true;
        if (REENTRANT.get()) return true;

        if (!isOre(state)) return true;
        ItemStack tool = player.getMainHandStack();
        if (tool.isEmpty()) return true;
        if (getLevel(tool) <= 0) return true;

        // BFS to find connected same-type ore blocks
        try {
            REENTRANT.set(true);
            List<BlockPos> cluster = collectCluster(serverWorld, pos, state, MAX_BLOCKS);
            if (cluster.size() <= 1) return true; // vanilla handles single block
            // Sort for stability (closest first)
            cluster.sort(Comparator.comparingInt((BlockPos p) -> p.getManhattanDistance(pos)));
            boolean creative = player.isCreative();
            int brokenExtra = 0;
            for (BlockPos bp : cluster) {
                if (bp.equals(pos)) continue; // origin handled by vanilla
                if (tool.isEmpty()) break;
                BlockState bs = world.getBlockState(bp);
                if (bs.getBlock() != state.getBlock()) continue; // must still match
                if (!creative && tool.isDamageable()) {
                    tool.setDamage(tool.getDamage() + 1);
                    if (tool.getDamage() >= tool.getMaxDamage()) {
                        player.sendEquipmentBreakStatus(tool.getItem(), EquipmentSlot.MAINHAND);
                        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                        break;
                    }
                }
                if (world.breakBlock(bp, true, player)) brokenExtra++;
            }
            if (brokenExtra > 0) {
                SimpleEnchantments.LOGGER.debug("vein_miner mined {} extra blocks at {}", brokenExtra, pos);
            }
        } finally {
            REENTRANT.set(false);
        }
        return true;
    }

    private static List<BlockPos> collectCluster(ServerWorld world, BlockPos origin, BlockState target, int limit) {
        List<BlockPos> list = new ArrayList<>();
        HashSet<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);
        while (!queue.isEmpty() && list.size() < limit) {
            BlockPos current = queue.poll();
            BlockState st = world.getBlockState(current);
            if (st.getBlock() != target.getBlock()) continue;
            list.add(current);
            // 3D neighbors (6-directional is sufficient for ore veins) - optionally include diagonals
            for (BlockPos offset : SIX_DIRECTIONS) {
                BlockPos np = current.add(offset.getX(), offset.getY(), offset.getZ());
                if (visited.add(np)) queue.add(np);
            }
        }
        return list;
    }

    private static final List<BlockPos> SIX_DIRECTIONS = List.of(
            new BlockPos(1,0,0), new BlockPos(-1,0,0), new BlockPos(0,1,0), new BlockPos(0,-1,0), new BlockPos(0,0,1), new BlockPos(0,0,-1)
    );

        // Custom tag so other mods can extend what counts as an ore.
        private static final TagKey<net.minecraft.block.Block> VEIN_MINER_ORES = TagKey.of(RegistryKeys.BLOCK, Identifier.of(SimpleEnchantments.MOD_ID, "vein_miner_ores"));

        private static boolean isOre(BlockState state) {
            if (state.isIn(VEIN_MINER_ORES)) return true;
            // Fallback heuristic: ends with or contains _ore, or special-case ancient debris / quartz / nether gold
            String key = state.getBlock().getTranslationKey();
            if (key == null) return false;
            return key.contains("_ore") || key.contains("ancient_debris") || key.contains("quartz") || key.contains("nether_gold");
        }

    private static int getLevel(ItemStack stack) {
        ItemEnchantmentsComponent comp = stack.getEnchantments();
        if (comp == null) return 0;
        for (var entry : comp.getEnchantmentEntries()) {
            var enchantment = entry.getKey();
            if (enchantment.getKey().filter(k -> k.equals(ModEnchantments.VEIN_MINER)).isPresent()) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
}
