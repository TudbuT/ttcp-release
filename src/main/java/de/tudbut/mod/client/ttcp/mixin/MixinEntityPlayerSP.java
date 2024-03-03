package de.tudbut.mod.client.ttcp.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.tudbut.mod.client.ttcp.utils.Utils;

@Mixin(value = EntityPlayerSP.class, priority = 934759)
public class MixinEntityPlayerSP extends EntityPlayerSP {

    @Shadow
    private boolean serverSprintState, serverSneakState, prevOnGround, autoJumpEnabled;
    @Shadow
    private double lastReportedPosX, lastReportedPosY, lastReportedPosZ;
    @Shadow
    private float lastReportedYaw, lastReportedPitch;
    @Shadow
    private int positionUpdateTicks;

    public MixinEntityPlayerSP(Minecraft p_i47378_1_, World p_i47378_2_, NetHandlerPlayClient p_i47378_3_, StatisticsManager p_i47378_4_, RecipeBook p_i47378_5_) {
        super(p_i47378_1_, p_i47378_2_, p_i47378_3_, p_i47378_4_, p_i47378_5_);
    }

    @Inject(method = "onUpdateWalkingPlayer", cancellable = true, at = @At("HEAD"))
    public void onUpdateWalkingPlayer(CallbackInfo ci) {
        Vec2f rotation = Utils.getRotation();
        if(rotation != null) {
            ci.cancel();
            Utils.markRotationSent();

            boolean flag = this.isSprinting();

            if (flag != this.serverSprintState)
            {
                if (flag)
                {
                    this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.START_SPRINTING));
                }
                else
                {
                    this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SPRINTING));
                }

                this.serverSprintState = flag;
            }

            boolean flag1 = this.isSneaking();

            if (flag1 != this.serverSneakState)
            {
                if (flag1)
                {
                    this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.START_SNEAKING));
                }
                else
                {
                    this.connection.sendPacket(new CPacketEntityAction(this, CPacketEntityAction.Action.STOP_SNEAKING));
                }

                this.serverSneakState = flag1;
            }

            if (this.isCurrentViewEntity())
            {
                AxisAlignedBB axisalignedbb = this.getEntityBoundingBox();
                double d0 = this.posX - this.lastReportedPosX;
                double d1 = axisalignedbb.minY - this.lastReportedPosY;
                double d2 = this.posZ - this.lastReportedPosZ;
                double d3 = (double)(this.rotationYaw - this.lastReportedYaw);
                double d4 = (double)(this.rotationPitch - this.lastReportedPitch);
                ++this.positionUpdateTicks;
                boolean flag2 = d0 * d0 + d1 * d1 + d2 * d2 > 9.0E-4D || this.positionUpdateTicks >= 20;
                boolean flag3 = true;

                if (this.isRiding())
                {
                    this.connection.sendPacket(new CPacketPlayer.PositionRotation(this.motionX, -999.0D, this.motionZ, rotation.x, rotation.y, this.onGround));
                    flag2 = false;
                }
                else if (flag2 && flag3)
                {
                    this.connection.sendPacket(new CPacketPlayer.PositionRotation(this.posX, axisalignedbb.minY, this.posZ, rotation.x, rotation.y, this.onGround));
                }
                else if (flag2)
                {
                    this.connection.sendPacket(new CPacketPlayer.Position(this.posX, axisalignedbb.minY, this.posZ, this.onGround));
                }
                else if (flag3)
                {
                    this.connection.sendPacket(new CPacketPlayer.Rotation(rotation.x, rotation.y, this.onGround));
                }
                else if (this.prevOnGround != this.onGround)
                {
                    this.connection.sendPacket(new CPacketPlayer(this.onGround));
                }

                if (flag2)
                {
                    this.lastReportedPosX = this.posX;
                    this.lastReportedPosY = axisalignedbb.minY;
                    this.lastReportedPosZ = this.posZ;
                    this.positionUpdateTicks = 0;
                }

                this.prevOnGround = this.onGround;
                this.autoJumpEnabled = this.mc.gameSettings.autoJump;
            }
        }
    }
}
