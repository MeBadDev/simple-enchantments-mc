package net.mebdadev.mc_enchantments.mixin;

import net.mebdadev.mc_enchantments.enchantments.SkyboundHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class SkyboundLivingEntityDamageMixin {

	// Signature: handleFallDamage(double fallDistance, float damageMultiplier, DamageSource source)
	@Inject(method = "handleFallDamage", at = @At("HEAD"))
	private void simpleEnchantments$skybound$reduceFallDamage(double fallDistance, float damageMultiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity)(Object)this;
		if (self instanceof PlayerEntity player && SkyboundHandler.getLevelFromBoots(player) > 0) {
			// Just scale stored fallDistance so vanilla computation yields reduced damage.
			double original = self.fallDistance;
			self.fallDistance = (float)(original * 0.2f); // 80% reduction
			if (player.getWorld().isClient) {
				System.out.println("[Skybound] Reduced fallDistance from " + original + " to " + self.fallDistance);
			}
		}
	}
}
