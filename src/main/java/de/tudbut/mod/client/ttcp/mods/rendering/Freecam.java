package de.tudbut.mod.client.ttcp.mods.rendering;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.events.ModuleEventRegistry;
import de.tudbut.mod.client.ttcp.utils.FreecamPlayer;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Render;

import java.util.Objects;

import static de.tudbut.mod.client.ttcp.utils.Tesselator.*;

@Render
public class Freecam extends Module {
    
    public static Freecam getInstance() {
        return TTCp.getModule(Freecam.class);
    }
    
    GameType type;
    
    @Override
    public boolean displayOnClickGUI() {
        return true;
    }
    
    @Override
    public boolean doStoreEnabled() {
        return false;
    }
    
    public void onEnable() {
        if(TTCp.isIngame() && !LSD.getInstance().enabled && TTCp.mc.getRenderViewEntity() == TTCp.player) {
            EntityPlayer player = new FreecamPlayer(TTCp.player, TTCp.world);
            TTCp.world.spawnEntity(player);
            type = TTCp.mc.playerController.getCurrentGameType();
            //TTCp.mc.playerController.setGameType(GameType.SPECTATOR);
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
            //TTCp.mc.playerController.setGameType(type);
            TTCp.mc.setRenderViewEntity(TTCp.mc.player);
        }
        TTCp.mc.gameSettings.thirdPersonView = 0;
        TTCp.mc.renderChunksMany = true;
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
    
    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
    
        if(TTCp.isIngame() && enabled) {
            Entity main = TTCp.player;
            Entity e = TTCp.mc.getRenderViewEntity();
            assert e != null;
            Vec3d p = e.getPositionEyes(event.getPartialTicks()).add(0, -e.getEyeHeight(), 0);
            Vec3d pos = main.getPositionVector();
            float entityHalfed = main.width / 2 + 0.01f;
            float entityHeight = main.height + 0.01f;
            
            ready();
            translate(-p.x, -p.y, -p.z);
            color(0x80ff0000);
            depth(false);
            begin(GL11.GL_QUADS);
            
            // bottom
            put(pos.x - entityHalfed, pos.y - 0.01, pos.z + entityHalfed);
            put(pos.x + entityHalfed, pos.y - 0.01, pos.z + entityHalfed);
            put(pos.x + entityHalfed, pos.y - 0.01, pos.z - entityHalfed);
            put(pos.x - entityHalfed, pos.y - 0.01, pos.z - entityHalfed);
    
            next();
            
            // top
            put(pos.x - entityHalfed, pos.y + entityHeight, pos.z + entityHalfed);
            put(pos.x + entityHalfed, pos.y + entityHeight, pos.z + entityHalfed);
            put(pos.x + entityHalfed, pos.y + entityHeight, pos.z - entityHalfed);
            put(pos.x - entityHalfed, pos.y + entityHeight, pos.z - entityHalfed);
    
            next();
    
            // z -
            put(pos.x - entityHalfed, pos.y + entityHeight, pos.z - entityHalfed);
            put(pos.x + entityHalfed, pos.y + entityHeight, pos.z - entityHalfed);
            put(pos.x + entityHalfed, pos.y - 0.01, pos.z - entityHalfed);
            put(pos.x - entityHalfed, pos.y - 0.01, pos.z - entityHalfed);
    
            next();
    
            // z +
            put(pos.x - entityHalfed, pos.y + entityHeight, pos.z + entityHalfed);
            put(pos.x + entityHalfed, pos.y + entityHeight, pos.z + entityHalfed);
            put(pos.x + entityHalfed, pos.y - 0.01, pos.z + entityHalfed);
            put(pos.x - entityHalfed, pos.y - 0.01, pos.z + entityHalfed);
    
            next();
    
            // x -
            put(pos.x - entityHalfed, pos.y + entityHeight, pos.z - entityHalfed);
            put(pos.x - entityHalfed, pos.y + entityHeight, pos.z + entityHalfed);
            put(pos.x - entityHalfed, pos.y - 0.01, pos.z + entityHalfed);
            put(pos.x - entityHalfed, pos.y - 0.01, pos.z - entityHalfed);
    
            next();
    
            // y +
            put(pos.x + entityHalfed, pos.y + entityHeight, pos.z - entityHalfed);
            put(pos.x + entityHalfed, pos.y + entityHeight, pos.z + entityHalfed);
            put(pos.x + entityHalfed, pos.y - 0.01, pos.z + entityHalfed);
            put(pos.x + entityHalfed, pos.y - 0.01, pos.z - entityHalfed);
            
            end();
        }
    }
    
    @Override
    public void init() {
        ModuleEventRegistry.disableOnNewPlayer.add(this);
    }
}
