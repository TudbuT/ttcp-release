package de.tudbut.mod.client.ttcp.mods.combat;

import de.tudbut.mod.client.ttcp.utils.category.Combat;
import de.tudbut.mod.client.ttcp.utils.Module; 
import de.tudbut.mod.client.ttcp.TTCp; 
import org.lwjgl.input.Mouse;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.EnumHand;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import de.tudbut.mod.client.ttcp.utils.Utils;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.obj.Save;
import net.minecraft.entity.Entity;

@Combat
public class PortalHand extends Module {

    Entity toAttack = null;
    Vec3d beginPos = null;
    boolean beginPosOk = false;
    int tpid = 0;
    @Save
    int range = 70;
    @Save
    float scale = 9;
    @Save
    boolean forceLagBack = true;
    
    SPacketPlayerPosLook lastPacket = null;
    long lastPacketTime = 0;

    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createInt(10, 300, "Range", this, "range"));
        subComponents.add(Setting.createFloat(0.5f, 50.5f, "VecScale", this, "scale"));
        subComponents.add(Setting.createBoolean("ForceLagBack", this, "forceLagBack"));
    }

    @Override
    public void onDisable() {
        tpid = 0;
    }

    @Override
    public void onTick() {
        if(lastPacketTime + 2000 > System.currentTimeMillis() && lastPacketTime != 0) {
            mc.player.connection.handlePlayerPosLook(lastPacket);
            lastPacket = null;
            lastPacketTime = 0;
            tpid = -1;
        }
        if(tpid <= 0) {
            if((-tpid) % 20 == 0) {
                mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY - 1000, mc.player.posZ, mc.player.onGround));
                System.out.println("TPID requested from server.");
            }
            tpid -= 1;
            return;
        }
        if(beginPos != null) {
            if(beginPosOk) {
                goTo(beginPos);
                beginPos = null;
                beginPosOk = false;
                // tpid = 0; // no longer required due to new algorithm in onPacket
                mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY - 1000, mc.player.posZ, mc.player.onGround));
                mc.player.connection.sendPacket(new CPacketConfirmTeleport(++tpid));
            }
            else {
                beginPosOk = true;
            }
        }
        if(toAttack != null) {
            Entity e = toAttack;
            toAttack = null;
            Vec3d v = mc.player.getPositionVector();
            goTo(e.getPositionVector());
            mc.playerController.attackEntity(mc.player, e);
            mc.player.swingArm(EnumHand.MAIN_HAND);
            goTo(v);
            beginPos = v;
        }
        if(Mouse.isButtonDown(0) && mc.currentScreen == null && toAttack == null && beginPos == null) {
            Entity e = Utils.getPointingEntity(range, 2);
            if(e == null)
                return;
            toAttack = e;
            mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY, mc.player.posZ, mc.player.onGround));
        }
    }

    @Override
    public void onChat(String s, String[] args) {
        float x = Float.parseFloat(args[0]);
        float y = Float.parseFloat(args[1]);
        float z = Float.parseFloat(args[2]);
        goTo(new Vec3d(x,y,z));
        mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY - 1000, mc.player.posZ, mc.player.onGround));
        mc.player.connection.sendPacket(new CPacketPlayer.Position(mc.player.posX, mc.player.posY, mc.player.posZ, mc.player.onGround));
        mc.player.connection.sendPacket(new CPacketConfirmTeleport(++tpid));
    }

    public void goTo(Vec3d destination) {
        Vec3d currentPosition = mc.player.getPositionVector();
        Vec3d vector = destination;
        vector = vector.subtract(currentPosition);
        vector = vector.normalize();
        vector = vector.scale(scale);
        // this is now the vector per packet
        while(currentPosition.distanceTo(destination) > scale) {
            if(forceLagBack) {
                mc.player.connection.sendPacket(new CPacketPlayer.Position(currentPosition.x, currentPosition.y - 1000, currentPosition.z, mc.player.onGround));
                mc.player.connection.sendPacket(new CPacketPlayer.Position(currentPosition.x, currentPosition.y, currentPosition.z, mc.player.onGround));
                mc.player.connection.sendPacket(new CPacketConfirmTeleport(++tpid));
            }
            currentPosition = currentPosition.add(vector); // set current position vector and then send to server
            mc.player.connection.sendPacket(new CPacketPlayer.Position(currentPosition.x, currentPosition.y, currentPosition.z, mc.player.onGround));
        }
        if(forceLagBack) {
            mc.player.connection.sendPacket(new CPacketPlayer.Position(currentPosition.x, currentPosition.y - 1000, currentPosition.z, mc.player.onGround));
            mc.player.connection.sendPacket(new CPacketPlayer.Position(currentPosition.x, currentPosition.y, currentPosition.z, mc.player.onGround));
            mc.player.connection.sendPacket(new CPacketConfirmTeleport(++tpid));
        }
        mc.player.connection.sendPacket(new CPacketPlayer.Position(destination.x, destination.y, destination.z, mc.player.onGround));
        mc.player.posX = destination.x;
        mc.player.posY = destination.y;
        mc.player.posZ = destination.z;
    }

    @Override
    public boolean onPacket(Packet<?> packet) {
        if(packet instanceof SPacketPlayerPosLook) {
            SPacketPlayerPosLook p = (SPacketPlayerPosLook) packet;
            System.out.println("Server PosLook: " + p.getX() + " " + p.getY() + " " + p.getZ() + " (" + p.getTeleportId() + ")");
            lastPacket = null;
            lastPacketTime = 0;
            if(tpid <= 0) {
                tpid = p.getTeleportId();
                System.out.println("TPID set!");
                return false;
            }
            if(p.getTeleportId() == 1) { // on join
                tpid = 0;
                return false;
            }
            if(p.getTeleportId() >= tpid) {
                tpid = p.getTeleportId();
                return false;
            }
            else { // packet should already be processed
                lastPacket = p;
                lastPacketTime = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }
}
