package com.github.alexthe666.rats.client.gui;

import com.github.alexthe666.rats.RatsMod;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RatsGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraftInstance) {

    }

    @Override
    public boolean hasConfigGui() {
        return true;
    }

    @Override
    public GuiScreen createConfigGui(GuiScreen parentScreen) {
        return new GuiRatConfig(parentScreen);
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    private class GuiRatConfig extends GuiConfig {
        public GuiRatConfig(GuiScreen parent) {
            super(parent, Lists.newArrayList(new ConfigElement(RatsMod.config.getCategory("all")), new ConfigElement(RatsMod.config.getCategory("rebirth_of_the_plague"))), RatsMod.MODID, false, false, "Rats General Config");
            titleLine2 = RatsMod.config.getConfigFile().getAbsolutePath();
        }

        @Override
        public void onGuiClosed() {
            super.onGuiClosed();
        }
    }
}
