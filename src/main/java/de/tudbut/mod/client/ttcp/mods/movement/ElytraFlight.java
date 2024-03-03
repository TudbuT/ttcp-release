package de.tudbut.mod.client.ttcp.mods.movement;

import de.tudbut.timer.AsyncTask;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Movement;
import de.tudbut.obj.Save;
import de.tudbut.tools.Lock;

@Movement
public class ElytraFlight extends Module {
    @Save
    float speed = 1;
    @Save
    boolean autoTakeoff = false;
    @Save
    boolean tpsSync = false;
    @Save
    float upDiv = 1;
    @Save
    float boostMod = 2;
    @Save
    float takeoffMotion = 2.5f;
    @Save
    int takeoffTicks = 1;
    int lastTakeoffTry = 0;
    @Save
    int restartDelay = 500;
    @Save
    float upspeed = 3;
    @Save
    boolean strictMode = false;
    @Save
    Mode mode = Mode.CONTROL;

    public enum Mode {
        CONTROL, BOOST;
    }
    
    Lock takeoff = new Lock();
    boolean restarting = false;
    boolean init;
    
    public void updateBinds() {
        customKeyBinds.setIfNull("faster", new KeyBind(null, this + "::faster", false));
        customKeyBinds.setIfNull("slower", new KeyBind(null, this + "::slower", false));
        customKeyBinds.setIfNull("boost", new KeyBind(null, this + "::boost", true));
        customKeyBinds.setIfNull("restart", new KeyBind(null, this + "::restart", true));
        customKeyBinds.setIfNull("up", new KeyBind(null, this + "::up", true));
        customKeyBinds.setIfNull("kill", new KeyBind(null, this + "::kill", true));
        subComponents.clear();
        subComponents.add(Setting.createEnum(Mode.class, "Mode", this, "mode"));
        subComponents.add(Setting.createFloat(1, 10, "Speed", this, "speed"));
        subComponents.add(Setting.createBoolean("AutoTakeoff", this, "autoTakeoff"));
        subComponents.add(Setting.createInt(0, 40, "TakeoffTicks", this, "takeoffTicks"));
        subComponents.add(Setting.createFloat(0, 5, "TakeoffMotion", this, "takeoffMotion"));
        subComponents.add(Setting.createFloat(1, 2, "Boost speed", this, "boostMod"));
        subComponents.add(Setting.createFloat(1, 1000, "UpDiv", this, "upDiv"));
        subComponents.add(Setting.createInt(0, 1000, "RestartDelay", this, "restartDelay"));
        subComponents.add(Setting.createBoolean("TPS Sync", this, "tpsSync"));
        subComponents.add(Setting.createBoolean("Use up instead of motion", this, "strictMode"));
        
        subComponents.add(Setting.createKey("Faster", customKeyBinds.get("faster")));
        subComponents.add(Setting.createKey("Slower", customKeyBinds.get("slower")));
        subComponents.add(Setting.createKey("Boost", customKeyBinds.get("boost")));
        subComponents.add(Setting.createKey("Restart", customKeyBinds.get("restart")));
        subComponents.add(Setting.createKey("Up", customKeyBinds.get("up")));
        subComponents.add(Setting.createKey("Kill", customKeyBinds.get("kill")));
        subComponents.add(Setting.createFloat(1, 10, "UpSpeed", this, "upspeed"));
    }
    
    public void restart() {
        InventoryUtils.clickSlot(6, ClickType.PICKUP, 0);
        new AsyncTask<>(() -> {
            Thread.sleep(restartDelay);
            InventoryUtils.clickSlot(6, ClickType.PICKUP, 0);
            takeoff.lock();
            restarting = true;
            return null;
        });
    }
    
    public void boost() {
        player.motionX *= boostMod;
        player.motionY *= boostMod;
        player.motionZ *= boostMod;
    }
    
    public void faster() {
        speed += 0.1;
        if(speed > 5)
            speed = 5;
        updateBinds();
    }
    
