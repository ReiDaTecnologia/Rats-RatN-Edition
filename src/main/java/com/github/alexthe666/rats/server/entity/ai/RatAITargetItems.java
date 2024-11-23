package com.github.alexthe666.rats.server.entity.ai;

import com.github.alexthe666.rats.server.entity.EntityRat;
import com.github.alexthe666.rats.server.entity.RatCommand;
import com.github.alexthe666.rats.server.entity.RatUtils;
import com.github.alexthe666.rats.server.items.RatsItemRegistry;
import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RatAITargetItems<T extends EntityItem> extends EntityAITarget {
    protected final RatAITargetItems.Sorter theNearestAttackableTargetSorter;
    protected final Predicate<? super EntityItem> targetEntitySelector;
    protected int executionChance;
    protected boolean mustUpdate;
    protected EntityItem targetEntity;
    private EntityRat rat;
    private int stuckTimer;
    private int stuckTimeOut;
    private Vec3d prevPositionVector;

    public RatAITargetItems(EntityRat creature, boolean checkSight) {
        this(creature, checkSight, false);
        this.setMutexBits(1);
    }

    public RatAITargetItems(EntityRat creature, boolean checkSight, boolean onlyNearby) {
        this(creature, 10, checkSight, onlyNearby, null);
    }

    public RatAITargetItems(EntityRat creature, int chance, boolean checkSight, boolean onlyNearby, @Nullable final Predicate<? super T> targetSelector) {
        super(creature, checkSight, onlyNearby);
        this.executionChance = chance;
        this.rat = creature;
        this.theNearestAttackableTargetSorter = new RatAITargetItems.Sorter(creature);
        this.targetEntitySelector = new Predicate<EntityItem>() {
            @Override
            public boolean apply(@Nullable EntityItem item) {
                ItemStack stack = item.getItem();
                if (rat.isItemTargetCommand()) {
                    return !stack.isEmpty() && rat.canRatPickupItem(stack);
                }else{
                    return !stack.isEmpty() && RatUtils.shouldRaidItem(stack) && rat.canRatPickupItem(stack);
                }
            }
        };
        this.setMutexBits(0);
    }

    @Override
    public boolean shouldExecute() {
        if (!rat.canMove() || this.taskOwner.isRiding() || rat.isInCage() || rat.isTargetCommand() && rat.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_FARMER)) {
            return false;
        }

        if(rat.getAttackTarget() != null){
            return false;
        }
        if (!this.mustUpdate) {
            long worldTime = this.taskOwner.world.getWorldTime() % 10;
            if (this.rat.getIdleTime() >= 100 && worldTime != 0) {
                return false;
            }
            if (this.rat.getRNG().nextInt(this.executionChance) != 0 && worldTime != 0) {
                return false;
            }
        }
        List<EntityItem> list = this.taskOwner.world.getEntitiesWithinAABB(EntityItem.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);


        if (list.isEmpty()) {
            return false;
        } else {
            Collections.sort(list, this.theNearestAttackableTargetSorter);
            this.targetEntity = list.get(0);
            this.mustUpdate = false;
            prevPositionVector = this.rat.getPositionVector();
            return true;
        }
    }

    protected double getTargetDistance() {
        return rat.getSearchRadius();
    }


    protected AxisAlignedBB getTargetableArea(double targetDistance) {
        AxisAlignedBB bb = new AxisAlignedBB(-targetDistance, -targetDistance, -targetDistance, targetDistance, targetDistance, targetDistance);
        return bb.offset(rat.getSearchCenter());
    }

    @Override
    public void startExecuting() {
        this.taskOwner.getNavigator().tryMoveToXYZ(this.targetEntity.posX, this.targetEntity.posY, this.targetEntity.posZ, 1);
        super.startExecuting();
    }

    @Override
    public void updateTask() {
        super.updateTask();
        if (this.targetEntity == null || this.targetEntity != null && this.targetEntity.isDead) {
            this.resetTask();
            this.taskOwner.getNavigator().clearPath();
        }
        stuckTimer++;
        if (stuckTimer >= 7) // Check if the rat is stuck roughly every second
        {
            if (prevPositionVector.distanceTo(this.rat.getPositionVector()) < 1) // Even when the rat is jumping up blocks it shouldn't return a number lower than 1 meaning the rat is probably stuck
                stuckTimeOut++; // 60 seconds (one minute) to avoid exploitation
            else
                stuckTimeOut = 0; // The rat need to be continuously stuck for 60 seconds
            prevPositionVector = this.rat.getPositionVector(); // Save the previous second's position vector to be compared with the current later. For some reason prevPos and lastTickPos have the same value as the current location so i had to save the previous location myself
            stuckTimer = 0; // Reset the stuck timer
        }
        if (stuckTimeOut >= 60) // If the rat is stuck for 60 seconds
        {
            this.targetEntity.setPosition(this.rat.posX, this.rat.posY, this.rat.posZ); // Tp the item to the rat's position
            stuckTimeOut = 0; // Reset the time out timer back to 0
        }
        if (this.targetEntity != null && !this.targetEntity.isDead && this.taskOwner.getDistanceSq(this.targetEntity) < 1 && shouldKeepGathering()) {
            EntityRat rat = (EntityRat) this.taskOwner;
            ItemStack duplicate = this.targetEntity.getItem().copy();
            int extractSize = rat.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_PLATTER) ? this.targetEntity.getItem().getCount() : 1;
            duplicate.setCount(extractSize);
            this.targetEntity.getItem().shrink(extractSize);
            if (!rat.getHeldItem(EnumHand.MAIN_HAND).isEmpty() && !rat.world.isRemote) {
                rat.entityDropItem(rat.getHeldItem(EnumHand.MAIN_HAND), 0.0F);
            }
            rat.setHeldItem(EnumHand.MAIN_HAND, duplicate);
            if (this.targetEntity.getThrower() != null) {
                EntityPlayer targetPlayer = this.taskOwner.world.getPlayerEntityByName(this.targetEntity.getThrower());
                if (targetPlayer != null && RatUtils.isCheese(duplicate)) {
                    if (!rat.isTamed() && rat.canBeTamed()) {
                        rat.wildTrust += 10;
                        rat.cheeseFeedings++;
                        rat.world.setEntityState(rat, (byte) 82);
                        if (rat.wildTrust >= 100 && rat.getRNG().nextInt(3) == 0 || rat.cheeseFeedings >= 15) {
                            rat.world.setEntityState(rat, (byte) 83);
                            rat.setTamed(true);
                            rat.setOwnerId(targetPlayer.getUniqueID());
                            rat.setOwnerMonster(false);
                            rat.setCommand(RatCommand.FOLLOW);
                        }
                    } else {
                        String untameableText = "entity.rat.untameable";
                        if (rat.getOwner() != null && !rat.getOwnerId().equals(targetPlayer.getUniqueID())) {
                            untameableText = "entity.rat.tamed_by_other";
                        }
                        if (!rat.isOwner(targetPlayer)) {
                            targetPlayer.sendStatusMessage(new TextComponentTranslation(untameableText), true);
                        }
                    }

                }
            }
            resetTask();
        }
    }

    public void makeUpdate() {
        this.mustUpdate = true;
    }

    private boolean shouldKeepGathering(){
        return rat.getHeldItemMainhand().isEmpty() || rat.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_PLATTER) && rat.getHeldItemMainhand().getCount() < 64;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.taskOwner.getNavigator().noPath();
    }

    public static class Sorter implements Comparator<Entity> {
        private final Entity theEntity;

        public Sorter(Entity theEntityIn) {
            this.theEntity = theEntityIn;
        }

        public int compare(Entity p_compare_1_, Entity p_compare_2_) {
            double d0 = this.theEntity.getDistanceSq(p_compare_1_);
            double d1 = this.theEntity.getDistanceSq(p_compare_2_);
            return d0 < d1 ? -1 : (d0 > d1 ? 1 : 0);
        }
    }

}
