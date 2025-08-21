package net.mebdadev.mc_enchantments.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.mebdadev.mc_enchantments.SimpleEnchantments;

public final class ModEnchantments {
    public static final RegistryKey<Enchantment> LUMBERJACK = of("lumberjack");
    public static final RegistryKey<Enchantment> VEIN_MINER = of("vein_miner");
    public static final RegistryKey<Enchantment> SKYBOUND = of("skybound");
    private static RegistryKey<Enchantment> of(String name) {
        return RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(SimpleEnchantments.MOD_ID, name));
    }

    public static void initialize() {
        // This method is called to ensure the class is loaded
    }
}
