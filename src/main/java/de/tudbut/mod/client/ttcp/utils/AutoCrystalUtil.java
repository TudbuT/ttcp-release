package de.tudbut.mod.client.ttcp.utils;

import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledDirectByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import de.tudbut.mod.client.ttcp.TTCp;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class AutoCrystalUtil {

    private static Minecraft mc = Minecraft.getMinecraft();


    public static AxisAlignedBB createBB(Vec3d crystalPos) {
        return new AxisAlignedBB(crystalPos.x - 1, crystalPos.y, crystalPos.z - 1, crystalPos.y + 2, crystalPos.x + 1, crystalPos.x + 1);
    }

    public static Vec2f createRotations(AxisAlignedBB box) {
        Vec3d posEyes = mc.player.getPositionEyes(1);

        Vec3d best = null;//new Vec3d(box.minX + .5 * box.maxX, box.minY + .5 * box.maxY, box.minZ + .5 * box.maxZ);
        double bestDistance = Double.POSITIVE_INFINITY;

        for (float ix = 0; ix < 1; ix+=.2f) {
            for (float iy = 0; iy < 1; iy+=.2f) {
                for (float iz = 0; iz < 1; iz+=.2f) {
                    double x = box.minX + ix * box.maxX;
                    double y = box.minY + iy * box.maxY;
                    double z = box.minZ + iz * box.maxZ;
                    Vec3d vec = new Vec3d(x,y,z);

                    RayTraceResult trace = mc.world.rayTraceBlocks(posEyes, vec);

                    if(trace == null) {
                        continue;
                    }

                    double f = vec.distanceTo(posEyes);
                    if(f < bestDistance) {
                        bestDistance = f;
                        best = vec;
                    }
                }
            }
        }

        return BlockUtils.getLegitRotationsVector(best);
    }

    public static CPacketUseEntity createAttackPacket(int eid) {
        CPacketUseEntity packet = new CPacketUseEntity();
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeVarInt(eid);
        buffer.writeEnumValue(CPacketUseEntity.Action.ATTACK);
        try {
            packet.readPacketData(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packet;
    }

    public static float getExplosionCost(EntityLivingBase entity, double x, double y, double z) {

        x += 0.5;
        z += 0.5;
        World world = TTCp.world;

        float dmg = 0;
        float f3 = 6.0F * 2.0F;
        Vec3d vec3d = new Vec3d(x, y, z);

        double d12 = entity.getDistance(x, y, z) / (double)f3;

        if (d12 <= 1.0D)
        {
            double d5 = entity.posX - x;
            double d7 = entity.posY + (double)entity.getEyeHeight() - y;
            double d9 = entity.posZ - z;
            double d13 = MathHelper.sqrt(d5 * d5 + d7 * d7 + d9 * d9);

            if (d13 != 0.0D)
            {
                double d14 = world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
                double d10 = (1.0D - d12) * d14;
                dmg += (float)((int)((d10 * d10 + d10) / 2.0D * 7.0D * (double)f3 + 1.0D));
            }
        }


        dmg = CombatRules.getDamageAfterAbsorb(dmg, (float)entity.getTotalArmorValue(), (float)entity.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue());

        if (entity.isPotionActive(MobEffects.RESISTANCE)) {
            int i = (Objects.requireNonNull(entity.getActivePotionEffect(MobEffects.RESISTANCE)).getAmplifier() + 1) * 5;
            int j = 25 - i;
            float f = dmg * (float)j;
            dmg = f / 25.0F;
        }

        if (dmg <= 0.0F) {
            return 0.0F;
        }
        else {
            try {
                int k = EnchantmentHelper.getEnchantmentModifierDamage(entity.getArmorInventoryList(), DamageSource.GENERIC);

                if (k > 0) {
                    dmg = CombatRules.getDamageAfterMagicAbsorb(dmg, (float) k);
                }
            } catch (NullPointerException ignore) { }
        }
        return Math.max(dmg, 0.0F);
    }

    public static boolean canPlace(BlockPos pos, float crystalRange) {

        Entity player = TTCp.player;

        if(player.getPositionEyes(1).distanceTo(new Vec3d(pos)) > crystalRange) {
            return false;
        }

        World world = TTCp.world;
        IBlockState iblockstate = world.getBlockState(pos);

        if (iblockstate.getBlock() != Blocks.OBSIDIAN && iblockstate.getBlock() != Blocks.BEDROCK)
        {
            return false;
        }
        else {
            BlockPos blockpos = pos.up();
            BlockPos blockpos1 = blockpos.up();
            boolean flag = !world.isAirBlock(blockpos) && !world.getBlockState(blockpos).getBlock().isReplaceable(world, blockpos);
            flag = flag | (!world.isAirBlock(blockpos1) && !world.getBlockState(blockpos1).getBlock().isReplaceable(world, blockpos1));

            if (flag) {
                return false;
            }
            else {
                double d0 = blockpos.getX();
                double d1 = blockpos.getY();
                double d2 = blockpos.getZ();
                AxisAlignedBB thisHitbox = new AxisAlignedBB(d0, d1, d2, d0 + 1.0D, d1 + 2.0D, d2 + 1.0D);
                List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, thisHitbox);

                return list.isEmpty();
            }
        }
    }
}
