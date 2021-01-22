package com.fuzs.respawnableanimals.config;

import com.fuzs.respawnableanimals.config.deserialize.EntryCollectionBuilder;
import com.google.common.collect.Lists;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

public class ConfigBuildHandler {

    private static final EntryCollectionBuilder<EntityType<?>> ENTITY_COLLECTION_BUILDER = new EntryCollectionBuilder<>(ForgeRegistries.ENTITIES);

    public static Set<EntityType<?>> animalBlacklist;
    public static boolean oldChunkPersistence;
    public static boolean summonedMobPersistence;
    public static boolean fixRabbitAI;
    public static boolean sheepRunFromWolves;

    public static void setup(ModConfig.Type type) {

        ConfigManager.builder().create("general", ConfigBuildHandler::setupGeneral, type);
    }

    private static void setupGeneral(ForgeConfigSpec.Builder builder) {

        ConfigManager.get().registerEntry(builder.comment("Blacklist animals which will never despawn.", EntryCollectionBuilder.CONFIG_STRING).define("Animal Blacklist", Lists.<String>newArrayList()),v -> animalBlacklist = v, v -> ENTITY_COLLECTION_BUILDER.buildEntrySet(v, animal -> animal.getClassification() == EntityClassification.CREATURE, "No animal"));
        ConfigManager.get().registerEntry(builder.comment("Make animals unable to despawn in chunks older than one hour. This prevents animals from vanishing out of pens in a world that has been used without this mod before.").define("Old Chunk Persistence", true), v -> oldChunkPersistence = v);
        ConfigManager.get().registerEntry(builder.comment("Make all mobs (not just animals) automatically persistent when spawned using the \"/summon\" command, a spawn egg or a dispenser.").define("Summoned Mob Persistence", false), v -> summonedMobPersistence = v);
        ConfigManager.get().registerEntry(builder.comment("Changes rabbit ai for them to no longer be scared as easily. Also adds some basic animal behaviour (such as kits following their parents) which for some reason is missing by default.").define("Fix Rabbit AI", true), v -> fixRabbitAI = v);
        ConfigManager.get().registerEntry(builder.comment("Sheep now flee from wolves instead of letting themselves get eaten, because why would they do that.").define("Sheep Run From Wolves", true), v -> sheepRunFromWolves = v);
    }

}