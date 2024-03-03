package de.tudbut.mod.client.ttcp.mods.rendering;

import de.tudbut.mod.client.ttcp.gui.GuiTTCIngame;
import de.tudbut.mod.client.ttcp.mods.combat.AutoTotem;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Render;
import de.tudbut.obj.Save;

@Render
public class HUD extends Module {
    
    static HUD instance;
    
    @Save
    public boolean showPopPredict = false;
    
    public HUD() {
        instance = this;
    }
    
    public static HUD getInstance() {
        return instance;
    }
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createBoolean("Show PopPredict", this, "showPopPredict"));
    }
    
    public void renderHUD() {
        if(enabled) {
            GuiTTCIngame.draw();
            renderTotems();
        }
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
    
    public void renderTotems() {
        if(enabled) {
            AutoTotem.instance.renderTotems();
        }
    }
}
