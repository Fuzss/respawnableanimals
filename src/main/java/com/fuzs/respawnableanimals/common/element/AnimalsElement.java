package com.fuzs.respawnableanimals.common.element;

import com.fuzs.puzzleslib_ra.config.deserialize.EntryCollectionBuilder;
import com.fuzs.puzzleslib_ra.element.AbstractElement;
import com.fuzs.puzzleslib_ra.element.side.ICommonElement;
import com.fuzs.respawnableanimals.mixin.accessor.IBooleanValueAccessor;
import com.google.common.collect.Lists;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.horse.SkeletonHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.GameRules;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;

public class AnimalsElement extends AbstractElement implements ICommonElement {

    public static final GameRules.RuleKey<GameRules.BooleanValue> PERSISTENT_ANIMALS = GameRules.register("persistentAnimals", GameRules.Category.SPAWNING, IBooleanValueAccessor.callCreate(false));
    
    public Set<EntityType<?>> animalBlacklist;
    public int maxAnimalNumber;
    public boolean summonedMobPersistence;

    @Override
    public String getDescription() {
        
        return "Animals are no longer persistent by default, making them spawn just like monsters.";
    }

    @Override
    public void setupCommon() {

        this.addListener(this::onBabyEntitySpawn);
        this.addListener(this::onAnimalTame);
        this.addListener(this::onPotentialSpawns);
        this.addListener(this::onEntityJoinWorld);
        this.addListener(this::onCheckSpawn);
    }

    @Override
    public void setupCommonConfig(ForgeConfigSpec.Builder builder) {

        addToConfig(builder.comment("Blacklist animals which will never despawn.", EntryCollectionBuilder.CONFIG_STRING).define("Animal Blacklist", Lists.<String>newArrayList()), v -> this.animalBlacklist = v, v -> new EntryCollectionBuilder<>(ForgeRegistries.ENTITIES).buildEntrySet(v));
        addToConfig(builder.comment("Constant for determining when to stop spawning animals in a world. Normally set to 10, monster constant is 70 for comparison. 18 is chosen to mimic spawning mechanics of the Beta era.").define("Animal Mob Cap", 18), v -> this.maxAnimalNumber = v);
        addToConfig(builder.comment("Make all mobs (not just animals) automatically persistent when spawned using the \"/summon\" command, a spawn egg or a dispenser.").define("Summoned Mob Persistence", false), v -> this.summonedMobPersistence = v);
    }

    private void onBabyEntitySpawn(final BabyEntitySpawnEvent evt) {

        // make freshly born baby animals persistent
        if (evt.getChild() != null) {

            evt.getChild().enablePersistence();
        }
    }

    private void onAnimalTame(final AnimalTameEvent evt) {

        // enable persistence for animals that have been tamed (cats, ocelots, wolves, and all horse types including llamas)
        evt.getAnimal().enablePersistence();
    }

    private void onPotentialSpawns(final WorldEvent.PotentialSpawns evt) {

        // prevent blacklisted animals from being respawned to prevent them from spawning endlessly as they are also blacklisted from counting towards the mob cap
        // this is not a good solution but I couldn't think of any other way
        evt.getList().removeIf(spawner -> this.animalBlacklist.contains(spawner.type));
    }

    private void onEntityJoinWorld(final EntityJoinWorldEvent evt) {

        // make skeleton horse spawned as trap persistent by default
        if (evt.getEntity() instanceof SkeletonHorseEntity && ((SkeletonHorseEntity) evt.getEntity()).isTrap()) {

            ((SkeletonHorseEntity) evt.getEntity()).enablePersistence();
        }
    }

    private void onCheckSpawn(final LivingSpawnEvent.CheckSpawn evt) {

        if (!(evt.getWorld() instanceof IServerWorld)) {

            return;
        }

        ServerWorld serverWorld = ((IServerWorld) evt.getWorld()).getWorld();
        if (evt.getSpawnReason() == SpawnReason.CHUNK_GENERATION || evt.getSpawnReason() == SpawnReason.NATURAL) {

            if (evt.getEntity() instanceof AnimalEntity && evt.getEntity().getType().getClassification() == EntityClassification.CREATURE) {

                // prevent animals from being spawned on world creation, but exclude blacklisted animals
                if (!serverWorld.getGameRules().getBoolean(PERSISTENT_ANIMALS) && !this.animalBlacklist.contains(evt.getEntity().getType())) {

                    if (evt.getSpawnReason() == SpawnReason.CHUNK_GENERATION) {

                        evt.setResult(Event.Result.DENY);
                    } else {

                        // prevent animals from being spawned when too far away from the closest player
                        double distanceToClosestPlayer = getPlayerDistance(serverWorld, evt.getX(), evt.getY(), evt.getZ());
                        if (!this.canSpawn(serverWorld, (MobEntity) evt.getEntity(), distanceToClosestPlayer)) {

                            evt.setResult(Event.Result.DENY);
                        }
                    }
                }
            }
        }
    }

    private double getPlayerDistance(ServerWorld serverWorld, double x, double y, double z) {

        PlayerEntity playerentity = serverWorld.getClosestPlayer(x, y, z, -1.0, false);

        // can't be null as the used event wouldn't have been called then
        assert playerentity != null;
        return playerentity.getDistanceSq(x, y, z);
    }

    private boolean canSpawn(ServerWorld serverWorld, MobEntity entity, double distanceToClosestPlayer) {

        if (distanceToClosestPlayer > entity.getType().getClassification().getInstantDespawnDistance() * entity.getType().getClassification().getInstantDespawnDistance() && canAnimalDespawn(this, entity, distanceToClosestPlayer)) {

            return false;
        } else {

            return entity.canSpawn(serverWorld, SpawnReason.NATURAL) && entity.isNotColliding(serverWorld);
        }
    }

    public static boolean canAnimalDespawn(AnimalsElement element, MobEntity entity, double distanceToClosestPlayer) {

        // replace canDespawn call, as injecting into the base method directly is not sufficient as it is overridden by many sub classes
        // special behavior such as blacklist is handled in preventDespawn injector
        if (element.isEnabled() && !entity.world.getGameRules().getBoolean(PERSISTENT_ANIMALS)) {

            return entity instanceof AnimalEntity && entity.getType().getClassification() == EntityClassification.CREATURE || entity.canDespawn(distanceToClosestPlayer);
        }

        return entity.canDespawn(distanceToClosestPlayer);
    }

}
