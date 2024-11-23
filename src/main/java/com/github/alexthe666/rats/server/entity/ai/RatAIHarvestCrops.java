package com.github.alexthe666.rats.server.entity.ai;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.entity.EntityRat;
import com.github.alexthe666.rats.server.entity.RatCommand;
import com.github.alexthe666.rats.server.items.RatsItemRegistry;
import com.google.common.collect.Lists;
import net.minecraft.block.*;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.IPlantable;
import net.minecraft.item.ItemBlock;

import java.util.*;

public class RatAIHarvestCrops extends EntityAIBase {
    private final EntityRat entity;
    private final BlockSorter targetSorter;
    private BlockPos targetBlock = null;
    private int feedingTicks;
    private static final List<EnumFacing> GrowDirections = Lists.newArrayList(EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST);
    private static final Random rand = new Random();
    private NBTTagCompound nbtDefault = new NBTTagCompound();
    private static final String[] isCropGrownList = new String[] {"rustic:grape_stem", "rustic:leaves_apple", "rustic:grape_leaves", "immersiveengineering:hemp", "botania:redstringfertilizer", "botania:shinyflower", "minecraft:double_plant", "minecraft:tallgrass", "forestry:leaves", "botania:doubleflower", "quark:roots", "mysticalcreations:"};
    private static final String[] customDropsList = new String[] {"forestry:leaves", "forestry:pods.", "randomthings:lotus", "rustic:wildberry_bush", "rustic:aloe_vera", "rustic:blood_orchid", "rustic:chamomile", "rustic:cloudsbluff", "rustic:cohosh", "rustic:core_root", "rustic:deathstalk_mushroom", "rustic:ginseng", "rustic:horsetail", "rustic:marsh_mallow", "rustic:mooncap_mushroom", "rustic:wind_thistle", "rustic:leaves_apple", "rustic:tomato_crop", "rustic:chili_crop", Blocks.COCOA.getRegistryName().toString(), "randomthings:beansprout", "rustic:grape_leaves"};
    private static final String[] customDropsPamList = new String[] {"harvestcraft:dateitem", "harvestcraft:papayaitem", "harvestcraft:cherryitem", "harvestcraft:figitem", "harvestcraft:soursopitem", "harvestcraft:dragonfruititem", "harvestcraft:rambutanitem", "harvestcraft:jackfruititem", "harvestcraft:passionfruititem", Items.APPLE.getRegistryName().toString(), "harvestcraft:lemonitem", "harvestcraft:pearitem", "harvestcraft:oliveitem", Items.STRING.getRegistryName().toString(), "harvestcraft:grapefruititem", "harvestcraft:pomegranateitem", "harvestcraft:cashewitem", "harvestcraft:vanillabeanitem", "harvestcraft:starfruititem", "harvestcraft:bananaitem", "harvestcraft:plumitem", "harvestcraft:avocadoitem", "harvestcraft:pecanitem", "harvestcraft:pistachioitem", "harvestcraft:hazelnutitem", "harvestcraft:limeitem", "harvestcraft:peppercornitem", "harvestcraft:almonditem", "harvestcraft:gooseberryitem", "harvestcraft:peachitem", "harvestcraft:chestnutitem", "harvestcraft:pawpawitem", "harvestcraft:coconutitem", "harvestcraft:mangoitem", "harvestcraft:apricotitem", "harvestcraft:orangeitem", "harvestcraft:walnutitem", "harvestcraft:lycheeitem", "harvestcraft:persimmonitem", "harvestcraft:guavaitem", "harvestcraft:breadfruititem", "harvestcraft:nutmegitem", "harvestcraft:durianitem", "harvestcraft:tamarinditem", "harvestcraft:cinnamonitem", "harvestcraft:maplesyrupitem", Items.PAPER.getRegistryName().toString()};

