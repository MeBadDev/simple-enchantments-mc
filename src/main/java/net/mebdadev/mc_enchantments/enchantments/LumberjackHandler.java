package net.mebdadev.mc_enchantments.enchantments;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.mebdadev.mc_enchantments.SimpleEnchantments;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Hand;

/**
 * Handles the tree-felling behavior for the Lumberjack enchantment.
 * Simplified heuristic: when a natural log is broken with the enchantment, perform a BFS upward/outward collecting
 * connected logs until leaves are encountered or limits are exceeded, then break them naturally.
 */
public final class LumberjackHandler {
    private static final int MAX_LOGS = 256; // safety cap

    private LumberjackHandler() {}

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register(LumberjackHandler::beforeBreak);
    }

    private static final ThreadLocal<Boolean> REENTRANT = ThreadLocal.withInitial(() -> false);

    private static boolean beforeBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (world.isClient) return true;
        if (!(world instanceof ServerWorld serverWorld)) return true;
        if (REENTRANT.get()) return true;

        ItemStack tool = player.getMainHandStack();
        if (tool.isEmpty()) return true;
        int level = getLumberjackLevel(tool);
        if (level <= 0) return true;
        if (!state.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) return true;

        SimpleEnchantments.LOGGER.debug("lumberjack enchantment triggered at {}", pos);
        try {
            REENTRANT.set(true);
            int broken = fellTree(serverWorld, pos, player, state.getBlock());
            if (broken > 1) {
                SimpleEnchantments.LOGGER.debug("lumberjack enchantment felled {} logs at {}", broken, pos);
            }
        } finally {
            REENTRANT.set(false);
        }
        return true; // allow normal break
    }

    private static int fellTree(ServerWorld world, BlockPos origin, PlayerEntity player, Block originalLogBlock) {
        Set<BlockPos> logs = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        logs.add(origin);
        int processed = 0;
        boolean sawLeaves = false;

        while (!queue.isEmpty() && processed < MAX_LOGS) {
            BlockPos current = queue.poll();
            processed++;
            BlockState st = world.getBlockState(current);
            if (!st.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) continue;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos np = current.add(dx, dy, dz);
                        if (!logs.add(np)) continue;
                        BlockState ns = world.getBlockState(np);
                        if (ns.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) {
                            queue.add(np);
                        } else if (ns.getBlock() instanceof LeavesBlock) {
                            sawLeaves = true;
                        }
                    }
                }
            }
        }
        if (!sawLeaves) return 0;
        // Prepare ordered list (remove origin, sort by height ascending so lower logs go first)
        List<BlockPos> toBreak = new ArrayList<>(logs);
        toBreak.remove(origin);
        toBreak.sort((a,b) -> Integer.compare(a.getY(), b.getY()));

        ItemStack tool = player.getMainHandStack();
        boolean creative = player.isCreative();
        int broken = 0;
        for (BlockPos bp : toBreak) {
            // Stop if tool broke
            if (tool.isEmpty()) break;
            BlockState st = world.getBlockState(bp);
            if (!st.isIn(net.minecraft.registry.tag.BlockTags.LOGS)) continue;
            // Damage tool (one per extra log) unless creative or not damageable
            if (!creative && tool.isDamageable()) {
                tool.setDamage(tool.getDamage() + 1);
                if (tool.getDamage() >= tool.getMaxDamage()) {
                    player.sendEquipmentBreakStatus(tool.getItem(), EquipmentSlot.MAINHAND);
                    // Remove the broken tool from main hand
                    player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    tool = ItemStack.EMPTY; // local reference update
                    break; // stop further felling
                }
            }
            if (world.breakBlock(bp, true, player)) {
                broken++;
            }
        }
        return broken + 1; // include origin (always broken by normal process)
    }

    private static int getLumberjackLevel(ItemStack stack) {
        ItemEnchantmentsComponent comp = stack.getEnchantments();
        if (comp == null) return 0;
        // Iterate through entries comparing registry key ids.
        for (var entry : comp.getEnchantmentEntries()) {
            var enchantment = entry.getKey(); // RegistryEntry<Enchantment>
            if (enchantment.getKey().filter(k -> k.equals(ModEnchantments.LUMBERJACK)).isPresent()) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
}
