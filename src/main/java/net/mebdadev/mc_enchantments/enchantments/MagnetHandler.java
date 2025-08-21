package net.mebdadev.mc_enchantments.enchantments;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Magnet enchantment: attracts nearby item entities into the player's inventory.
 * Design:
 *  - Applied to chest armor (tag: #minecraft:chest_armor)
 *  - Up to 3 levels. Radius = 4 + 2 * level. Pull strength scales mildly.
 *  - Runs server-side each tick. Does not move items currently being picked up or with pickup delay.
 */
public final class MagnetHandler {
    private MagnetHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(MagnetHandler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            int level = getLevelFromChest(player);
            if (level <= 0) continue;
            double radius = 4.0 + 2.0 * level;
            Box box = Box.of(player.getPos(), radius * 2, radius * 2, radius * 2);
            List<ItemEntity> items = player.getWorld().getEntitiesByClass(ItemEntity.class, box, ie -> !ie.isRemoved() && !ie.cannotPickup() && !ie.isTouchingWaterOrRain());
            if (items.isEmpty()) continue;
            Vec3d playerPos = player.getPos().add(0, 0.5, 0);
            double strength = 0.35 + 0.1 * level; // motion added each tick
            for (ItemEntity item : items) {
                if (item.getOwner() != null && !item.getOwner().equals(player.getUuid())) continue; // respect ownership
                Vec3d dir = playerPos.subtract(item.getPos());
                double dist = dir.length();
                if (dist < 0.2) continue; // close enough / let vanilla pickup occur
                Vec3d norm = dir.normalize();
                // scale with inverse distance a bit so closer items accelerate less harshly
                double accel = strength * Math.min(1.5, 0.6 + (dist / radius));
                Vec3d vel = item.getVelocity().add(norm.multiply(accel));
                // clamp velocity magnitude to avoid launching
                double maxSpeed = 1.2 + 0.2 * level;
                if (vel.lengthSquared() > maxSpeed * maxSpeed) {
                    vel = vel.normalize().multiply(maxSpeed);
                }
                item.setVelocity(vel);
                item.velocityModified = true;
            }
        }
    }

    private static int getLevelFromChest(PlayerEntity player) {
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isEmpty()) return 0;
        ItemEnchantmentsComponent comp = chest.getEnchantments();
        if (comp == null) return 0;
        for (var entry : comp.getEnchantmentEntries()) {
            var enc = entry.getKey();
            if (enc.getKey().filter(k -> k.equals(ModEnchantments.MAGNET)).isPresent()) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
}
