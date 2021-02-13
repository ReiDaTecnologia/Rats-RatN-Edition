package com.github.alexthe666.rats.server.events;

import com.github.alexthe666.rats.RatsMod;
import com.github.alexthe666.rats.server.blocks.RatsBlockRegistry;
import com.github.alexthe666.rats.server.compat.TinkersCompatBridge;
import com.github.alexthe666.rats.server.entity.*;
import com.github.alexthe666.rats.server.items.RatsItemRegistry;
import com.github.alexthe666.rats.server.message.MessageRatDismount;
import com.github.alexthe666.rats.server.message.MessageSwingArm;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.RandomChance;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.github.alexthe666.rats.RatsMod.PLAGUE_MAX_HEALTH_MODIFIER_UUID;

@Mod.EventBusSubscriber
public class ServerEvents {

    Predicate<Entity> UNTAMED_RAT_SELECTOR = new Predicate<Entity>() {
        public boolean apply(@Nullable Entity p_apply_1_) {
            return p_apply_1_ instanceof EntityRat && !((EntityRat) p_apply_1_).isTamed();
        }
    };

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntityPlayer().getHeldItem(EnumHand.MAIN_HAND).getItem() == RatsItemRegistry.CHEESE_STICK || event.getEntityPlayer().getHeldItem(EnumHand.OFF_HAND).getItem() == RatsItemRegistry.CHEESE_STICK) {
            event.setUseBlock(Event.Result.DENY);
        }
        if (event.getEntityPlayer().getHeldItem(EnumHand.MAIN_HAND).getItem() == RatsItemRegistry.CHUNKY_CHEESE_TOKEN || event.getEntityPlayer().getHeldItem(EnumHand.OFF_HAND).getItem() == RatsItemRegistry.CHUNKY_CHEESE_TOKEN) {
            if (!RatsMod.CONFIG_OPTIONS.disableRatlantis) {
                if (!event.getEntityPlayer().isCreative()) {
                    event.getItemStack().shrink(1);
                }
                boolean canBuild = true;
                BlockPos pos = event.getPos().offset(event.getFace());
                for (int i = 0; i < 4; i++) {
                    IBlockState state = event.getWorld().getBlockState(pos.up(i));
                    if (state.getBlockHardness(event.getWorld(), pos.up(i)) == -1.0F) {
                        canBuild = false;
                    }
                }
                if (canBuild) {
                    event.getEntityPlayer().playSound(SoundEvents.BLOCK_END_PORTAL_SPAWN, 1, 1);
                    event.getWorld().setBlockState(pos, RatsBlockRegistry.MARBLED_CHEESE_RAW.getDefaultState());
                    event.getWorld().setBlockState(pos.up(), RatsBlockRegistry.RATLANTIS_PORTAL.getDefaultState());
                    event.getWorld().setBlockState(pos.up(2), RatsBlockRegistry.RATLANTIS_PORTAL.getDefaultState());
                    event.getWorld().setBlockState(pos.up(3), RatsBlockRegistry.MARBLED_CHEESE_RAW.getDefaultState());
                }
            }
        }
        if (RatsMod.CONFIG_OPTIONS.cheesemaking && event.getWorld().getBlockState(event.getPos()).getBlock() == Blocks.CAULDRON && isMilk(event.getItemStack())) {
            if (event.getWorld().getBlockState(event.getPos()).getValue(BlockCauldron.LEVEL) == 0) {
                event.getWorld().setBlockState(event.getPos(), RatsBlockRegistry.MILK_CAULDRON.getDefaultState());
                if (!event.getWorld().isRemote) {
                    CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) event.getEntityPlayer(), event.getPos(), new ItemStack(RatsBlockRegistry.MILK_CAULDRON));
                }
                event.getEntityPlayer().playSound(SoundEvents.ITEM_BUCKET_EMPTY, 1, 1);
                if (!event.getEntityPlayer().isCreative()) {
                    if (event.getItemStack().getItem() == Items.MILK_BUCKET) {
                        event.getItemStack().shrink(1);
                        event.getEntityPlayer().addItemStackToInventory(new ItemStack(Items.BUCKET));
                    } else if (isMilk(event.getItemStack())) {
                        IFluidHandlerItem fluidHandler = FluidUtil.getFluidHandler(event.getItemStack());
                        if (fluidHandler != null) {
                            fluidHandler.drain(Fluid.BUCKET_VOLUME, true);
                        }
                    }
                }
                event.setUseItem(Event.Result.DENY);
                event.setCanceled(true);
            }
        }
    }

    private boolean isMilk(ItemStack stack) {
        if (stack.getItem() == Items.MILK_BUCKET) {
            return true;
        }
        FluidStack fluidStack = FluidUtil.getFluidContained(stack);
        return fluidStack != null && fluidStack.amount >= Fluid.BUCKET_VOLUME && (fluidStack.getFluid().getUnlocalizedName().contains("milk") || fluidStack.getFluid().getUnlocalizedName().contains("Milk"));
    }

    @SubscribeEvent
    public void onPlayerInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof EntityOcelot) {
            EntityOcelot ocelot = (EntityOcelot) event.getTarget();
            Item heldItem = event.getEntityPlayer().getHeldItem(event.getHand()).getItem();
            Random random = event.getWorld().rand;
            if (ocelot.getHealth() < ocelot.getMaxHealth()) {
                if (heldItem == RatsItemRegistry.RAW_RAT) {
                    ocelot.heal(4);
                    event.getWorld().playSound(null, ocelot.posX, ocelot.posY, ocelot.posZ, SoundEvents.ENTITY_LLAMA_EAT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    event.getWorld().playSound(null, ocelot.posX, ocelot.posY, ocelot.posZ, SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    for (int i = 0; i < 3; i++) {
                        event.getWorld().spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, ocelot.posX + random.nextDouble() - random.nextDouble(), ocelot.posY + 0.5 + random.nextDouble() - random.nextDouble(), ocelot.posZ + random.nextDouble() - random.nextDouble(), 0, 0, 0);
                    }
                }
                if (heldItem == RatsItemRegistry.COOKED_RAT) {
                    ocelot.heal(8);
                    event.getWorld().playSound(null, ocelot.posX, ocelot.posY, ocelot.posZ, SoundEvents.ENTITY_LLAMA_EAT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    event.getWorld().playSound(null, ocelot.posX, ocelot.posY, ocelot.posZ, SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                    for (int i = 0; i < 3; i++) {
                        event.getWorld().spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, ocelot.posX + random.nextDouble() - random.nextDouble(), ocelot.posY + 0.5 + random.nextDouble() - random.nextDouble(), ocelot.posZ + random.nextDouble() - random.nextDouble(), 0, 0, 0);
                    }
                }
            }
        }
        if(event.getTarget() instanceof EntityVillager){
            ItemStack heldItem = event.getEntityPlayer().getHeldItem(event.getHand());
            if(heldItem.getItem() == RatsItemRegistry.PLAGUE_DOCTORATE && !((EntityVillager) event.getTarget()).isChild()){
                EntityVillager villager = (EntityVillager)event.getTarget();
                EntityPlagueDoctor doctor = new EntityPlagueDoctor(event.getWorld());
                doctor.copyLocationAndAnglesFrom(villager);
                villager.setDead();
                doctor.onInitialSpawn(event.getWorld().getDifficultyForLocation(event.getPos()), null);
                if(!event.getWorld().isRemote){
                    event.getWorld().spawnEntity(doctor);
                }
                doctor.setNoAI(villager.isAIDisabled());
                if (villager.hasCustomName()) {
                    doctor.setCustomNameTag(villager.getCustomNameTag());
                    doctor.setAlwaysRenderNameTag(villager.getAlwaysRenderNameTag());
                }
                event.getEntityPlayer().swingArm(event.getHand());
                if(!event.getEntityPlayer().isCreative()){
                    heldItem.shrink(1);
                }
            }
        }
        if(event.getTarget() instanceof EntityPlagueDoctor){
            ItemStack heldItem = event.getEntityPlayer().getHeldItem(event.getHand());
            if(heldItem.getItem() == RatsItemRegistry.PLAGUE_TOME && !((EntityPlagueDoctor) event.getTarget()).isChild()){
                EntityPlagueDoctor villager = (EntityPlagueDoctor)event.getTarget();
                EntityBlackDeath doctor = new EntityBlackDeath(event.getWorld());
                doctor.copyLocationAndAnglesFrom(villager);
                villager.setDead();
                doctor.onInitialSpawn(event.getWorld().getDifficultyForLocation(event.getPos()), null);
                if(!event.getWorld().isRemote){
                    event.getWorld().spawnEntity(doctor);
                }
                doctor.setNoAI(villager.isAIDisabled());
                if (villager.hasCustomName()) {
                    doctor.setCustomNameTag(villager.getCustomNameTag());
                    doctor.setAlwaysRenderNameTag(villager.getAlwaysRenderNameTag());
                }
                event.getEntityPlayer().swingArm(event.getHand());
                if(!event.getEntityPlayer().isCreative()){
                    heldItem.shrink(1);
                }
            }
        }
        PotionEffect plague = event.getEntityPlayer().getActivePotionEffect(RatsMod.PLAGUE_POTION);
        if(plague != null && RatsMod.CONFIG_OPTIONS.plagueSpread && !(event.getTarget() instanceof EntityRat)){
            if(event.getTarget() instanceof EntityLivingBase && !((EntityLivingBase) event.getTarget()).isPotionActive(RatsMod.PLAGUE_POTION)){
                ((EntityLivingBase) event.getTarget()).addPotionEffect(plague);
                event.getTarget().playSound(SoundEvents.ENTITY_ZOMBIE_INFECT, 1.0F, 1.0F);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerPunch(AttackEntityEvent event) {
        ItemStack itemstack = event.getEntityPlayer().getHeldItem(EnumHand.MAIN_HAND);
        TinkersCompatBridge.onPlayerSwing(event.getEntityPlayer(), itemstack);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() != null && event.getEntity() instanceof EntityIronGolem && RatsMod.CONFIG_OPTIONS.golemsTargetRats) {
            EntityIronGolem golem = (EntityIronGolem) event.getEntity();
            golem.targetTasks.addTask(4, new EntityAINearestAttackableTarget(golem, EntityRat.class, 10, false, false, UNTAMED_RAT_SELECTOR));
        }
        if (event.getEntity() != null && RatUtils.isPredator(event.getEntity()) && event.getEntity() instanceof EntityAnimal) {
            EntityAnimal animal = (EntityAnimal) event.getEntity();
            animal.targetTasks.addTask(5, new EntityAINearestAttackableTarget(animal, EntityRat.class, true));
        }
        if (event.getEntity() != null && event.getEntity() instanceof EntityHusk) {
            if (((EntityHusk) event.getEntity()).getRNG().nextFloat() < RatsMod.CONFIG_OPTIONS.archeologistHatSpawnRate) {
                event.getEntity().setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(RatsItemRegistry.ARCHEOLOGIST_HAT));
                ((EntityLiving) event.getEntity()).setDropChance(EntityEquipmentSlot.HEAD, 0.5F);
            }
        }
        if (event.getEntity() != null && (event.getEntity() instanceof AbstractSkeleton || event.getEntity() instanceof EntityZombie) && BiomeDictionary.hasType(event.getWorld().getBiome(event.getEntity().getPosition()), BiomeDictionary.Type.JUNGLE)) {
            if (((EntityLiving) event.getEntity()).getRNG().nextFloat() < RatsMod.CONFIG_OPTIONS.archeologistHatSpawnRate) {
                event.getEntity().setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(RatsItemRegistry.ARCHEOLOGIST_HAT));
                ((EntityLiving) event.getEntity()).setDropChance(EntityEquipmentSlot.HEAD, 0.5F);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLeftClick(PlayerInteractEvent.LeftClickEmpty event) {
        ItemStack itemstack = event.getEntityPlayer().getHeldItem(EnumHand.MAIN_HAND);
        if(TinkersCompatBridge.onPlayerSwing(event.getEntityPlayer(), itemstack)){
            RatsMod.NETWORK_WRAPPER.sendToServer(new MessageSwingArm());
        }
        if (event.getEntityPlayer().isSneaking() && !event.getEntityPlayer().getPassengers().isEmpty()) {
            for (Entity passenger : event.getEntityPlayer().getPassengers()) {
                if (passenger instanceof EntityRat) {
                    passenger.dismountRidingEntity();
                    passenger.setPosition(event.getEntityPlayer().posX, event.getEntityPlayer().posY, event.getEntityPlayer().posZ);
                    RatsMod.NETWORK_WRAPPER.sendToServer(new MessageRatDismount(passenger.getEntityId()));
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack itemstack = event.getEntityPlayer().getHeldItem(EnumHand.MAIN_HAND);
        TinkersCompatBridge.onPlayerSwing(event.getEntityPlayer(), itemstack);
    }

    @SubscribeEvent
    public void onGatherCollisionBoxes(GetCollisionBoxesEvent event) {
        if (event.getEntity() instanceof EntityRat) {
            event.getCollisionBoxesList().removeIf(aabb -> ((EntityRat) event.getEntity()).canPhaseThroughBlock(event.getWorld(), new BlockPos(aabb.minX, aabb.minY, aabb.minZ)));
        }
    }

    @SubscribeEvent
    public void onDrops(LivingDropsEvent event) {
        if (event.getEntityLiving() instanceof EntityIllagerPiper && event.getSource().getTrueSource() instanceof EntityPlayer && event.getEntityLiving().world.rand.nextFloat() < RatsMod.CONFIG_OPTIONS.piperHatDropRate + (RatsMod.CONFIG_OPTIONS.piperHatDropRate / 2) * event.getLootingLevel()) {
            event.getDrops().add(new EntityItem(event.getEntity().world, event.getEntityLiving().posX, event.getEntityLiving().posY, event.getEntityLiving().posZ, new ItemStack(RatsItemRegistry.PIPER_HAT)));
        }
        if (event.getEntityLiving() instanceof EntityCreeper && ((EntityCreeper)event.getEntityLiving()).getPowered()) {
            event.getDrops().add(new EntityItem(event.getEntity().world, event.getEntityLiving().posX, event.getEntityLiving().posY, event.getEntityLiving().posZ, new ItemStack(RatsItemRegistry.CHARGED_CREEPER_CHUNK, event.getLootingLevel() + 1 + event.getEntityLiving().world.rand.nextInt(2))));
        }
        if (event.getSource().getTrueSource() instanceof EntityRat && ((EntityRat)event.getSource().getTrueSource()).hasUpgrade(RatsItemRegistry.RAT_UPGRADE_ARISTOCRAT)) {
            event.getDrops().add(new EntityItem(event.getEntity().world, event.getEntityLiving().posX, event.getEntityLiving().posY, event.getEntityLiving().posZ, new ItemStack(RatsItemRegistry.TINY_COIN)));
        }
    }

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        PotionEffect plague = event.getEntityLiving().getActivePotionEffect(RatsMod.PLAGUE_POTION);
        if (plague != null) {
            if (plague.getAmplifier() == 0) {
                event.setAmount(event.getAmount() * RatsMod.CONFIG_OPTIONS.plagueStage1HealingDebuff);
            }
            else if (plague.getAmplifier() > 0) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            AxisAlignedBB axisalignedbb = event.getEntityLiving().getEntityBoundingBox().grow(RatsMod.CONFIG_OPTIONS.ratVoodooDistance, RatsMod.CONFIG_OPTIONS.ratVoodooDistance, RatsMod.CONFIG_OPTIONS.ratVoodooDistance);
            List<EntityRat> list = event.getEntityLiving().world.getEntitiesWithinAABB(EntityRat.class, axisalignedbb);
            List<EntityRat> voodooRats = new ArrayList<>();
            int capturedRat = 0;
            if (!list.isEmpty()) {
                for (EntityRat rat : list) {
                    if (rat.isTamed() && (rat.isOwner(event.getEntityLiving())) && rat.hasUpgrade(RatsItemRegistry.RAT_UPGRADE_VOODOO)) {
                        voodooRats.add(rat);
                    }
                }
                if(!voodooRats.isEmpty()){
                    float damage = event.getAmount() / Math.max(1, voodooRats.size());
                    event.setCanceled(true);
                    for(EntityRat rat : voodooRats){
                        rat.attackEntityFrom(event.getSource(), damage);
                    }
                }

            }
        }

        //Plague spreading between infected entities
        if(event.getSource().getImmediateSource() instanceof EntityLivingBase && RatsMod.CONFIG_OPTIONS.plagueSpread){
            EntityLivingBase attacker = (EntityLivingBase)event.getSource().getImmediateSource();
            PotionEffect plague = attacker.getActivePotionEffect(RatsMod.PLAGUE_POTION);
            if(plague != null && !(event.getEntityLiving() instanceof EntityRat)){
                if(!event.getEntityLiving().isPotionActive(RatsMod.PLAGUE_POTION)){
                    event.getEntityLiving().addPotionEffect(plague);
                    event.getEntityLiving().playSound(SoundEvents.ENTITY_ZOMBIE_INFECT, 1.0F, 1.0F);
                }
            }
        }
    }


    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving().world.isRemote && (event.getEntityLiving().isPotionActive(RatsMod.PLAGUE_POTION) || event.getEntityLiving() instanceof EntityRat && ((EntityRat) event.getEntityLiving()).hasPlague())) {
            Random rand = event.getEntityLiving().getRNG();
            if(rand.nextInt(4) == 0) {
                int entitySize = 1;
                if (event.getEntityLiving().getEntityBoundingBox().getAverageEdgeLength() > 0) {
                    entitySize = Math.max(1, (int) event.getEntityLiving().getEntityBoundingBox().getAverageEdgeLength());
                }
                for (int i = 0; i < entitySize; i++) {
                    float motionX = rand.nextFloat() * 0.2F - 0.1F;
                    float motionZ = rand.nextFloat() * 0.2F - 0.1F;
                    RatsMod.PROXY.spawnParticle("flea", event.getEntityLiving().posX + (double) (rand.nextFloat() * event.getEntityLiving().width * 2F) - (double) event.getEntityLiving().width,
                            event.getEntityLiving().posY + (double) (rand.nextFloat() * event.getEntityLiving().height),
                            event.getEntityLiving().posZ + (double) (rand.nextFloat() * event.getEntityLiving().width * 2F) - (double) event.getEntityLiving().width,
                            motionX, 0.0F, motionZ);
                }
            }
        }

        //Plague Section --------
        EntityLivingBase entity = event.getEntityLiving();
        if (!entity.world.isRemote) {
            PotionEffect plague = entity.getActivePotionEffect(RatsMod.PLAGUE_POTION);
            if (plague != null) {
                if (entity.getEntityData().getInteger("plague_level") == 0) {
                    entity.getEntityData().setInteger("plague_level", plague.getAmplifier() + 1);
                    entity.getEntityData().setLong("plague_infected_time", entity.world.getTotalWorldTime());
                }

                //If the entity was infected for 10 mins or more increase plague level
                if (entity.world.getTotalWorldTime() - entity.getEntityData().getLong("plague_infected_time") >= RatsMod.CONFIG_OPTIONS.plagueStageDuration * 20L && entity.getEntityData().getInteger("plague_level") < 4) {
                    int amp = entity.getEntityData().getInteger("plague_level");
                    entity.addPotionEffect(new PotionEffect(RatsMod.PLAGUE_POTION, RatsMod.CONFIG_OPTIONS.plagueEffectDuration * 20, amp));
                    entity.getEntityData().setLong("plague_infected_time", entity.world.getTotalWorldTime());
                    entity.getEntityData().setInteger("plague_level", amp + 1);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            if (event.getSource() == RatsMod.plagueDamage) {
                event.getEntityLiving().getEntityData().setBoolean("was_plagued", true);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {

            AttributeModifier healthMod = event.getOriginal().getAttributeMap().getAttributeInstance(SharedMonsterAttributes.MAX_HEALTH).getModifier(PLAGUE_MAX_HEALTH_MODIFIER_UUID);
            Multimap<String, AttributeModifier> modMap = ArrayListMultimap.create();

            PotionEffect plague = event.getOriginal().getActivePotionEffect(RatsMod.PLAGUE_POTION);
            if (plague != null) {
                //The player wasn't killed by the plague (keep it after death)
                if (plague.getAmplifier() > 2 && event.getOriginal().getEntityData().getBoolean("was_plagued")) {
                    //The player was killed by the plague on level IV (decrease max plague level)
                    if (healthMod != null) {
                        healthMod = new AttributeModifier(PLAGUE_MAX_HEALTH_MODIFIER_UUID, "Rats Plague Max health debuff", -RatsMod.CONFIG_OPTIONS.plagueMaxHealthDebuff + healthMod.getAmount(), 0);
                    }
                    else {
                        healthMod = RatsMod.PLAGUE_MAX_HEALTH_MODIFIER;
                    }

                    modMap.put(SharedMonsterAttributes.MAX_HEALTH.getName(), healthMod);
                    //System.out.println("Ho diminuito la vita");
                }
                else if (RatsMod.CONFIG_OPTIONS.plagueRespawnStage == -1) {
                    //Config option is set to dynamically assign new plague stage based on the old one
                    event.getEntityPlayer().addPotionEffect(new PotionEffect(RatsMod.PLAGUE_POTION, plague.getDuration(), plague.getAmplifier()));
                }
                else if (RatsMod.CONFIG_OPTIONS.plagueRespawnStage > 0) {
                    //Config option is set to assign a specific stage of plague after dying
                    event.getEntityPlayer().addPotionEffect(new PotionEffect(RatsMod.PLAGUE_POTION, plague.getDuration(), RatsMod.CONFIG_OPTIONS.plagueRespawnStage - 1));
                }
                //if plagueRespawnStage is exactly == 0 --> plague effect is not restored after death
            }

            if (healthMod != null)
                modMap.put(SharedMonsterAttributes.MAX_HEALTH.getName(), healthMod);
            event.getEntityPlayer().getAttributeMap().applyAttributeModifiers(modMap);
        }
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getItem().getItem().getRegistryName().toString().equals(RatsMod.CONFIG_OPTIONS.plagueRestoreHealthItem)) {
            Multimap<String, AttributeModifier> modMap = ArrayListMultimap.create();
            modMap.put(SharedMonsterAttributes.MAX_HEALTH.getName(), RatsMod.PLAGUE_MAX_HEALTH_MODIFIER);
            event.getEntityLiving().getAttributeMap().removeAttributeModifiers(modMap);
        }
    }

    @SubscribeEvent
    public void onChestGenerated(LootTableLoadEvent event) {
        if(RatsMod.CONFIG_OPTIONS.addLoot) {
            if (event.getName().equals(LootTableList.CHESTS_SIMPLE_DUNGEON) || event.getName().equals(LootTableList.CHESTS_ABANDONED_MINESHAFT)
                    || event.getName().equals(LootTableList.CHESTS_DESERT_PYRAMID) || event.getName().equals(LootTableList.CHESTS_JUNGLE_TEMPLE)
                    || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CORRIDOR) || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CROSSING)
                    || event.getName().equals(LootTableList.CHESTS_IGLOO_CHEST) || event.getName().equals(LootTableList.CHESTS_WOODLAND_MANSION)
                    || event.getName().equals(LootTableList.CHESTS_VILLAGE_BLACKSMITH)) {
                LootCondition chance = new RandomChance(0.4f);
                LootEntryItem item = new LootEntryItem(RatsItemRegistry.CONTAMINATED_FOOD, 20, 1, new LootFunction[0], new LootCondition[0], "rats:contaminated_food");
                LootPool pool = new LootPool(new LootEntry[]{item}, new LootCondition[]{chance}, new RandomValueRange(1, 5), new RandomValueRange(0, 3), "rats:contaminated_food");
                event.getTable().addPool(pool);
            }
            if (event.getName().equals(LootTableList.CHESTS_SIMPLE_DUNGEON) || event.getName().equals(LootTableList.CHESTS_ABANDONED_MINESHAFT)
                    || event.getName().equals(LootTableList.CHESTS_DESERT_PYRAMID) || event.getName().equals(LootTableList.CHESTS_JUNGLE_TEMPLE)
                    || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CORRIDOR) || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CROSSING)) {
                LootCondition chance = new RandomChance(0.2f);
                LootEntryItem item = new LootEntryItem(RatsItemRegistry.TOKEN_FRAGMENT, 8, 10, new LootFunction[0], new LootCondition[0], "rats:token_fragment");
                LootPool pool = new LootPool(new LootEntry[]{item}, new LootCondition[]{chance}, new RandomValueRange(1, 1), new RandomValueRange(0, 1), "token_fragment");
                event.getTable().addPool(pool);
            }
            if (event.getName().equals(LootTableList.CHESTS_SIMPLE_DUNGEON) || event.getName().equals(LootTableList.CHESTS_ABANDONED_MINESHAFT)
                    || event.getName().equals(LootTableList.CHESTS_DESERT_PYRAMID) || event.getName().equals(LootTableList.CHESTS_JUNGLE_TEMPLE)
                    || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CORRIDOR) || event.getName().equals(LootTableList.CHESTS_STRONGHOLD_CROSSING)) {
                LootCondition chance = new RandomChance(0.05f);
                LootEntryItem item = new LootEntryItem(RatsItemRegistry.RAT_UPGRADE_BASIC, 3, 8, new LootFunction[0], new LootCondition[0], "rats:rat_upgrade_basic");
                LootPool pool = new LootPool(new LootEntry[]{item}, new LootCondition[]{chance}, new RandomValueRange(1, 1), new RandomValueRange(0, 0), "rat_upgrade_basic");
                event.getTable().addPool(pool);
            }
        }
    }

}