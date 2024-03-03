package de.tudbut.mod.client.ttcp.mods.movement;

import net.minecraft.network.play.client.CPacketPlayer;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Movement;
import de.tudbut.obj.Save;

@Movement
public class BHop extends Module {
    public enum Mode {
        PACKET,
        PACKETJUMP,
        MOTION,
        JUMP,
        LOWHOP,
        ;
    }
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createEnum(Mode.class, "Mode", this, "mode"));
        subComponents.add(Setting.createBoolean("OnGround", this, "packetOnGround"));
    }
    
    @Save
    Mode mode = Mode.JUMP;
    
    @Save
    boolean packetOnGround = false;
    
    @Override
    public void onTick() {
        if(player.onGround && (player.movementInput.moveForward != 0 || player.movementInput.moveStrafe != 0)) {
            if(mode == Mode.JUMP) {
                player.jump();
            }
            if(mode == Mode.MOTION) {
                player.motionY = 0.425F;
            }
            if(mode == Mode.LOWHOP) {
                player.motionY = 0.425F;
            }
            if(mode == Mode.PACKET) {
                player.connection.sendPacket(new CPacketPlayer.Position(player.posX, player.posY += 1.1, player.posZ, packetOnGround));
                player.onGround = false;
            }
            if(mode == Mode.PACKETJUMP) {
                player.connection.sendPacket(new CPacketPlayer.Position(player.posX, player.posY + 0.41999998688698D, player.posZ, packetOnGround));
                player.connection.sendPacket(new CPacketPlayer.Position(player.posX, player.posY + 0.7531999805211997D, player.posZ, packetOnGround));
                player.connection.sendPacket(new CPacketPlayer.Position(player.posX, player.posY + 1.00133597911214D, player.posZ, packetOnGround));
                player.connection.sendPacket(new CPacketPlayer.Position(player.posX, player.posY += 1.16610926093821D, player.posZ, packetOnGround));
                player.onGround = false;
            }
        }
        else if(mode == Mode.LOWHOP) {
            player.motionY = -0.1;
        }
    }
}
