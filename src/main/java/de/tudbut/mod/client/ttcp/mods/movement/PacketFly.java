package de.tudbut.mod.client.ttcp.mods.movement;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Movement;
import de.tudbut.mod.client.ttcp.utils.FlightBot;
import de.tudbut.obj.Save;
import de.tudbut.parsing.TCN;
import de.tudbut.tools.ConfigSaverTCN2;

@Movement
public class PacketFly extends Module {
    
    double posX, posY, posZ;
    
    @Save
    Mode mode = Mode.CREATIVE;
    
    @Save
    float speed = 0.4f;
    
    @Save
    boolean glide = true;
    
    private boolean forceSendOK;
    
    enum Mode {
        CREATIVE,
        CONTROL,
        CONTROL_PACKET,
        FORCE,
        BOOST,
        ELYTRA,
        DAMAGE,
        ELYTRAFORCE,
        ;
    }
    
    
    @Save
    boolean useUWP = false;
    
    @Save
    int forceOffset = -20;
    @Save
    boolean forceP0 = false;
    @Save
    boolean forceP1 = true;
    @Save
    boolean forceGround = true;
    
    @Save
    boolean confirmPacket = true;
    
    @Save
    boolean antikick = false;
    
    @Save
    boolean predict = true;
    int tpid = 0;

