package net.mebdadev.mc_enchantments.mixin;

import net.mebdadev.mc_enchantments.enchantments.SkyboundHandler;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class SkyboundPlayerMixin {

    @Inject(method = "jump", at = @At("HEAD"))
    private void simpleEnchantments$skybound$boostJump(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        double vy = self.getVelocity().y;
        double modified = SkyboundHandler.modifyJumpVelocity(self, vy);
        if (modified != vy) {
            self.setVelocity(self.getVelocity().x, modified, self.getVelocity().z);
        }
    }
}
