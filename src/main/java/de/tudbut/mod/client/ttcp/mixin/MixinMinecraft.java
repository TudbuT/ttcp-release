package de.tudbut.mod.client.ttcp.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import de.tudbut.mod.client.ttcp.TTCp;

/**
 * @author TudbuT
 * @since 23 Nov 2021
 */

@Mixin(Minecraft.class)
public class MixinMinecraft {
    
    @Shadow
    private int leftClickCounter;
    @Shadow
    public RayTraceResult objectMouseOver;
    @Shadow
    public PlayerControllerMP playerController;
    @Shadow
    public EntityPlayerSP player;
    
    /**
     * @author TudbuT, Minecraft
     */
    /*
    @Overwrite
    private void clickMouse() {
        if (this.leftClickCounter <= 0)
        {
            if (this.objectMouseOver == null)
            {
                System.err.println("Null returned as 'hitResult', this shouldn't happen!");
            }
            else if (!this.player.isRowingBoat())
            {
                switch (this.objectMouseOver.typeOfHit)
                {
                    case ENTITY:
                        this.playerController.attackEntity(this.player, this.objectMouseOver.entityHit);
                        break;
                    case BLOCK:
                        BlockPos blockpos = this.objectMouseOver.getBlockPos();
                    
                        if (!TTCp.mc.world.isAirBlock(blockpos))
                        {
                            this.playerController.clickBlock(blockpos, this.objectMouseOver.sideHit);
                            break;
                        }
                
                    case MISS:
                        this.player.resetCooldown();
                        ForgeHooks.onEmptyLeftClick(this.player);
                }
            
                this.player.swingArm(EnumHand.MAIN_HAND);
            }
        }
    }*/
}