    int lastTicksExisted = 0;
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createEnum(Mode.class, "Mode", this, "mode"));
        subComponents.add(Setting.createBoolean("UWP", this, "useUWP"));
        subComponents.add(Setting.createFloat(0.1f, 20, "Speed (b/s)", this, "speed"));
        subComponents.add(Setting.createInt(-1000, 1000, "Force offset", this, "forceOffset"));
        subComponents.add(Setting.createBoolean("MainPacket type", this, "forceP0"));
        subComponents.add(Setting.createBoolean("ForcePacket type", this, "forceP1"));
        subComponents.add(Setting.createBoolean("Force onGround", this, "forceGround"));
        subComponents.add(Setting.createBoolean("Constant glide", this, "glide"));
        subComponents.add(Setting.createBoolean("ConfirmPacket", this, "confirmPacket"));
        subComponents.add(Setting.createBoolean("AntiKick", this, "antikick"));
        subComponents.add(Setting.createBoolean("Predict ForceRet", this, "predict"));
    }
    
    @Override
    public void onEnable() {
        posX = player.posX; posY = player.posY; posZ = player.posZ;
    }
    
    @Override
    public void onTick() {
        if(FlightBot.isActive() && !FlightBot.allowPacketFly)
            return;

        EntityPlayerSP player = TTCp.player;
        
        if(player.ticksExisted < lastTicksExisted) 
            lastTicksExisted = player.ticksExisted;

        PlayerCapabilities capabilities = player.capabilities;
        
        capabilities.isFlying = mode == Mode.CREATIVE;
        float speed = this.speed / 20f;


        if(!FlightBot.allowPacketFly) {
            player.motionX = player.motionY = player.motionZ = 0;
        }
    
        if(glide) {
            player.motionY -= 0.05 * speed;
        }
        
        if(forceGround)
            player.onGround = true;
        
        if(mode == Mode.CONTROL) {
            Vec2f movementVec = player.movementInput.getMoveVector();
    
            float f1 = MathHelper.sin(player.rotationYaw * 0.017453292F);
            float f2 = MathHelper.cos(player.rotationYaw * 0.017453292F);
            double x = movementVec.x * f2 - movementVec.y * f1;
            double y = (player.movementInput.jump ? 1 : 0) + (player.movementInput.sneak ? -1 : 0);
            double z = movementVec.y * f2 + movementVec.x * f1;
    
            if(x == 0 && y == 0 && z == 0) {
                return;
            }
    
            float d = (float) Math.sqrt(x * x + y * y + z * z);
    
            if (d < 1) {
                d = 1;
            }
    
            player.motionX += x / d * speed;
            player.motionY += y / d * speed;
            player.motionZ += z / d * speed;
        }
    
        if(mode == Mode.CONTROL_PACKET) {
            Vec2f movementVec = player.movementInput.getMoveVector();
        
            float f1 = MathHelper.sin(player.rotationYaw * 0.017453292F);
            float f2 = MathHelper.cos(player.rotationYaw * 0.017453292F);
            double x = movementVec.x * f2 - movementVec.y * f1;
            double y = (player.movementInput.jump ? 1 : 0) + (player.movementInput.sneak ? -1 : 0);
            double z = movementVec.y * f2 + movementVec.x * f1;
        
            if(x == 0 && y == 0 && z == 0) {
                return;
            }
        
            float d = (float) Math.sqrt(x * x + y * y + z * z);
        
            if (d < 1) {
                d = 1;
            }
        
            double posX = player.posX + x / d * speed;
            double posY = player.posY + y / d * speed;
            double posZ = player.posZ + z / d * speed;
        
            player.connection.sendPacket(new CPacketPlayer.PositionRotation(player.posX = posX, player.posY = posY, player.posZ = posZ, player.rotationYaw, player.rotationPitch, forceP0));
        }
        
        if(mode == Mode.BOOST) {
            Vec2f movementVec = player.movementInput.getMoveVector();
    
            float f1 = MathHelper.sin(player.rotationYaw * 0.017453292F);
            float f2 = MathHelper.cos(player.rotationYaw * 0.017453292F);
            double x = movementVec.x * f2 - movementVec.y * f1;
            double y = (player.movementInput.jump ? 1 : 0) + (player.movementInput.sneak ? -1 : 0);
            double z = movementVec.y * f2 + movementVec.x * f1;
    
            if(x == 0 && y == 0 && z == 0) {
                player.motionY -= 1;
                return;
            }
    
            float d = (float) Math.sqrt(x * x + y * y + z * z);
    
            if (d < 1) {
                d = 1;
            }
    
            player.motionX += x / d * speed;
            player.motionY += y * 2 * speed;
            player.motionZ += z / d * speed;
        }
    
        if(mode == Mode.FORCE) {
            Vec2f movementVec = player.movementInput.getMoveVector();
        
            float f1 = MathHelper.sin(player.rotationYaw * 0.017453292F);
            float f2 = MathHelper.cos(player.rotationYaw * 0.017453292F);
            double x = movementVec.x * f2 - movementVec.y * f1;
            double y = (player.movementInput.jump ? 1 : 0) + (player.movementInput.sneak ? -1 : 0);
            double z = movementVec.y * f2 + movementVec.x * f1;
            player.motionX = 0;
            player.motionY = 0;
            player.motionZ = 0;
            
            if(x == 0 && y == 0 && z == 0) {
                return;
            }
            
            float d = (float) Math.sqrt(x * x + y * y + z * z);
        
            if (d < 1) {
                d = 1;
            }
    
            posX = player.posX += x / d * speed;
            posY = player.posY += y / d * speed;
            posZ = player.posZ += z / d * speed;
    
            forceSendOK = true;
            player.connection.sendPacket(new CPacketPlayer.PositionRotation(posX, posY, posZ, player.rotationYaw, player.rotationPitch, forceP0));
            forceSendOK = true;
            player.connection.sendPacket(new CPacketPlayer.Position(posX, posY + forceOffset, posZ, forceP1));
        }
    
    
        if(mode == Mode.ELYTRA) {
            Vec2f movementVec = player.movementInput.getMoveVector();
        
            float f1 = MathHelper.sin(player.rotationYaw * 0.017453292F);
            float f2 = MathHelper.cos(player.rotationYaw * 0.017453292F);
            double x = movementVec.x * f2 - movementVec.y * f1;
            double y = (player.movementInput.jump ? 1 : 0) + (player.movementInput.sneak ? -1 : 0);
            double z = movementVec.y * f2 + movementVec.x * f1;
    
            if(x == 0 && y == 0 && z == 0) {
                return;
            }
    
            float d = (float) Math.sqrt(x * x + y * y + z * z);
        
            if (d < 1) {
                d = 1;
            }
    
            double posX = player.posX + x / d * speed;
            double posY = player.posY + y / d * speed;
            double posZ = player.posZ + z / d * speed;
        
            player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_FALL_FLYING));
            player.connection.sendPacket(new CPacketPlayer.PositionRotation(posX, posY, posZ, player.rotationYaw, player.rotationPitch, forceP0));
            
        }
        
        if(mode == Mode.DAMAGE) {
            Vec2f movementVec = player.movementInput.getMoveVector();
        
            float f1 = MathHelper.sin(player.rotationYaw * 0.017453292F);
            float f2 = MathHelper.cos(player.rotationYaw * 0.017453292F);
            double x = movementVec.x * f2 - movementVec.y * f1;
            double y = (player.movementInput.jump ? 1 : 0) + (player.movementInput.sneak ? -1 : 0);
            double z = movementVec.y * f2 + movementVec.x * f1;
    
            if(x == 0 && y == 0 && z == 0) {
                return;
            }
    
            float d = (float) Math.sqrt(x * x + y * y + z * z);
        
            if (d < 1) {
                d = 1;
            }
        
        
            double posX = player.posX + x / d * speed;
            double posY = player.posY + y / d * speed;
            double posZ = player.posZ + z / d * speed;
            
            
            player.connection.sendPacket(new CPacketPlayer.PositionRotation(posX, posY, posZ, player.rotationYaw, player.rotationPitch, forceP0));
            player.connection.sendPacket(new CPacketPlayer.Position(posX, posY + forceOffset, posZ, forceP1));
        }
        if(glide)
            player.motionY -= 0.05 * speed;
    
        if(useUWP)
            player.connection.sendPacket(new CPacketInput((float) player.motionX / this.speed, (float) player.motionZ / this.speed, player.motionY > 0,player.motionY < 0 || player.isSneaking()));
    
    
    }
    
    @Override
    public void onDisable() {
        TTCp.player.capabilities.isFlying = false;
    }
    
    @Override
    public boolean onPacket(Packet<?> packet) {
        boolean b = false;
        if(packet instanceof SPacketPlayerPosLook) {
            posX = player.posX = ((SPacketPlayerPosLook) packet).getX();
            posY = player.posY = ((SPacketPlayerPosLook) packet).getY();
            posZ = player.posZ = ((SPacketPlayerPosLook) packet).getZ();
        }
        if(!useUWP && packet instanceof CPacketInput)
            b = true;
        if ((mode == Mode.FORCE ) && (packet instanceof CPacketPlayer.Position || packet instanceof CPacketPlayer.PositionRotation)) {
            if (!forceSendOK) {
                b = true;
            }
            forceSendOK = false;
        }
        if(packet instanceof CPacketPlayer && mc.player.ticksExisted >= lastTicksExisted + 15) {
            try {
                TCN fixer = new TCN();
                fixer.set(CPacketPlayer.class.getDeclaredFields()[1].getName(), ((CPacketPlayer)packet).getY(mc.player.posY) - 0.3);
                ConfigSaverTCN2.read(fixer, packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
            lastTicksExisted = mc.player.ticksExisted;
        }
        return b;
    }
}