    public void slower() {
        speed -= 0.1;
        if(speed < 0.1f)
            speed = 0.1f;
        updateBinds();
    }
    
    int upStage = 0;
    
    public void up() {
        if(upStage == 0)
            upStage = 20;
    }
    
    public void kill() {
        // Trick server into setting onGround=true temporarily, this causes the
        // if-statement in START_FALL_FLYING to fail and instead of starting the
        // elytra, it stops it.
        player.connection.sendPacket(new CPacketPlayer.Rotation(0,0, true));
        // Now send the packet. The server now believes we aren't using our elytra
        // anymore.
        player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING));
    }
    
    boolean b;
    @Override
    public void onEveryTick() {
        if(TTCp.mc.world == null) {
            init = false;
            return;
        }
        EntityPlayerSP player = TTCp.player;
        
        if(upStage > 0) {
            switch (upStage--) {
                case 20:
                    b = TTCp.getModule(ViewAnchor.class).enabled;
                    TTCp.getModule(ViewAnchor.class).enabled = false;
                    enabled = false;
                    green = false;
                    onDisable();
                    
                    player.rotationPitch = 45;
                    Vec2f movementVec = new Vec2f(0,1);
    
                    float f1 = MathHelper.sin(player.rotationYaw * 0.017453292F);
                    float f2 = MathHelper.cos(player.rotationYaw * 0.017453292F);
                    double x = movementVec.x * f2 - movementVec.y * f1;
                    double z = movementVec.y * f2 + movementVec.x * f1;
                    float d = (float) Math.sqrt(x * x + z * z);
    
                    if (d < 1) {
                        d = 1;
                    }
    
                    if(mode == Mode.BOOST && player.rotationPitch > 0) {
                        player.motionX += x * upspeed / 40;
                        player.motionZ += z * upspeed / 40;
                    }
                    if(mode == Mode.CONTROL) {
                        player.motionX = x / d * upspeed;
                        player.motionY = 0;
                        player.motionZ = z / d * upspeed;
                    }
                    break;
                case 19:
                    player.rotationPitch = -45;
                    break;
                case 18:
                    player.rotationPitch = -70;
                    break;
                case 17:
                    player.rotationPitch = -90;
                    break;
                case 1:
                    enabled = true;
                    green = true;
                    onEnable();
                    player.rotationPitch = 20;
                    if(b) {
                        TTCp.getModule(ViewAnchor.class).enabled = true;
                    }
                    break;
            }
        }
    
        if(restarting) {
            if (player.isElytraFlying()) {
                player.motionX = 0;
                player.motionY = 0;
                player.motionZ = 0;
                init = true;
                takeoff.unlock();
                restarting = false;
        
                negateElytraFallMomentum(player);
            }
            else if (autoTakeoff && player.motionY < 0) {
                takeoff.lock();
                if (lastTakeoffTry >= takeoffTicks) {
                    player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING));
                    lastTakeoffTry = 0;
                }
            }
        }
        lastTakeoffTry++;
    }

    int takeoffStep = 0;
    
    @Override
    public void onTick() {
        if (TTCp.mc.world == null) {
            init = false;
            return;
        }
        EntityPlayerSP player = TTCp.player;
    
        if(upStage > 0) {
            return;
        }
        
        FlightBot.setSpeed(speed());
        boolean blockMovement = mode == Mode.CONTROL && FlightBot.tickBot();
    
        if (init) {
            if (TTCp.player == TTCp.mc.getRenderViewEntity()) {
                if (!blockMovement) {
                    if(player.movementInput.jump && strictMode) {
                        up();
                        enabled = false;
                        green = false;
                        onDisable();
                        return;
                    }
                    
                    Vec2f movementVec = player.movementInput.getMoveVector();
    
                    float f1 = MathHelper.sin(player.rotationYaw * 0.017453292F);
                    float f2 = MathHelper.cos(player.rotationYaw * 0.017453292F);
                    double x = movementVec.x * f2 - movementVec.y * f1;
                    double y = (player.movementInput.jump ? 1 : 0) + (player.movementInput.sneak ? -1 : 0);
                    double z = movementVec.y * f2 + movementVec.x * f1;
                    float d = (float) Math.sqrt(x * x + y * y + z * z);
    
                    if (d < 1) {
                        d = 1;
                    }

                    if(mode == Mode.BOOST && player.rotationPitch > 0) {
                        player.motionX += x * speed() / 40;
                        player.motionZ += z * speed() / 40;
                    }
                    if(mode == Mode.CONTROL) {
                        player.motionX = x / d * speed();
                        if(!strictMode || y < 0)
                            player.motionY = y / d * speed() / upDiv;
                        else
                            player.motionY = 0;
                        player.motionZ = z / d * speed();
                    }
                }
            }
            else if (!FlightBot.isFlying()) {
                player.motionX = 0;
                player.motionY = 0;
                player.motionZ = 0;
            }
            if(mode == Mode.CONTROL)
                negateElytraFallMomentum(player);
        
        }
        else if (player.isElytraFlying()) {
            takeoffStep = 0;
            player.motionX = 0;
            player.motionZ = 0;
            init = true;
            takeoff.unlock();
        
            if(mode == Mode.CONTROL)
                negateElytraFallMomentum(player);
        }
        else {
            if (autoTakeoff && player.motionY < -0.05 && !player.onGround) {
                takeoff.lock(5000);
                if (lastTakeoffTry >= takeoffTicks) {
                    // if(takeoffStep == 0) {
                        // Trick server into setting onGround=false temporarily, helps with reliability
                        player.connection.sendPacket(new CPacketPlayer.Position(player.posX, player.posY, player.posZ, false));
                        // Start elytraflying
                        player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING));
                        player.motionY = takeoffMotion;
                    // }
                    lastTakeoffTry = 0;
                }
                if(takeoff.isLocked()) {
                    if(takeoffStep == 1) {
                        // not needed // Now clear elytraflight status to avoid anticheat and then set motion
                        // not needed // Trick server into setting onGround=true temporarily, this causes the
                        // not needed // if-statement in START_FALL_FLYING to fail and instead of starting the
                        // not needed // elytra, it stops it.
                        // not needed player.connection.sendPacket(new CPacketPlayer.Rotation(0,0, true));
                        // not needed // Now send the packet. The server now believes we aren't using our elytra
                        // not needed // anymore.
                        // not needed player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING));
                        // player.motionY = 0;
                    }
                    // wait a bit and actually start elytra
                    if(takeoffStep == 2) {
                        // not needed // Trick server into setting onGround=false temporarily, helps with reliability
                        // not needed player.connection.sendPacket(new CPacketPlayer.Rotation(0,0, false));
                        // not needed // Start elytraflying
                        // not needed player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING));
                    }
                    takeoffStep++;
                }
            }
        }
        if(player.onGround || player.motionY > 0.05) {
            takeoff.unlock();
        }
        if(!player.isElytraFlying())
            init = false;
    }
    
    private float speed() {
        return tpsSync ? speed * Utils.tpsMultiplier() : speed;
    }
    
    public void negateElytraFallMomentum(EntityPlayer player) {
        if (!player.isInWater()) {
            if (!player.isInLava()) {
                Vec3d vec3d = player.getLookVec();
                float f = player.rotationPitch * 0.017453292F;
                double d = vec3d.length();
                float f1 = MathHelper.cos(f);
                f1 = (float) ((double) f1 * (double) f1 * Math.min(1.0D, d / 0.4D));
                player.motionY -= -0.08D + (double) f1 * 0.06D;
            }
        }
    }
    
    @Override
    public void onDisable() {
        takeoffStep = 0;
    }
    
    @Override
    public void onChat(String s, String[] args) {
    }
    
    @Override
    public int danger() {
        return 2;
    }
}
