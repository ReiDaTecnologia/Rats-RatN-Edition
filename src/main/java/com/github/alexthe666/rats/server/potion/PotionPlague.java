package com.github.alexthe666.rats.server.potion;

import com.github.alexthe666.rats.RatsMod;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class PotionPlague extends Potion {

    public PotionPlague() {
        super(false, 0X445637);
        this.setRegistryName(RatsMod.MODID, "plague");
    }

    @Override
    public void performEffect(@Nonnull EntityLivingBase entityLivingBaseIn, int amplifier) {
        if (entityLivingBaseIn instanceof EntityPlayer) {
            if (amplifier == 2) {
                if (entityLivingBaseIn.ticksExisted % (RatsMod.CONFIG_OPTIONS.plagueStage3DamageFrequency * 20) == 0) {
                    entityLivingBaseIn.attackEntityFrom(RatsMod.plagueDamage, RatsMod.CONFIG_OPTIONS.plagueStage3Damage);
                }
            }
            else if (amplifier > 2) {
                entityLivingBaseIn.attackEntityFrom(RatsMod.plagueDamage, RatsMod.CONFIG_OPTIONS.plagueStage4Damage);
            }
        }
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return duration > 0;
    }

    @Override
    @Nonnull
    public String getName() {
        return "rats.plague";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, @Nonnull PotionEffect effect, net.minecraft.client.Minecraft mc) {
        mc.getTextureManager().bindTexture(PotionConfitByaldi.TEXTURE);
        this.drawTexturedModalRect(x + 6, y + 7, 18, 0, 18, 18);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(int x, int y, @Nonnull PotionEffect effect, net.minecraft.client.Minecraft mc, float alpha) {
        mc.getTextureManager().bindTexture(PotionConfitByaldi.TEXTURE);
        this.drawTexturedModalRect(x + 3, y + 3, 18, 0, 18, 18);

    }

    @SideOnly(Side.CLIENT)
    private void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, y + height, 200D).tex(((float) (textureX) * 0.00390625F), ((float) (textureY + height) * 0.00390625F)).endVertex();
        bufferbuilder.pos(x + width, (y + height), 200).tex(((float) (textureX + width) * 0.00390625F), ((float) (textureY + height) * 0.00390625F)).endVertex();
        bufferbuilder.pos(x + width, y, 200).tex(((float) (textureX + width) * 0.00390625F), ((float) (textureY) * 0.00390625F)).endVertex();
        bufferbuilder.pos(x, y, 200).tex(((float) textureX * 0.00390625F), ((float) textureY * 0.00390625F)).endVertex();
        tessellator.draw();
    }

    @Override
    @Nonnull
    public java.util.List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
