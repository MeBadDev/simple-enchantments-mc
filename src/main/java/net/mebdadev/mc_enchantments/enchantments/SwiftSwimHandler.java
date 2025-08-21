package net.mebdadev.mc_enchantments.enchantments;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.item.ItemStack;

/**
 * Swift Swim: leggings enchantment.
 * - While submerged, grants Dolphin's Grace and a small Haste boost to improve swimming and underwater mining.
 * - Duration refreshed each tick while condition holds.
 */
public final class SwiftSwimHandler {
    private SwiftSwimHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(SwiftSwimHandler::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            int level = getLevelFromLeggings(player);
            if (level <= 0) continue;
            // Only apply when submerged in water (not just rain)
            if (!player.isSubmergedInWater()) continue;
            // Dolphin's Grace grants fast swimming; amplifier 0 is enough.
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 20, 0, false, false, true));
            // Give a modest Haste for better underwater mining; amplifier scales with level-1
            int hasteAmp = Math.max(0, level - 1);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20, hasteAmp, false, false, true));
        }
    }

    private static int getLevelFromLeggings(PlayerEntity player) {
        ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
        if (legs.isEmpty()) return 0;
        ItemEnchantmentsComponent comp = legs.getEnchantments();
        if (comp == null) return 0;
        for (var entry : comp.getEnchantmentEntries()) {
            var enc = entry.getKey();
            if (enc.getKey().filter(k -> k.equals(ModEnchantments.SWIFTSWIM)).isPresent()) {
                return entry.getIntValue();
            }
        }
        return 0;
    }
}
