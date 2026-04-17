package com.pvpbot;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class PvpBotEntities {

    public static final Identifier PVP_BOT_ID = Identifier.of(PvpBotMod.MOD_ID, "pvp_bot");
    public static final RegistryKey<EntityType<?>> PVP_BOT_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, PVP_BOT_ID);

    public static final EntityType<PvpBotEntity> PVP_BOT =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    PVP_BOT_ID,
                    EntityType.Builder.<PvpBotEntity>create(PvpBotEntity::new, SpawnGroup.MISC)
                            .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                            .maxTrackingRange(64)
                            .build(PVP_BOT_KEY)
            );

    public static void register() {
        FabricDefaultAttributeRegistry.register(PVP_BOT, PvpBotEntity.createAttributes());
        PvpBotMod.LOGGER.info("PvP Bot entity type registered.");
    }
}
