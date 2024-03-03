package de.tudbut.mod.client.ttcp.mixin;

import com.google.common.base.Predicates;
import com.google.common.base.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.mods.combat.HitCorrection;
import de.tudbut.mod.client.ttcp.mods.combat.Reach;

import java.util.List;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {
    
    @Inject(method = "hurtCameraEffect", at = { @At("HEAD") }, cancellable = true)
    public void hurtCameraEffect(float partialTicks, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
    }
    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    public void displayTotem(ItemStack stack, CallbackInfo callbackInfo) {
        callbackInfo.cancel();
    }
    
    @Final
    @Shadow
    private Minecraft mc;
    
    @Shadow
    private Entity pointedEntity;
    
    /**
     * @author Minecraft
     */
    @Inject(method = "getMouseOver", at = @At("HEAD"), cancellable = true)
    public void getMouseOver(float partialTicks, CallbackInfo callbackInfo)
    {
        callbackInfo.cancel();
        Reach reach = TTCp.getModule(Reach.class);
        
        
        Entity entity = this.mc.getRenderViewEntity();
        
        if (entity != null) {
            if (this.mc.world != null) {
                this.mc.profiler.startSection("pick");
                this.mc.pointedEntity = null;
                double d0 = reach.enabled ? Reach.reach : 3;
                this.mc.objectMouseOver = entity.rayTrace(TTCp.mc.playerController.getBlockReachDistance(), partialTicks);
                Vec3d vec3d = entity.getPositionEyes(partialTicks);
                boolean flag = false;
                double d1 = d0;
                
                if (this.mc.playerController.extendedReach()) {
                    d1 += 1;
                    d0 = d1;
                }
                else {
                    if (d0 > (reach.enabled ? Reach.reach : 3)) {
                        flag = true;
                    }
                }
                
                if (this.mc.objectMouseOver != null) {
                    d1 = this.mc.objectMouseOver.hitVec.distanceTo(vec3d);
                }
                
                Vec3d vec3d1 = entity.getLook(1.0F);
                Vec3d vec3d2 = vec3d.add(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0);
                this.pointedEntity = null;
                Vec3d vec3d3 = null;
                List<Entity> list = this.mc.world.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().expand(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0).grow(1.0D, 1.0D, 1.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, p_apply_1_ -> p_apply_1_ != null && p_apply_1_.canBeCollidedWith()));
                double d2 = d1;
                
                for (int j = 0; j < list.size(); ++j) {
                    Entity entity1 = list.get(j);
                    AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(entity1.getCollisionBorderSize()).grow(TTCp.getModule(HitCorrection.class).enabled ? HitCorrection.amount : 0);
                    RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);
                    
                    if (axisalignedbb.contains(vec3d)) {
                        if (d2 >= 0.0D) {
                            this.pointedEntity = entity1;
                            vec3d3 = raytraceresult == null ? vec3d : raytraceresult.hitVec;
                            d2 = 0.0D;
                        }
                    }
                    else if (raytraceresult != null) {
                        double d3 = vec3d.distanceTo(raytraceresult.hitVec);
                        
                        if (d3 < d2 || d2 == 0.0D) {
                            if (entity1.getLowestRidingEntity() == entity.getLowestRidingEntity() && !entity1.canRiderInteract()) {
                                if (d2 == 0.0D) {
                                    this.pointedEntity = entity1;
                                    vec3d3 = raytraceresult.hitVec;
                                }
                            }
                            else {
                                this.pointedEntity = entity1;
                                vec3d3 = raytraceresult.hitVec;
                                d2 = d3;
                            }
                        }
                    }
                }
                
                if (this.pointedEntity != null && flag && vec3d.distanceTo(vec3d3) > (reach.enabled ? Reach.reach : 3)) {
                    this.pointedEntity = null;
                    this.mc.objectMouseOver = new RayTraceResult(RayTraceResult.Type.MISS, vec3d3, null, new BlockPos(vec3d3));
                }
                
                if (this.pointedEntity != null && (d2 < d1 || this.mc.objectMouseOver == null)) {
                    this.mc.objectMouseOver = new RayTraceResult(this.pointedEntity, vec3d3);
                    
                    if (this.pointedEntity instanceof EntityLivingBase || this.pointedEntity instanceof EntityItemFrame) {
                        this.mc.pointedEntity = this.pointedEntity;
                    }
                }
                
                this.mc.profiler.endSection();
            }
        }
    }
}
