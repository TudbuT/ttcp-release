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
public class ViewAnchor extends Module {
    
    @Save
    boolean x,y;
    @Save
    boolean voidEnable = true;
    float px, py;
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createBoolean("X", this, "x"));
        subComponents.add(Setting.createBoolean("Y", this, "y"));
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
            px = player.rotationYaw;
            py = player.rotationPitch;
        }
    }
    
    @Override
    public void onSubTick() {
        EntityPlayerSP player = TTCp.player;
        
        if(x) {
            player.rotationYaw = px;
        }
        if(y) {
            player.rotationPitch = py;
        }
        
        
    }
}
