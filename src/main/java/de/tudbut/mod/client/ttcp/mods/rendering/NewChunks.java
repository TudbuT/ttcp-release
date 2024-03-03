package de.tudbut.mod.client.ttcp.mods.rendering;

import de.tudbut.type.Vector3d;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Utils;
import de.tudbut.mod.client.ttcp.utils.category.Render;

import java.util.ArrayList;

import static de.tudbut.mod.client.ttcp.utils.Tesselator.*;

@Render
public class NewChunks extends Module {
    
    ArrayList<ChunkPos> chunks = new ArrayList<>();
    
    @SubscribeEvent
    public void onChunkData(ChunkEvent.Load event) {
        if(TTCp.isIngame() && Utils.isCallingFrom(Chunk.class)) {
            chunks.add(event.getChunk().getPos());
        }
    }
    
    @SubscribeEvent
    public void onRenderWorld(Event event) {
        
        if(event instanceof RenderWorldLastEvent)
            if(this.enabled && TTCp.isIngame()) {
                Entity e = TTCp.mc.getRenderViewEntity();
                assert e != null;
                pos = e.getPositionEyes(((RenderWorldLastEvent) event).getPartialTicks()).add(0, -e.getEyeHeight(), 0);
    
                for (int i = 0; i < chunks.size(); i++) {
                    drawAroundChunk(new Vector3d(chunks.get(i).x * 16 + 8, 64, chunks.get(i).z * 16 + 8), 0x80ff0000);
                }
            }
    }
    
    Vec3d pos = new Vec3d(0, 0, 0);
    
    public void drawAroundChunk(Vector3d pos, int color) {
        try {
    
            ready();
            translate(-this.pos.x, -this.pos.y, -this.pos.z);
            color(color);
            depth(false);
            begin(GL11.GL_LINES);
    
    
            // bottom
            put(pos.getX() - 8, pos.getY() - 0.01, pos.getZ() + 8);
            put(pos.getX() + 8, pos.getY() - 0.01, pos.getZ() + 8);
            
            put(pos.getX() + 8, pos.getY() - 0.01, pos.getZ() + 8);
            put(pos.getX() + 8, pos.getY() - 0.01, pos.getZ() - 8);
            
            put(pos.getX() + 8, pos.getY() - 0.01, pos.getZ() - 8);
            put(pos.getX() - 8, pos.getY() - 0.01, pos.getZ() - 8);
            
            put(pos.getX() - 8, pos.getY() - 0.01, pos.getZ() - 8);
            put(pos.getX() - 8, pos.getY() - 0.01, pos.getZ() + 8);
    
            end();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onTick() {
    
    }
}
