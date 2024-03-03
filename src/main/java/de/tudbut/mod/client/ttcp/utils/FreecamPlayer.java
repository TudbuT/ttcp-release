package de.tudbut.mod.client.ttcp.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.MoverType;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.mods.rendering.Freecam;

import javax.annotation.Nonnull;

@SideOnly(Side.CLIENT)
public class FreecamPlayer extends EntityOtherPlayerMP
{
    public MovementInput movementInput;
    protected Minecraft mc;
    protected final EntityPlayerSP original;
    
    public FreecamPlayer(EntityPlayerSP playerSP, World world)
    {
        super(world, playerSP.getGameProfile());
        this.dimension = playerSP.dimension;
        this.original = playerSP;
        this.mc = Minecraft.getMinecraft();
        this.movementInput = playerSP.movementInput;
        preparePlayerToSpawn();
        capabilities.allowFlying = true;
        capabilities.isFlying = true;
        this.setPositionAndRotation(playerSP.posX, playerSP.posY, playerSP.posZ, playerSP.rotationYaw, playerSP.rotationPitch);
    }
    
    @Override
    @Nonnull
    public String getName() {
        return original.getName() + "\u0000";
    }
    
    @Override
    public boolean isSpectator() {
        return true;
    }
    
    public void onLivingUpdate()
    {
        if(TTCp.mc.world == null) {
            Freecam.getInstance().onDisable();
            Freecam.getInstance().enabled = false;
            return;
        }
        TTCp.mc.renderChunksMany = false;
        TTCp.mc.player.setInvisible(false);
        setInvisible(true);
        
        inventory.copyInventory(TTCp.player.inventory);
    
        prevRotationYaw = rotationYaw;
        prevRotationPitch = rotationPitch;
        prevRotationYawHead = rotationYawHead;
        setRotation(original.rotationYaw, original.rotationPitch);
        setRotationYawHead(original.rotationYaw);
        original.prevRenderArmYaw = original.renderArmYaw;
        original.prevRenderArmPitch = original.renderArmPitch;
        original.renderArmPitch = (float)((double)original.renderArmPitch + (double)(original.rotationPitch - original.renderArmPitch) * 0.5D);
        original.renderArmYaw = (float)((double)original.renderArmYaw + (double)(original.rotationYaw - original.renderArmYaw) * 0.5D);
        updateEntityActionState();
    
        movementInput.updatePlayerMoveState();
        Vec2f movementVec = movementInput.getMoveVector();
    
        float f1 = MathHelper.sin(rotationYaw * 0.017453292F);
        float f2 = MathHelper.cos(rotationYaw * 0.017453292F);
        double x = movementVec.x * f2 - movementVec.y * f1;
        double y = (movementInput.jump ? 1 : 0) + (movementInput.sneak ? -1 : 0);
        double z = movementVec.y * f2 + movementVec.x * f1;
        float d = (float) Math.sqrt(x * x + y * y + z * z);
        
        movementInput.jump = false;
        movementInput.sneak = false;
        movementInput.forwardKeyDown = false;
        movementInput.backKeyDown = false;
        movementInput.leftKeyDown = false;
        movementInput.rightKeyDown = false;
        movementInput.moveForward = 0;
        movementInput.moveStrafe = 0;
    
        if(d < 1) {
            d = 1;
        }
    
        motionX = x / d;
        motionY = y / d;
        motionZ = z / d;
        
        noClip = true;
        move(MoverType.SELF, motionX, motionY, motionZ);
        
        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;
    }
}
