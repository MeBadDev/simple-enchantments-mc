package net.mebdadev.mc_enchantments.mixin.client;

import net.mebdadev.mc_enchantments.enchantments.SkyboundHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class SkyboundClientPlayerMixin {
    @Unique private boolean simpleEnchantments$skybound$canDouble = false;
    @Unique private boolean simpleEnchantments$skybound$didDouble = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void simpleEnchantments$skybound$tick(CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity)(Object)this;
        if (self.isOnGround() || self.isTouchingWater() || self.isInLava()) {
            simpleEnchantments$skybound$didDouble = false;
            simpleEnchantments$skybound$canDouble = SkyboundHandler.getLevelFromBoots(self) > 0;
        }
    boolean jumpKey = MinecraftClient.getInstance().options != null && MinecraftClient.getInstance().options.jumpKey.isPressed();
    if (simpleEnchantments$skybound$canDouble && !simpleEnchantments$skybound$didDouble && jumpKey && !self.isOnGround() && self.getVelocity().y < 0.05) {
            // Perform double jump
            self.setVelocity(self.getVelocity().x, 0.42D, self.getVelocity().z);
            simpleEnchantments$skybound$didDouble = true;
            self.fallDistance = 0; // forgive fall a bit right after jump
        }
    }
}
