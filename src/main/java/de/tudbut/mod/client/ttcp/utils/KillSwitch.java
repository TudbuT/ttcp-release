package de.tudbut.mod.client.ttcp.utils;

import de.tudbut.io.StreamReader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.model.ISmartVariant;
import org.lwjgl.input.Mouse;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.GuiTTC;
import de.tudbut.tools.Lock;
import de.tudbut.tools.Tools2;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

public class KillSwitch {
    
    public static String type = "been deactivated by a developer";
    public static boolean running = false; // TODO change back to false
    public static Lock lock = new Lock(true);
    
    public static void deactivate() {
        if(running)
            return;
        running = true;
        try {
            for (int i = 0; i < TTCp.modules.length; i++) {
                try {
                    Module module = TTCp.modules[i];
                    module.enabled = false;
                    module.onDisable();
                } catch (Exception ignore) {
                }
            }
        } catch (Exception ignore) {
        }
        try {
            Minecraft.getMinecraft().displayGuiScreen(new GuiKilled());
        } catch (Exception ignore) {
        }
        try {
            new File(TTCp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).delete();
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        lock.lock(15000);
        new Thread(() -> {
            try {
                Thread.sleep(15000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Minecraft.getMinecraft().shutdown();
        }).start();
    }
    
    public static class GuiKilled extends GuiScreen {
        
        {
            this.mc = Minecraft.getMinecraft();
        }
        
        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }
    
        @Override
        public void initGui() {
            super.initGui();
        }
    
        @Override
        public void onGuiClosed() {
            new Thread(() -> {
                try {
                    Thread.sleep(5);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Minecraft.getMinecraft().displayGuiScreen(this);
            }).start();
            super.onGuiClosed();
        }
        ScaledResolution sr = new ScaledResolution(mc);
        int y = sr.getScaledHeight() / 3;
        Lock timer = new Lock(); { timer.lock(15000); }
        
        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            sr = new ScaledResolution(mc);
            y = sr.getScaledHeight() / 3;
            drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 0xff000000);
            if(timer.isLocked())
                Mouse.setGrabbed(true);
            drawString("Your TTC has " + type + ".");
            drawString("Please contact us on the discord (https://discord.gg/UgbPQvyfmc)");
            drawString("or at TudbuT#2624!!!");
            drawString("Your mouse will be grabbed for about 15 more");
            drawString("seconds, afterwards, your minecraft will exit...");
            if(!timer.isLocked()) {
                Mouse.setGrabbed(false);
                Minecraft.getMinecraft().shutdown();
            }
        }
    
        private void drawString(String s) {
            String[] lines = s.split("\n");
            for (int i = 0 ; i < lines.length ; i++) {
                s = lines[i];
                mc.fontRenderer.drawString(
                        "§l" + s,
                        sr.getScaledWidth() / 2f - mc.fontRenderer.getStringWidth("§l" + s) / 2f,
                        y,
                        0xffff0000,
                        true
                );
                y += 13;
            }
        }
    }
}
