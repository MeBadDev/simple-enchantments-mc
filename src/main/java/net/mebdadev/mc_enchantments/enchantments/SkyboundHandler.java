package net.mebdadev.mc_enchantments.enchantments;

import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * Skybound: single level enchantment.
 * Effects:
 *  - Allows one extra mid-air jump (double jump) while falling/airborne.
 *  - Reduces fall damage by 80%.
 */
public final class SkyboundHandler {
	private SkyboundHandler() {}

	public static void register() { /* reserved for future event hooks */ }

	public static int getLevelFromBoots(PlayerEntity player) {
		ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
		if (boots.isEmpty()) return 0;
		ItemEnchantmentsComponent comp = boots.getEnchantments();
		if (comp == null) return 0;
		for (var entry : comp.getEnchantmentEntries()) {
			var enc = entry.getKey();
			if (enc.getKey().filter(k -> k.equals(ModEnchantments.SKYBOUND)).isPresent()) {
				return entry.getIntValue();
			}
		}
		return 0;
	}

	// Track whether player has used mid-air jump (simple boolean stored per tick in player object via capability-like static; minimal approach)
	private static final java.util.WeakHashMap<PlayerEntity, Boolean> USED = new java.util.WeakHashMap<>();

	public static double modifyJumpVelocity(PlayerEntity player, double originalY) {
		if (getLevelFromBoots(player) <= 0) {
			USED.remove(player);
			return originalY;
		}
		// When on ground: reset
		if (player.isOnGround()) {
			USED.put(player, false);
			return originalY; // normal initial jump strength
		}
		// If airborne and not yet used extra jump and vertical velocity is small (player pressed jump again)
		boolean used = USED.getOrDefault(player, false);
		if (!used) {
			USED.put(player, true);
			// Give a fixed boost (simulate second jump)
			return Math.max(originalY, 0.42D); // vanilla jump base
		}
		return originalY;
	}

	public static float modifyFallDamage(PlayerEntity player, float original, float distance) {
		if (getLevelFromBoots(player) <= 0) return original;
		return original * 0.2f; // 80% reduction
	}
}
