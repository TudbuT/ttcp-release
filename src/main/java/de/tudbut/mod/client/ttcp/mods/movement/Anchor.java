package de.tudbut.mod.client.ttcp.mods.movement;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Movement;
import de.tudbut.obj.Save;

@Movement
public class Anchor extends Module {
    
    @Save
    boolean x,y,z;
    @Save
    boolean voidEnable = true;
    double px, py, pz;
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createBoolean("X", this, "x"));
        subComponents.add(Setting.createBoolean("Y", this, "y"));
        subComponents.add(Setting.createBoolean("Z", this, "z"));
        subComponents.add(Setting.createBoolean("Enable in void", this, "voidEnable"));
    }
    
    @Override
    public void onEveryTick() {
        if(player.posY < 0 && voidEnable && !enabled) {
            enabled = true;
            green = true;
            onEnable();
        }
    }
    
    @Override
    public void onEnable() {
        EntityPlayerSP player = TTCp.player;
        if(player != null) {
            px = player.posX;
            py = player.posY;
            pz = player.posZ;
        }
    }
    
    @Override
    public void onSubTick() {
        EntityPlayerSP player = TTCp.player;
        
        if(x) {
            player.motionX = 0;
            player.posX = px;
        }
        if(y) {
            player.motionY = 0;
            player.posY = py;
        }
        if(z) {
            player.motionZ = 0;
            player.posZ = pz;
        }
        
        
    }
    
    public boolean onPacket(Packet<?> packet) {
        return packet instanceof CPacketConfirmTeleport;
    }
}
