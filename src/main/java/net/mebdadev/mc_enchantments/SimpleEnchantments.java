package net.mebdadev.mc_enchantments;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.mebdadev.mc_enchantments.enchantments.LumberjackHandler;
import net.mebdadev.mc_enchantments.enchantments.ModEnchantments;
import net.mebdadev.mc_enchantments.enchantments.VeinMinerHandler;



public class SimpleEnchantments implements ModInitializer {
	public static final String MOD_ID = "simple-enchantments";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEnchantments.initialize();
		LumberjackHandler.register();
		VeinMinerHandler.register();
		LOGGER.info("Simple Enchantments mod initialized with Lumberjack & Vein Miner handlers!");
	}
}