package de.tudbut.mod.client.ttcp.mods.rendering;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.GuiRewrite;
import de.tudbut.mod.client.ttcp.gui.GuiTTC;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Render;
import de.tudbut.obj.Save;
import de.tudbut.obj.TLMap;

import java.io.IOException;

@Render
public class ClickGUI extends Module {
    
    static ClickGUI instance;
    // TMP fix for mouse not showing
    @Save
    public boolean mouseFix = false;
    
    @Save
    public boolean flipButtons = false;
    
    @Save
    public int themeID = 0;
    
    public GuiTTC.ITheme customTheme = null;
    
    public GuiTTC.ITheme getTheme() {
        if(customTheme != null)
            return customTheme;
        return GuiTTC.Theme.values()[themeID];
    }
    
    private int confirmInstance = 0;
    
    public ClickGUI() {
        instance = this;
        clickGuiShow = true;
        if(TTCp.guiNotLoadedYet) {
            KillSwitch.type = "detected that it has been tampered with";
            ThreadManager.run(KillSwitch::deactivate);
            try {
                Thread.sleep(10000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            TTCp.verify();
        }
    }
    
    public static ClickGUI getInstance() {
        return instance;
    }
    
    @Save
    public ScrollDirection sd = ScrollDirection.Vertical;
    
    public enum ScrollDirection {
        Vertical,
        Horizontal
    }
    
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new Button("Flip buttons: " + flipButtons, it -> {
            flipButtons = !flipButtons;
            it.text = "Flip buttons: " + flipButtons;
        }));
        subComponents.add(new Button("Theme: " + getTheme(), it -> {
            if(customTheme == null) {
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                    themeID--;
                else
                    themeID++;
    
                if (themeID < 0)
                    themeID = GuiTTC.Theme.values().length - 1;
                if (themeID > GuiTTC.Theme.values().length - 1)
                    themeID = 0;
    
                it.text = "Theme: " + getTheme();
            }
        }));
        subComponents.add(Setting.createEnum(ScrollDirection.class, "Scroll", this, "sd"));
        subComponents.add(new Button("Reset layout", it -> {
            displayConfirmation = true;
            confirmInstance = 0;
        }));
        subComponents.add(new Button("Mouse fix: " + mouseFix, it -> {
            mouseFix = !mouseFix;
            it.text = "Mouse fix: " + mouseFix;
        }));
        subComponents.add(new Button("Reset client", it -> {
            displayConfirmation = true;
            confirmInstance = 1;
        }));
    }
    
    @Override
    public void onEnable() {
        // Show the GUI
        try {
            ChatUtils.print("Showing ClickGUI");
            TTCp.mc.displayGuiScreen(new GuiRewrite());
        } catch (Exception e) {
            e.printStackTrace();
            enabled = false;
        }
    }
    
    @Override
    public void onConfirm(boolean result) {
        if (result)
            switch (confirmInstance) {
                case 0:
                    // Reset ClickGUI by closing it, resetting its values, and opening it
                    enabled = false;
                    onDisable();
                    TTCp.categories = new TLMap<>();
                    enabled = true;
                    onEnable();
                    break;
                case 1:
                    displayConfirmation = true;
                    confirmInstance = 2;
                    break;
                case 2:
                    enabled = false;
                    onDisable();
    
                    // Saving file
                    try {
                        TTCp.file.setContent("");
                        TTCp.file = null;
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    Minecraft.getMinecraft().shutdown();
                    break;
            }
    }
    
    @Override
    public void onDisable() {
        // Kill the GUI
        if (TTCp.mc.currentScreen != null && TTCp.mc.currentScreen.getClass() == GuiRewrite.class)
            TTCp.mc.displayGuiScreen(null);
    }
    
    @Override
    public void onEveryTick() {
        if(key.key == null) {
            key.key = Keyboard.KEY_COMMA;
            updateBindsFull();
        }
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
}
