package de.tudbut.mod.client.ttcp.utils;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.Vec3d;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.mods.movement.ElytraBot;
import de.tudbut.mod.client.ttcp.mods.movement.PacketFly;
import de.tudbut.obj.Atomic;

import java.util.Date;

public class FlightBot {
    
    private static Atomic<Vec3d> destination;
    private static EntityPlayerSP player = TTCp.player;
    private static volatile boolean lock = false;
    private static boolean flying = false;
    private static boolean active = false;
    private static long tookOff = 0;
    private static double speed = 1;
    public static boolean allowPacketFly = false;
    
    public static boolean isActive() {
        return active;
    }
    
    public static boolean isFlying() {
        player = TTCp.player;
        return destination != null && destination.get() != null && flying && player.getPositionVector().distanceTo(destination.get()) > 1;
    }
    
    private FlightBot() { }
    
    public static void activate(Atomic<Vec3d> destination, double speed) {
        while (lock);
        flying = true;
        active = true;
        FlightBot.speed = speed;
        FlightBot.destination = destination;
    }
    
    public static void activate(Atomic<Vec3d> destination) {
        activate(destination, 1);
    }
    
    public static void deactivate() {
        active = false;
        speed = 1;
    }
    
    public static void updateDestination(Atomic<Vec3d> destination) {
        while (lock);
        FlightBot.destination = destination;
    }
    
    static boolean forceSpeed = false;
    public static void setForce(boolean force) {
        forceSpeed = force;
    }
    public static void setSpeed(double speed) {
        if(forceSpeed)
            return;
        FlightBot.speed = speed;
    }
    
    private static void takeOff() {
        player = TTCp.player;
        
        if (player.onGround) {
            if (!player.isElytraFlying()) {
                tookOff = 0;
                player.jump();
            }
        }
    }
    
    public static synchronized boolean tickBot() {
        if(!active)
            return false;
        
        player = TTCp.player;
    
        PacketFly packetFly = TTCp.getModule(PacketFly.class);
        if (!player.isElytraFlying() && !packetFly.enabled) {
            if (new Date().getTime() - tookOff > 100) {
                takeOff();
            }
            return false;
        }
        
        if(new Date().getTime() - tookOff < 300 && tookOff != 0) {
            return true;
        }
        
        if(destination.get() == null) {
            return false;
        }
        
        lock = true;
        double x, y, z;
        Vec3d dest = destination.get();
        double dx = dest.x - player.posX, dy = dest.y - player.posY, dz = dest.z - player.posZ;
        
        
        double d = Math.sqrt(dx*dx + dy*dy + dz*dz);
        
        if(d < 1) {
            d = 1;
            flying = false;
        }
        else
            flying = true;
        
        d = d / speed;
    
        x = dx / d;
        y = dy / d;
        z = dz / d;
        
        player.motionX = x;
        if(!ElytraBot.getInstance().strict || y < 0)
            player.motionY = y;
        player.motionZ = z;
        lock = false;
        
        if(packetFly.enabled) {
            allowPacketFly = true;
            packetFly.onTick();
            allowPacketFly = false;
        }
        
        return true;
    }
}