    public RatAIHarvestCrops(EntityRat entity) {
        super();
        this.entity = entity;
        this.targetSorter = new BlockSorter(entity);
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.entity.canMove() || !this.entity.isTamed() || this.entity.getCommand() != RatCommand.HARVEST || this.entity.isInCage()) {
            return false;
        }
        if (!this.entity.getHeldItem(EnumHand.MAIN_HAND).isEmpty()) {
            return false;
        }
        resetTarget();
        return targetBlock != null;
    }

    private boolean isCropGrown(IBlockState state, BlockPos pos) // Checks if the crop is fully grown. It was too long and used in many places, so I decide to make it into a method
    {
        Block block = state.getBlock();
        Block block2 = this.entity.world.getBlockState(pos.down()).getBlock();
        int meta = block.getMetaFromState(state);
        if (block.getRegistryName().toString().equals(isCropGrownList[8])) // Forestry's leaves are a bit weird so we need a custom logic for them otherwise it causes glitches
        {
            TileEntity tileentity = this.entity.world.getTileEntity(pos); // Get that leave's TE for null check
            if (tileentity != null)
            {
                nbtDefault = tileentity.writeToNBT(nbtDefault);
                return nbtDefault.getBoolean("FL") && !((IGrowable) block).canGrow(this.entity.world, pos, state, this.entity.world.isRemote); // The FL boolean aka "IsFruitLeaf" defines if the leaves is a fruit leaves or not, we only want the fruit leaves that is fully grown
            }
        }
        return (block instanceof IGrowable && !((IGrowable) block).canGrow(this.entity.world, pos, state, this.entity.world.isRemote) && !(block instanceof BlockStem || Arrays.stream(isCropGrownList).anyMatch(block.getRegistryName().toString()::contains))) || ((block instanceof BlockCrops || block.getRegistryName().toString().contains(isCropGrownList[11])) && meta == 7) || ((block == Blocks.PUMPKIN || block == Blocks.MELON_BLOCK) && !(block instanceof BlockStem)) || ((block == Blocks.CACTUS || block == Blocks.REEDS || block.getRegistryName().toString().equals(isCropGrownList[3])) && block == block2) || block == Blocks.NETHER_WART && meta == 3 || block.getRegistryName().toString().equals(isCropGrownList[1]) && state.getValue((PropertyInteger) block.getBlockState().getProperty("apple_age")) == 3 || block.getRegistryName().toString().equals("rustic:wildberry_bush") && meta == 1 || block.getRegistryName().toString().equals(isCropGrownList[2]) && state.getValue((PropertyBool) block.getBlockState().getProperty("grapes"));
    }

    private List<BlockPos> chorusFlowerAmount(IBlockState state, BlockPos pos) // Checks if the chorus flower is ready
    {
        List<BlockPos> chorusFlowers = new ArrayList<>();
        Block blockBelow = this.entity.world.getBlockState(pos.down()).getBlock();
        if (state.getBlock() == Blocks.CHORUS_PLANT && blockBelow == Blocks.END_STONE) // Checks if the block is a chorus plant and the block below it is an end stone (so the very root of the chorus)
        {
            BlockPos pos2 = pos; // Creates a new BlockPos to be used in the loop to change locations
            List<BlockPos> nextPositions = new ArrayList<>(); // Save the next chorus plants so it can jump to one after the direction loop is done
            List<BlockPos> prevPositions = new ArrayList<>(); // Save previous chorus plants so it don't get stuck in a loop
            int j = 1;
            outerLoop:
            for (int i = 0; i < j; ++i) // Main loop
            {
                for (EnumFacing direction : GrowDirections) // Keep looping between the possible directions that chorus can grow to
                {
                    BlockPos neighbourPos = pos2.offset(direction); // Get the neighbour BlockPos
                    IBlockState neighbourState = this.entity.world.getBlockState(neighbourPos); // Get the neighbour IBlockState
                    if (neighbourState.getBlock() == Blocks.CHORUS_FLOWER && !chorusFlowers.contains(neighbourPos)) // Checks if it is a chorus flower, and it isn't in the chorus flower's list yet
                    {
                        chorusFlowers.add(neighbourPos); // Add that chorus flower's position to the list
                        if (direction != EnumFacing.EAST) // Only continue if it isn't at the EAST direction as it need to move to the next position in the last direction aka the EAST direction
                            continue;
                    }
                    else if (neighbourState.getBlock() == Blocks.CHORUS_PLANT && !prevPositions.contains(neighbourPos)) // Checks if it is chorus plant, and it isn't in the previous positions' list
                    {
                        nextPositions.add(neighbourPos); // Add that chorus plant's position to the list to be switched to later
                        if (direction != EnumFacing.EAST)
                            continue;
                    }
                    if (direction == EnumFacing.EAST) // This should only be done in the last direction, and after it was checked for chorus plant/flower in all directions
                    {
                        if (nextPositions.size() > 1) // If it found more than one chorus plant
                        {
                            prevPositions.add(pos2); // Add the current BlockPos (not the direction offset) to the previous positions' list
                            pos2 = nextPositions.get(rand.nextInt(nextPositions.size() - 1)); // Get a random chorus plant off the list to switch to
                            nextPositions.remove(pos2); // Remove that chorus plant's position off the list
                            ++j;
                            break; // Break and go to the next loop with the new position
                        }
                        else if (nextPositions.size() == 1) // If it found only one chorus plant
                        {
                            pos2 = nextPositions.get(0); // Get the first chorus plant in the list as there is only one, we know the index
                            nextPositions.remove(0);
                            ++j;
                            break;
                        }
                        else // If it found no more chorus plants to jump to
                            break outerLoop; // Break the outerLoop using the label
                    }
                }
            }
            return chorusFlowers; // Return the chorus flowers that it found, if any
        }
        else if (state.getBlock() == Blocks.CHORUS_FLOWER && state.getValue(BlockChorusFlower.AGE) == 5 && blockBelow == Blocks.END_STONE) // Checks if the block is a chorus flower and is at age 5 and the block below is an end stone
        {
            chorusFlowers.add(pos); // Add the block pos to the list
            return chorusFlowers; // Return one in size()
        }
        else
            return chorusFlowers; // Return zero in size()
    }

    private void resetTarget() {
        List<BlockPos> allBlocks = new ArrayList<>();
        int RADIUS = this.entity.getSearchRadius();
        for (BlockPos pos : BlockPos.getAllInBox(this.entity.getSearchCenter().add(-RADIUS, -RADIUS, -RADIUS), this.entity.getSearchCenter().add(RADIUS, RADIUS, RADIUS))) {
            IBlockState block = this.entity.world.getBlockState(pos);
            List<BlockPos> chorusFlowerAmount = chorusFlowerAmount(block, pos);
            if (isCropGrown(block, pos) || chorusFlowerAmount.size() > 1 || (chorusFlowerAmount.size() == 1 && this.entity.world.getBlockState(chorusFlowerAmount.get(0)).getValue(BlockChorusFlower.AGE) == 5)) { // It only considers a chorus ready if it is more than 1 (so 2 or more) or if there is only one and that chorus is fully grown (age 5), meaning it was grown into the rare form of having one single chorus flower
                allBlocks.add(pos);
            }
        }
        if (!allBlocks.isEmpty()) {
            allBlocks.sort(this.targetSorter);
            this.targetBlock = allBlocks.get(0);
        }
    }

    @Override
    public boolean shouldContinueExecuting() {
        return targetBlock != null && this.entity.getHeldItem(EnumHand.MAIN_HAND).isEmpty() && (isCropGrown(this.entity.world.getBlockState(targetBlock), targetBlock) || chorusFlowerAmount(this.entity.world.getBlockState(targetBlock), targetBlock).size() > 1 || (chorusFlowerAmount(this.entity.world.getBlockState(targetBlock), targetBlock).size() == 1 && this.entity.world.getBlockState(chorusFlowerAmount(this.entity.world.getBlockState(targetBlock), targetBlock).get(0)).getValue(BlockChorusFlower.AGE) == 5));
    }

    public void resetTask() {
        this.entity.getNavigator().clearPath();
        resetTarget();
    }

    @SuppressWarnings("UnusedReturnValue")
    private NonNullList<ItemStack> customDrops(NonNullList<ItemStack> drops, IBlockState state) // Remove incorrect drops or add custom drops to the drops' list as sometimes it doesn't get it right. It was too long and used in many places, so I decide to make it into a method
    {
        int k;
        for (k = 0; k < customDropsList.length; k++)
        {
            if (k == 0) // This only need to run once as if any of the options is true, it will break the loop
            {
                if (state.getBlock().getRegistryName().toString().equals("extrautils2:enderlilly") || state.getBlock().getRegistryName().toString().equals("extrautils2:redorchid"))
                {
                    k = -3;
                    break;
                }
                if (state.getBlock() == Blocks.CHORUS_PLANT || state.getBlock() == Blocks.CHORUS_FLOWER)
                {
                    k = -2;
                    break;
                }
                if (state.getBlock().getRegistryName().toString().startsWith("harvestcraft:pam") && !state.getBlock().getRegistryName().toString().endsWith("crop"))
                {
                    k = -1;
                    break;
                }
            }
            if (state.getBlock().getRegistryName().toString().contains(customDropsList[k]))
                break;
        }
        switch (k)
        {
            case -3:
                if (!RatsMod.CONFIG_OPTIONS.ratsBreakBlockOnHarvest || entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_REPLANTER)) // Remove one seed from customReplant crops that don't need any special treatment for its drops when replanting
                {
                    for (int i = 0; i < drops.size(); ++i) {
                        if (isPlantable(drops.get(i).getItem())) {
                            if (drops.get(i).getCount() == 1)
                                drops.remove(i);
                            else
                                drops.get(i).setCount(drops.get(i).getCount() - 1);
                            break;
                        }
                    }
                }
                break;
            case -2:
                if (!RatsMod.CONFIG_OPTIONS.ratsBreakBlockOnHarvest || entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_REPLANTER))
                {
                    if (chorusFlowerAmount(state, targetBlock).size() != 1) // Manually add the chorus flowers to the drops list. One is removed for re-plantation. It don't put any chorus flower if it is only one chorus flower (as that was used to replant)
                        drops.add(new ItemStack(Blocks.CHORUS_FLOWER, chorusFlowerAmount(state, targetBlock).size() - 1));
                }
                else
                    drops.add(new ItemStack(Blocks.CHORUS_FLOWER, chorusFlowerAmount(state, targetBlock).size())); // Put the actual amount of chorus as it wasn't replanted
                break;
            case -1:
                if (!RatsMod.CONFIG_OPTIONS.ratsBreakBlockOnHarvest || entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_REPLANTER)) { // Remove an extra fruit from Pam's fruit trees when replanting
                    for (int i = 0; i < drops.size(); ++i) {
                        if (Arrays.stream(customDropsPamList).anyMatch(drops.get(i).getItem().getRegistryName().toString()::equals)) {
                            if (drops.get(i).getCount() == 1)
                                drops.remove(i);
                            else
                                drops.get(i).setCount(drops.get(i).getCount() - 1);
                            break;
                        }
                    }
                }
                break;
            case 0:
                if (!RatsMod.CONFIG_OPTIONS.ratsBreakBlockOnHarvest || entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_REPLANTER)) // Remove the sapling that getDrops occasionally get when replanting
                {
                    for (int i = 0; i < drops.size(); ++i)
                    {
                        if (drops.get(i).getItem().getRegistryName().toString().equals("forestry:sapling"))
                        {
                            if (drops.get(i).getCount() == 1)
                                drops.remove(i);
                            else
                                drops.get(i).setCount(drops.get(i).getCount() - 1);
                        }
                    }
                }
                break;
            case 1:
                if (state.getBlock().getRegistryName().toString().equals("forestry:pods.papaya"))
                    drops.add(new ItemStack(Item.getByNameOrId("forestry:fruits"), 1, 6)); // getDrops returns nothing for Forestry's pods, so I have to add them manually
                if (state.getBlock().getRegistryName().toString().equals("forestry:pods.dates"))
                    drops.add(new ItemStack(Item.getByNameOrId("forestry:fruits"), 4, 5)); // Strangely all fruits trees have a fixed drop amount of fruits, no genetics can alter that. Makes things easier for me tbh
                break;
            case 2:
                drops.add(new ItemStack(Item.getByNameOrId("randomthings:ingredient"), 1, 10)); // Random Things' Lotus don't have a getDrops method, so I fix it here
                break;
            case 3:
                if (!RatsMod.CONFIG_OPTIONS.ratsBreakBlockOnHarvest || entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_REPLANTER)) // Remove Wildberry Bush when replanting
                    drops.clear();
                drops.add(new ItemStack(Item.getByNameOrId("rustic:wildberries")));
                break;
            case 4:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:aloe_vera"), rand.nextInt(2) + 1)); // For some reason, getDrops return only one drop instead of 2-3, so I have to fix it here
                break;
            case 5:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:blood_orchid"), rand.nextInt(2) + 1));
                break;
            case 6:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:chamomile"), rand.nextInt(2) + 1));
                break;
            case 7:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:cloudsbluff"), rand.nextInt(2) + 1));
                break;
            case 8:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:cohosh"), rand.nextInt(2) + 1));
                break;
            case 9:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:core_root"), rand.nextInt(2) + 1));
                break;
            case 10:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:deathstalk_mushroom"), rand.nextInt(2) + 1));
                break;
            case 11:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:ginseng"), rand.nextInt(2) + 1));
                break;
            case 12:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:horsetail"), rand.nextInt(2) + 1));
                break;
            case 13:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:marsh_mallow"), rand.nextInt(2) + 1));
                break;
            case 14:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:mooncap_mushroom"), rand.nextInt(2) + 1));
                break;
            case 15:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:wind_thistle"), rand.nextInt(2) + 1));
                break;
            case 16:
                if (!RatsMod.CONFIG_OPTIONS.ratsBreakBlockOnHarvest || entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_REPLANTER)) // Remove the sapling that getDrops occasionally get when replanting
                {
                    for (int i = 0; i < drops.size(); ++i)
                    {
                        if (drops.get(i).getItem().getRegistryName().toString().equals("rustic:sapling_apple"))
                        {
                            if (drops.get(i).getCount() == 1)
                                drops.remove(i);
                            else
                                drops.get(i).setCount(drops.get(i).getCount() - 1);
                        }
                    }
                }
                break;
            case 17:
            case 18:
                drops.clear(); // Rustic's tomato and chili crop have an odd breakBlock so items still drop regardless, to avoid going too complex to fix this minor issue, I will just let it handle the drops
                break;
            case 19:
                if (!RatsMod.CONFIG_OPTIONS.ratsBreakBlockOnHarvest || entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_REPLANTER)) { // Cocoa Beans aren't considered seeds so we remove one cocoa bean when replanting
                    for (int i = 0; i < drops.size(); ++i) {
                        if (drops.get(i).getItem() == Items.DYE && EnumDyeColor.byDyeDamage(drops.get(i).getMetadata()) == EnumDyeColor.BROWN) {
                            if (drops.get(i).getCount() == 1)
                                drops.remove(i);
                            else
                                drops.get(i).setCount(drops.get(i).getCount() - 1);
                            break;
                        }
                    }
                }
                break;
            case 20:
                drops.add(new ItemStack(Item.getByNameOrId("randomthings:beans"), rand.nextInt(2) + 2)); // RandomThings' beans are weird: right-clicking it give 1-2, breaking it give 3-4, and getDrops return only one bean. I decided to go with the highest (3-4)
                if (!RatsMod.CONFIG_OPTIONS.ratsBreakBlockOnHarvest || entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_REPLANTER))
                {
                    for (int i = 0; i < drops.size(); ++i) {
                        if (drops.get(i).getItem().getRegistryName().toString().equals("randomthings:beans")) {
                            if (drops.get(i).getCount() == 1)
                                drops.remove(i);
                            else
                                drops.get(i).setCount(drops.get(i).getCount() - 1);
                            break;
                        }
                    }
                }
                break;
            case 21:
                drops.add(new ItemStack(Item.getByNameOrId("rustic:grapes"), rand.nextInt(2))); // getDrops give one grape but right-clicking give 1-2 grapes. I'm sticking with the highest (1-2)
                break;
        }
        return drops;
    }

    private void customReplant(IBlockState state) // Some blocks need a special treatment when replanting
    {
        if (state.getBlock().getRegistryName().toString().equals("rustic:wildberry_bush"))
        {
            this.entity.world.destroyBlock(targetBlock, false);
            this.entity.world.setBlockState(targetBlock, state.withProperty((PropertyBool) state.getBlock().getBlockState().getProperty("berries"), false));
        }
        if (state.getBlock().getRegistryName().toString().equals("rustic:leaves_apple"))
        {
            this.entity.world.destroyBlock(targetBlock, false);
            this.entity.world.setBlockState(targetBlock, state.withProperty((PropertyInteger) state.getBlock().getBlockState().getProperty("apple_age"), 0));
        }
        if (state.getBlock().getRegistryName().toString().equals("rustic:tomato_crop") || state.getBlock().getRegistryName().toString().equals("rustic:chili_crop"))
        {
            this.entity.world.destroyBlock(targetBlock, false);
            this.entity.world.setBlockState(targetBlock, state.withProperty((PropertyInteger) state.getBlock().getBlockState().getProperty("age"), 2)); // The dev want it to reset to age 2 not 0
        }
        if (state.getBlock().getRegistryName().toString().equals("rustic:grape_leaves"))
        {
            this.entity.world.destroyBlock(targetBlock, false);
            this.entity.world.setBlockState(targetBlock, state.withProperty((PropertyBool) state.getBlock().getBlockState().getProperty("grapes"), false));
        }
        if (state.getBlock().getRegistryName().toString().equals("extrautils2:enderlilly") || state.getBlock().getRegistryName().toString().equals("extrautils2:redorchid"))
        {
            this.entity.world.destroyBlock(targetBlock, false);
            this.entity.world.setBlockState(targetBlock, state.withProperty((PropertyInteger) state.getBlock().getBlockState().getProperty("growth"), 0));
        }
        if (state.getBlock().getRegistryName().toString().equals("forestry:leaves") || state.getBlock().getRegistryName().toString().contains("forestry:pods."))
        {
            TileEntity tileEntity = this.entity.world.getTileEntity(targetBlock);
            if (tileEntity != null)
            {
                nbtDefault = tileEntity.writeToNBT(nbtDefault); // Save the leaves or pod's nbt tag for later
                NBTTagCompound nbtCocoon = new NBTTagCompound();
                IBlockState stateBelow = this.entity.world.getBlockState(targetBlock.down()); // Used to check if the block below is a cocoon
                if (stateBelow.getBlock().getRegistryName().toString().equals("forestry:cocoon"))
                {
                    TileEntity cocoonTE = this.entity.world.getTileEntity(targetBlock.down());
                    if (cocoonTE != null)
                    {
                        nbtCocoon = cocoonTE.writeToNBT(nbtCocoon); // Save the cocoon's nbt data for later
                        this.entity.world.destroyBlock(targetBlock.down(), false); // Break it the normal way to avoid it dropping any item
                    }
                }
                this.entity.world.destroyBlock(targetBlock, false);
                if (state.getBlock().getRegistryName().toString().contains("forestry:pods."))
                {
                    this.entity.world.setBlockState(targetBlock, state.withProperty(BlockCocoa.AGE, 0)); // Forestry's pods (date and papaya) extends Minecraft's cocoa block, so I have to reset their age to 0
                    nbtDefault.setShort("MT", (short) 0); // MT aka "Maturity" defines the age of the pod
                    tileEntity = this.entity.world.getTileEntity(targetBlock);
                    if (tileEntity != null)
                        tileEntity.readFromNBT(nbtDefault); // Re-apply the modified nbt data of pod
                }
                else
                    this.entity.world.setBlockState(targetBlock, state);
                if (state.getBlock().getRegistryName().toString().equals("forestry:leaves"))
                {
                    nbtDefault.setShort("RT", (short) 0); // RT aka "Ripening Time" defines the age of the fruit so we set it back to 0
                    tileEntity = this.entity.world.getTileEntity(targetBlock);
                    if (tileEntity != null)
                        tileEntity.readFromNBT(nbtDefault); // Re-apply the modified nbt data of the fruit leaves
                    if (!nbtCocoon.isEmpty()) // Re-add the cocoon if it exists
                    {
                        this.entity.world.setBlockState(targetBlock.down(), stateBelow);
                        TileEntity cocoonTE = this.entity.world.getTileEntity(targetBlock.down());
                        if (cocoonTE != null)
                            cocoonTE.readFromNBT(nbtCocoon);
                    }
                }
            }
        }
        if (state.getBlock() == Blocks.CHORUS_PLANT || state.getBlock() == Blocks.CHORUS_FLOWER)
        {
            this.entity.world.destroyBlock(targetBlock, false);
            if (state.getBlock() == Blocks.CHORUS_PLANT)
                this.entity.world.setBlockState(targetBlock, Blocks.CHORUS_FLOWER.getDefaultState());
            else
                this.entity.world.setBlockState(targetBlock, state.withProperty(BlockChorusFlower.AGE, 0));
        }
    }

    @Override
    public void updateTask() {
        if (this.targetBlock != null) {
            IBlockState block = this.entity.world.getBlockState(this.targetBlock);
            List<BlockPos> chorusFlowerAmount = chorusFlowerAmount(block, targetBlock);
            if (isCropGrown(block, targetBlock) || chorusFlowerAmount.size() > 1 || (chorusFlowerAmount.size() == 1 && this.entity.world.getBlockState(chorusFlowerAmount.get(0)).getValue(BlockChorusFlower.AGE) == 5))
            {
                this.entity.getNavigator().tryMoveToXYZ(this.targetBlock.getX() + 0.5D, this.targetBlock.getY(), this.targetBlock.getZ() + 0.5D, 1D);
                double distance = this.entity.getDistance(this.targetBlock.getX(), this.targetBlock.getY(), this.targetBlock.getZ());
                if (distance < 2.5F) { // Increased by 1F for Cactus
                    NonNullList<ItemStack> drops = NonNullList.create();
                    block.getBlock().getDrops(drops, this.entity.world, targetBlock, block, 0);
                    if (drops.isEmpty())
                        drops.addAll(block.getBlock().getDrops(this.entity.world, targetBlock, block, 0)); // In case they use the deprecated version of getDrops
                    customDrops(drops, block);
                    if ((!RatsMod.CONFIG_OPTIONS.ratsBreakBlockOnHarvest || entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_REPLANTER)) && (block.getBlock().getRegistryName().toString().equals("rustic:tomato_crop") || block.getBlock().getRegistryName().toString().equals("rustic:chili_crop") || block.getBlock().getRegistryName().toString().equals("rustic:grape_leaves") || block.getBlock().getRegistryName().toString().equals("forestry:leaves") || block.getBlock().getRegistryName().toString().contains("forestry:pods.") || block.getBlock().getRegistryName().toString().equals("rustic:leaves_apple") || block.getBlock().getRegistryName().toString().equals("extrautils2:enderlilly") || block.getBlock() == Blocks.CHORUS_PLANT || block.getBlock() == Blocks.CHORUS_FLOWER || block.getBlock().getRegistryName().toString().equals("rustic:wildberry_bush") || block.getBlock().getRegistryName().toString().equals("extrautils2:redorchid")))
                        customReplant(block);
                    else
                        this.entity.world.destroyBlock(targetBlock, false);
                    if ((!RatsMod.CONFIG_OPTIONS.ratsBreakBlockOnHarvest || entity.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_REPLANTER)) && (block.getBlock() instanceof IGrowable || block.getBlock() == Blocks.NETHER_WART) && (!(block.getBlock().getRegistryName().toString().equals("rustic:tomato_crop") || block.getBlock().getRegistryName().toString().equals("rustic:chili_crop") || block.getBlock().getRegistryName().toString().equals("rustic:grape_leaves") || block.getBlock().getRegistryName().toString().equals("forestry:leaves") || block.getBlock().getRegistryName().toString().contains("forestry:pods.") || block.getBlock().getRegistryName().toString().equals("rustic:leaves_apple") || block.getBlock().getRegistryName().toString().equals("extrautils2:enderlilly") || block.getBlock().getRegistryName().toString().equals("rustic:wildberry_bush") || block.getBlock() == Blocks.COCOA || block.getBlock().getRegistryName().toString().equals("extrautils2:redorchid")))) {
                        for (int i = 0; i < drops.size(); ++i) {
                            if (isPlantable(drops.get(i).getItem())) {
                                if (drops.get(i).getCount() == 1)
                                    drops.remove(i);
                                else
                                    drops.get(i).setCount(drops.get(i).getCount() - 1);
                                break;
                            }
                        }
                        try
                        {
                            PropertyInteger age = (PropertyInteger) block.getBlock().getBlockState().getProperty("age");
                            this.entity.world.setBlockState(targetBlock, block.withProperty(age, Collections.min(age.getAllowedValues())));
                        } catch (Exception ignored) { }
                    }
                    if (!drops.isEmpty() && entity.canRatPickupItem(drops.get(0))) {
                        ItemStack duplicate = drops.get(0).copy();
                        drops.remove(0);
                        if (!this.entity.getHeldItem(EnumHand.MAIN_HAND).isEmpty() && !this.entity.world.isRemote) {
                            this.entity.entityDropItem(this.entity.getHeldItem(EnumHand.MAIN_HAND), 0.0F);
                        }
                        this.entity.setHeldItem(EnumHand.MAIN_HAND, duplicate);
                        for (ItemStack drop : drops) {
                            this.entity.entityDropItem(drop, 0);
                        }
                        //this.entity.fleePos = this.targetBlock; // Removing this make the rat not run to a random, nearby block (caused by his targetBlock becoming null in the lines below) sometimes
                    }
                    this.targetBlock = null;
                    this.resetTask();
                }
            }
            else // Adding this else allow the rat to immediately stop going after a no longer valid target, fixing a bug where the rat would get stuck going after an invalid target
            {
                this.targetBlock = null;
                this.resetTask();
            }
        }
    }

    private static boolean isPlantable(Item item) {
        if (item instanceof IPlantable) {
            return true;
        } else if (item instanceof ItemBlock) {
            Block block = Block.getBlockFromItem(item);
            return block instanceof IPlantable;
        }
        return false;
    }

    public static class BlockSorter implements Comparator<BlockPos> {
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
