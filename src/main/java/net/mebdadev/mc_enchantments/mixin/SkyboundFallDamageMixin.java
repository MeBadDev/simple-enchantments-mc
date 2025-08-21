package net.mebdadev.mc_enchantments.mixin;

import net.mebdadev.mc_enchantments.enchantments.SkyboundHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class SkyboundFallDamageMixin {

    // Reduce fall damage right before LivingEntity#damage is invoked inside handleFallDamage.
    // If mapped name differs, adjust "handleFallDamage" accordingly (check crash log if fails again).
    @ModifyArg(
            method = "handleFallDamage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;damage(Lnet/minecraft/entity/damage/DamageSource;F)Z"),
            index = 1
    )
    private float simpleEnchantments$skybound$reduceFallDamage(float amount) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (self instanceof PlayerEntity player) {
            return SkyboundHandler.modifyFallDamage(player, amount, (float) self.fallDistance);
        }
        return amount;
    }
}
