package com.github.alexthe666.rats.server.entity.ai;

import com.github.alexthe666.rats.server.entity.EntityRat;
import com.github.alexthe666.rats.server.entity.RatCommand;
import com.github.alexthe666.rats.server.entity.RatUtils;
import com.github.alexthe666.rats.server.items.RatsItemRegistry;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Comparator;

public class RatAIDepositInInventory extends EntityAIBase {
    private static final int RADIUS = 16;
    private final EntityRat entity;
    private final BlockSorter targetSorter;
    private BlockPos targetBlock = null;
    private int feedingTicks;
    private int breakingTime;
    private int previousBreakProgress;
    private int stuckTimer;
    private int stuckTimeOut;
    private Vec3d prevPositionVector;

    public RatAIDepositInInventory(EntityRat entity) {
        super();
        this.entity = entity;
        this.targetSorter = new BlockSorter(entity);
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.entity.canMove() || !this.entity.isTamed() || this.entity.getCommand() != RatCommand.TRANSPORT && this.entity.getCommand() != RatCommand.GATHER && this.entity.getCommand() != RatCommand.HUNT_ANIMALS && this.entity.getCommand() != RatCommand.HUNT_MONSTERS && this.entity.getCommand() != RatCommand.HARVEST || entity.getAttackTarget() != null) {
            return false;
        }
        if(!this.entity.shouldDepositItem(entity.getHeldItemMainhand())){
            return false;
        }
        if (this.entity.getHeldItem(EnumHand.MAIN_HAND).isEmpty()) {
            return false;
        }
        resetTarget();
        return targetBlock != null;
    }

    private void resetTarget() {
        this.targetBlock = entity.depositPos;
        prevPositionVector = this.entity.getPositionVector();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return targetBlock != null && !this.entity.getHeldItem(EnumHand.MAIN_HAND).isEmpty() && this.entity.shouldDepositItem(entity.getHeldItemMainhand());
    }

    public void resetTask() {
        this.entity.getNavigator().clearPath();
        resetTarget();
    }

    public boolean canSeeChest() {
        RayTraceResult rayTrace = RatUtils.rayTraceBlocksIgnoreRatholes(entity.world, entity.getPositionVector(), new Vec3d(targetBlock.getX() + 0.5, targetBlock.getY() + 0.5, targetBlock.getZ() + 0.5), false);
        if (rayTrace != null && rayTrace.hitVec != null) {
            BlockPos sidePos = rayTrace.getBlockPos();
            BlockPos pos = new BlockPos(rayTrace.hitVec);
            return entity.world.isAirBlock(sidePos) || entity.world.isAirBlock(pos) || this.entity.world.getTileEntity(pos) == this.entity.world.getTileEntity(targetBlock);
        }

        return true;
    }

    private BlockPos getMovePos() {
        return this.targetBlock.offset(this.entity.depositFacing);
    }

    @Override
    public void updateTask() {
        if (this.targetBlock != null && this.entity.world.getTileEntity(this.targetBlock) != null) {
            TileEntity te = this.entity.world.getTileEntity(this.targetBlock);
            //break block if has miner upgrade
            if (this.entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_MINER) && !entity.getMoveHelper().isUpdating() && entity.onGround && !this.entity.getNavigator().tryMoveToXYZ(getMovePos().getX() + 0.5D, getMovePos().getY(), getMovePos().getZ() + 0.5D, 1D)) {
                BlockPos rayPos = this.entity.rayTraceBlockPos(this.targetBlock.up());
                if (rayPos != null && !rayPos.equals(targetBlock)) {
                    IBlockState block = this.entity.world.getBlockState(rayPos);
                    if (RatUtils.canRatBreakBlock(this.entity.world, rayPos, this.entity) && block.getMaterial().blocksMovement() && block.getMaterial() != Material.AIR) {
                        double distance = this.entity.getDistance(rayPos.getX(), rayPos.getY(), rayPos.getZ());
                        SoundType soundType = block.getBlock().getSoundType(block, this.entity.world, rayPos, null);
                        if (distance < 2.5F) {
                            this.entity.world.setEntityState(this.entity, (byte) 85);
                            this.entity.crafting = true;
                            if (distance < 0.6F) {
                                this.entity.motionZ *= 0.0D;
                                this.entity.motionX *= 0.0D;
                                this.entity.getNavigator().clearPath();
                                this.entity.getMoveHelper().action = EntityMoveHelper.Action.WAIT;
                            }
                            breakingTime++;
                            int hardness = (int) (block.getBlockHardness(this.entity.world, rayPos) * 100);
                            int i = (int) ((float) this.breakingTime / hardness * 10.0F);
                            if (breakingTime % 10 == 0) {
                                this.entity.playSound(soundType.getHitSound(), soundType.volume, soundType.pitch);
                                this.entity.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.5F);
                            }
                            if (i != this.previousBreakProgress) {
                                this.entity.world.sendBlockBreakProgress(this.entity.getEntityId(), rayPos, i);
                                this.previousBreakProgress = i;
                            }
                            if (this.breakingTime == hardness) {
                                this.entity.world.setEntityState(this.entity, (byte) 86);
                                entity.playSound(SoundEvents.ENTITY_ITEM_PICKUP, 1, 1F);
                                this.entity.playSound(soundType.getBreakSound(), soundType.volume, soundType.pitch);
                                this.breakingTime = 0;
                                this.previousBreakProgress = -1;
                                destroyBlock(rayPos, block);
                                this.entity.fleePos = rayPos;
                                targetBlock = null;
                                this.entity.crafting = false;
                                this.resetTask();
                            }
                        }
                    }
                }
            } else {
                this.entity.getNavigator().tryMoveToXYZ(getMovePos().getX() + 0.5D, getMovePos().getY(), getMovePos().getZ() + 0.5D, 1D);
                double distance = this.entity.getDistance(this.targetBlock.getX() + 0.5D, this.targetBlock.getY() + 1, this.targetBlock.getZ() + 0.5D);
                stuckTimer++;
                if (stuckTimer >= 20) // Check if the rat is stuck roughly every second
                {
                    if (prevPositionVector.distanceTo(this.entity.getPositionVector()) < 1) // Even when the rat is jumping up blocks it shouldn't return a number lower than 1 meaning the rat is probably stuck
                        stuckTimeOut++; // 60 seconds (one minute) to avoid exploitation
                    else
                        stuckTimeOut = 0; // The rat need to be continuously stuck for 60 seconds
                    prevPositionVector = this.entity.getPositionVector(); // Save the previous second's position vector to be compared with the current later. For some reason prevPos and lastTickPos have the same value as the current location so i had to save the previous location myself
                    stuckTimer = 0; // Reset the stuck timer
                }
                if (distance < 2.5 && distance >= 1.86 && canSeeChest() && te instanceof IInventory) {
                    toggleChest((IInventory) te, true);
                }
                if ((distance < 1.86 || stuckTimeOut >= 60) && canSeeChest()) { // Skip the distance check if the rat is stuck for 60 seconds
                    if (te instanceof IInventory) {
                        toggleChest((IInventory) te, false);
                    }
                    IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, this.entity.depositFacing);
                    ItemStack duplicate = this.entity.getHeldItem(EnumHand.MAIN_HAND).copy();
                    if (duplicate.getCount() != ItemHandlerHelper.insertItem(handler, duplicate, true).getCount()) {
                        this.entity.setHeldItem(EnumHand.MAIN_HAND, ItemHandlerHelper.insertItem(handler, duplicate, false));
                        this.targetBlock = null;
                        stuckTimeOut = 0; // Reset the time out timer back to 0
                        this.resetTask();
                    }

                }
            }

        }
    }

    private void destroyBlock(BlockPos pos, IBlockState state) {
        NonNullList<ItemStack> drops = NonNullList.create();
        state.getBlock().getDrops(drops, this.entity.world, pos, state, 0);
        if (!drops.isEmpty() && entity.canRatPickupItem(drops.get(0))) {
            for (ItemStack drop : drops) {
                this.entity.entityDropItem(drop, 0);
            }
            this.entity.world.destroyBlock(pos, false);
            this.entity.fleePos = pos;
        }
    }

    public void toggleChest(IInventory te, boolean open) {
        if (te instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) te;
            if (open) {
                chest.numPlayersUsing++;
                this.entity.world.addBlockEvent(this.targetBlock, chest.getBlockType(), 1, chest.numPlayersUsing);
            } else {
                if (chest.numPlayersUsing > 0) {
                    chest.numPlayersUsing = 0;
                    this.entity.world.addBlockEvent(this.targetBlock, chest.getBlockType(), 1, chest.numPlayersUsing);
                }
            }
        }
    }

    public class BlockSorter implements Comparator<BlockPos> {
        private final Entity entity;

        public BlockSorter(Entity entity) {
            this.entity = entity;
        }

        @Override
        public int compare(BlockPos pos1, BlockPos pos2) {
            double distance1 = this.getDistance(pos1);
            double distance2 = this.getDistance(pos2);
            return Double.compare(distance1, distance2);
        }

        private double getDistance(BlockPos pos) {
            double deltaX = this.entity.posX - (pos.getX() + 0.5);
            double deltaY = this.entity.posY + this.entity.getEyeHeight() - (pos.getY() + 0.5);
            double deltaZ = this.entity.posZ - (pos.getZ() + 0.5);
            return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        }
    }
}
