package com.pvpbot;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * PvP practice bot with basic anti-mace prediction and low-health gapping.
 */
public class PvpBotEntity extends PathAwareEntity {

    private static final float GAP_HEALTH_THRESHOLD = 0.35f;
    private static final int GAP_USE_TICKS = 32;
    private static final int GAP_COOLDOWN_TICKS = 220;
    private static final int SHIELD_RAISE_TICKS = 20;

    /** The UUID of the player this bot should attack. Null = passive. */
    private UUID targetPlayerUUID = null;

    /** Human-readable name of this bot (set on spawn). */
    private String botLabel = "PvPBot";

    private int gapUseTicksRemaining = 0;
    private int gapCooldownTicksRemaining = 0;
    private int shieldTicksRemaining = 0;

    public PvpBotEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setCustomNameVisible(true);
        this.setCustomName(Text.literal("§c[PvP Bot] §f" + botLabel));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.ARMOR, 4.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.add(4, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));

    }

    @Override
    protected void initEquipment(net.minecraft.util.math.random.Random random,
                                 net.minecraft.world.LocalDifficulty difficulty) {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.MACE));
        this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        this.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        this.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
    }

    @Override
    protected void mobTick() {
        super.mobTick();

        if (this.getEntityWorld().isClient) {
            return;
        }

        if (gapCooldownTicksRemaining > 0) {
            gapCooldownTicksRemaining--;
        }

        if (shieldTicksRemaining > 0) {
            shieldTicksRemaining--;
            if (shieldTicksRemaining == 0 && this.isUsingItem()) {
                this.stopUsingItem();
            }
        }

        if (gapUseTicksRemaining > 0) {
            gapUseTicksRemaining--;
            this.getNavigation().stop();
            if (gapUseTicksRemaining == 0) {
                finishGap();
            }
            return;
        }

        if (shouldGap()) {
            startGap();
            return;
        }

        ServerPlayerEntity assignedTarget = resolveAssignedTarget();
        if (assignedTarget != null) {
            this.setTarget(assignedTarget);
            if (shouldPredictSlam(assignedTarget)) {
                raiseShield();
            }
        }
    }

    private boolean shouldGap() {
        float healthRatio = this.getHealth() / this.getMaxHealth();
        return healthRatio <= GAP_HEALTH_THRESHOLD && gapCooldownTicksRemaining <= 0;
    }

    private void startGap() {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_APPLE));
        this.setCurrentHand(Hand.MAIN_HAND);
        this.gapUseTicksRemaining = GAP_USE_TICKS;
        this.gapCooldownTicksRemaining = GAP_COOLDOWN_TICKS;
    }

    private void finishGap() {
        if (this.isUsingItem()) {
            this.stopUsingItem();
        }

        this.heal(8.0f);
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
        this.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 1200, 0));

        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.MACE));
    }

    private ServerPlayerEntity resolveAssignedTarget() {
        if (targetPlayerUUID == null || !(this.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return null;
        }
        return serverWorld.getPlayerByUuid(targetPlayerUUID);
    }

    private boolean shouldPredictSlam(ServerPlayerEntity target) {
        double verticalDiff = target.getY() - this.getY();
        double horizontalDistSq = Math.pow(target.getX() - this.getX(), 2)
                + Math.pow(target.getZ() - this.getZ(), 2);

        return verticalDiff >= 8.0 && horizontalDistSq <= 36.0;
    }

    private void raiseShield() {
        if (!this.getOffHandStack().isOf(Items.SHIELD)) {
            return;
        }
        if (!this.isUsingItem()) {
            this.setCurrentHand(Hand.OFF_HAND);
            this.shieldTicksRemaining = SHIELD_RAISE_TICKS;
        }
    }

    public void setTargetPlayer(UUID targetUUID) {
        this.targetPlayerUUID = targetUUID;
        this.setTarget(null);

        if (targetUUID != null && this.getEntityWorld() instanceof ServerWorld serverWorld) {
            PlayerEntity player = serverWorld.getPlayerByUuid(targetUUID);
            if (player != null) {
                this.setTarget(player);
            }
        }
    }

    public UUID getTargetPlayerUUID() {
        return targetPlayerUUID;
    }

    public void setBotLabel(String label) {
        this.botLabel = label;
        this.setCustomName(Text.literal("§c[PvP Bot] §f" + label));
    }

    public String getBotLabel() {
        return botLabel;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

}
