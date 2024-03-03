package de.tudbut.mod.client.ttcp.mods.rendering;

import net.minecraft.entity.player.EntityPlayer;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.events.ModuleEventRegistry;
import de.tudbut.mod.client.ttcp.gui.GuiTTC;
import de.tudbut.mod.client.ttcp.utils.LSDRenderer;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Render;

import java.lang.reflect.Field;
import java.util.Objects;

@Render
public class LSD extends Module {
    public static LSD getInstance() {
        return TTCp.getModule(LSD.class);
    }
    
    int mode = 0x00;
    
    {
        try {
            subButtons.add(new GuiTTC.Button("Mode: " + getMode(mode), text -> {
                mode++;
                if(mode > 0x0a)
                    mode = 0x00;
                try {
                    text.set("Mode: " + getMode(mode));
                }
                catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }));
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
    
    private String getMode(int mode) throws IllegalAccessException {
        Class<LSDRenderer> clazz = LSDRenderer.class;
        Field[] fields = clazz.getDeclaredFields();
        
        for (int i = 0; i < fields.length; i++) {
            if(fields[i].getInt(null) == mode && !fields[i].getName().equals("mode")) {
                return fields[i].getName();
            }
        }
        
        return null;
    }
    
    @Override
    public boolean displayOnClickGUI() {
        return true;
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
    
    @Override
    public boolean doStoreEnabled() {
        return false;
    }
    
    @Override
    public void onTick() {
        LSDRenderer.mode = mode;
    }
    
    public void onEnable() {
        if(TTCp.isIngame() && !Freecam.getInstance().enabled) {
            EntityPlayer player = new LSDRenderer(TTCp.player, TTCp.world);
            TTCp.world.spawnEntity(player);
            TTCp.mc.renderChunksMany = true;
            //TTCp.mc.skipRenderWorld = true;
            TTCp.mc.setRenderViewEntity(player);
        }
        else
            enabled = false;
    }
    
    @Override
    public int danger() {
        return 1;
    }
    
    @Override
    public void onDisable() {
        if(TTCp.isIngame()) {
            TTCp.world.removeEntity(Objects.requireNonNull(TTCp.mc.getRenderViewEntity()));
            TTCp.mc.setRenderViewEntity(TTCp.mc.player);
            TTCp.mc.renderChunksMany = true;
        }
    }
    
    @Override
    public void init() {
        ModuleEventRegistry.disableOnNewPlayer.add(this);
    }
}
