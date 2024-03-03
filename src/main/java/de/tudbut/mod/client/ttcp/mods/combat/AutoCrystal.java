package de.tudbut.mod.client.ttcp.mods.combat;

import de.tudbut.type.Vector3d;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.events.EventHandler;
import de.tudbut.mod.client.ttcp.events.ModuleEventRegistry;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Combat;
import de.tudbut.obj.DoubleTypedObject;
import de.tudbut.obj.Save;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static de.tudbut.mod.client.ttcp.utils.AutoCrystalUtil.*;
import static de.tudbut.mod.client.ttcp.utils.Tesselator.*;

@Combat
public class AutoCrystal extends Module {

    private static AutoCrystal instance;

    public static AutoCrystal getInstance() {
        return instance;
    }

    @Save
    float crystalRange = 5;
    @Save
    float minDmg = 0, maxDmg = 3;
    @Save
    float selfDamageCostM = 2;
    @Save
    boolean render = true;
    @Save
    boolean sequential = false;
    @Save
    boolean fastBreak = true;
    @Save
    boolean predict = false;
    @Save
    int predictMargin = 2;
    @Save
    int maxChain = 10;

    int chain = 0;
    float eidsPerSecond = 1;
    int lastEIDUpdateEIDs = -1;
    long lastEIDUpdate = System.currentTimeMillis();
    World lastWorld;

    EnumHand main = EnumHand.MAIN_HAND;
    EntityLivingBase currentTarget;
    ArrayList<DoubleTypedObject<BlockPos, Long>> ownCrystals = new ArrayList<>();


    { instance = this; }

    {
        ModuleEventRegistry.disableOnNewPlayer.add(this);
    }

    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createFloat(2, 10, "CrystalRange", this, "crystalRange"));
        subComponents.add(Setting.createFloat(0, 20, "MinDamage", this, "minDmg"));
        subComponents.add(Setting.createFloat(0.5f, 20, "MaxDamage", this, "maxDmg"));
        subComponents.add(Setting.createFloat(0, 10, "SelfDmgCost", this, "selfDamageCostM"));
        subComponents.add(Setting.createBoolean("Render", this, "render"));
        subComponents.add(Setting.createBoolean("Sequential", this, "sequential"));
        subComponents.add(Setting.createBoolean("FastBreak", this, "fastBreak"));
        subComponents.add(Setting.createBoolean("Predict", this, "predict"));
        subComponents.add(Setting.createInt(0, 40, "PredictMargin", this, "predictMargin"));
        subComponents.add(Setting.createInt(0, 40, "MaxChainLength", this, "maxChain"));

    }

    @Override
    public void onTick() {
        placeCrystal();
        breakCrystal();
    }

    public void updateOwnCrystals() {
        for (int i = 0; i < ownCrystals.size(); i++) {
            if(System.currentTimeMillis() - ownCrystals.get(i).t > 2000) {
                ownCrystals.remove(i--);
            }
        }
    }

    public void eid(int eid) {
        if(lastWorld != mc.world) {
            lastEIDUpdate = System.currentTimeMillis();
            lastEIDUpdateEIDs = -1;
            eidsPerSecond = 1;
            currentTarget = null;
        }

        long timePassed = System.currentTimeMillis() - lastEIDUpdate;
        if(lastEIDUpdateEIDs == -1)
            lastEIDUpdateEIDs = eid -1;
        int addedEIDs = eid - lastEIDUpdateEIDs;
        eidsPerSecond = (eidsPerSecond * 8f + addedEIDs) / (timePassed / 1000f);
        lastEIDUpdate = System.currentTimeMillis();
        lastEIDUpdateEIDs = eid;
    }

    private void remove(BlockPos pos) {
        for (int i = 0; i < ownCrystals.size(); i++) {
            if(ownCrystals.get(i).o.equals(pos.down())) {
                ownCrystals.remove(i);
                break;
            }
        }
    }

    @Override
    public boolean onPacket(Packet<?> packet) {

        // Phobos calls it "Predict", but it doesn't actually predict anything lmao
        if(packet instanceof SPacketSpawnObject && ((SPacketSpawnObject) packet).getType() == 51) {
            SPacketSpawnObject spawner = ((SPacketSpawnObject) packet);
            System.out.println("XX");
            if(fastBreak) {
                Vec3d vec = new Vec3d(spawner.getX(), spawner.getY(), spawner.getZ());
                if (
                        isCrystalInRange(spawner.getX(), spawner.getY(), spawner.getZ()) &&
                        getCostForPlacement(currentTarget, ((int) Math.floor(spawner.getX())), ((int) Math.floor(spawner.getY())), ((int) Math.floor(spawner.getZ()))) < 0
                ) {
                    hitCrystal(spawner.getEntityID(), vec);
                    if(++chain >= maxChain) {
                        return false;
                    }
                    BlockPos pos = BlockUtils.getRealPos(vec);
                    if (ensureCrystalsSelected())
                        return false;
                    ownCrystals.add(new DoubleTypedObject<>(pos.down(), System.currentTimeMillis()));
                    BlockUtils.clickOnBlock(pos.down(), main);
                    // This one actually does predict.
                    if (predict) {
                        int eid = Math.round(spawner.getEntityID() + eidsPerSecond * (EventHandler.ping[0]) / 1000f);
                        for (int i = -predictMargin; i <= predictMargin; i++) {
                            player.connection.sendPacket(createAttackPacket(eid + i));
                        }
                    }
                }
            }
        }
        if(packet instanceof SPacketDestroyEntities) {
            SPacketDestroyEntities remover = ((SPacketDestroyEntities) packet);
            int[] ids = remover.getEntityIDs();
            for (int i = 0; i < ids.length; i++) {
                notifyEntityDeath(ids[i]);
            }
        }

        return packet instanceof SPacketExplosion;
    }

    private boolean ensureCrystalsSelected() {
        return player.getHeldItemMainhand().getItem() != Items.END_CRYSTAL;
    }

    private void notifyEntityDeath(int id) {
        Entity entity;
        if((entity = mc.world.getEntityByID(id)) instanceof EntityEnderCrystal) {
            remove(BlockUtils.getRealPos(entity.getPositionVector()));
        }
    }

    public BlockPos findBestPos(EntityLivingBase toAttack) {
        BlockPos best = null;
        float bestCost = Float.POSITIVE_INFINITY;

        for (int ix = (int) Math.floor(-crystalRange); ix <= (int) Math.ceil(crystalRange); ix++) {
            for (int iy = (int) Math.floor(-crystalRange); iy <= (int) Math.ceil(crystalRange); iy++) {
                for (int iz = (int) Math.floor(-crystalRange); iz <= (int) Math.ceil(crystalRange); iz++) {
                    double x = ix + player.posX, y = iy + player.posY, z = iz + player.posZ;
                    BlockPos pos = new BlockPos(x,y,z);
                    if(canPlace(pos, crystalRange)) {
                        float cost = getCostForPlacement(toAttack, pos.getX(), pos.up().getY(), pos.getZ());

                        if(cost == Float.POSITIVE_INFINITY)
                            continue;
                        if (cost < bestCost) {
                            best = pos;
                            bestCost = cost;
                        }
                    }
                }
            }
        }

        return best;
    }

    public void findTarget() {
        if(currentTarget == null || currentTarget.getHealth() == 0) {
            EntityLivingBase best = null;
            float bestStat = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < mc.world.loadedEntityList.size(); i++) {
                Entity e =  mc.world.loadedEntityList.get(i);
                if(e instanceof EntityLivingBase) {
                    EntityLivingBase entity = ((EntityLivingBase) e);
                    float stat =
                            entity.getHealth() +
                            entity.getAbsorptionAmount() +
                            entity.getTotalArmorValue() -
                            entity.getDistance(player) * 20;
                    if(entity instanceof EntityPlayer) {
                        stat *= 100;
                    }
                    if(Arrays.stream(Utils.getAllies()).noneMatch(ally -> ally.equals(entity))) {
                        if (stat > bestStat) {
                            best = entity;
                            bestStat = stat;
                        }
                    }
                }
            }
            currentTarget = best;
            if(best != null)
                ChatUtils.print("New target: " + best + " at " + bestStat);
        }
    }

    public void placeCrystal() {

        if(lastWorld != mc.world) {
            lastEIDUpdate = System.currentTimeMillis();
            lastEIDUpdateEIDs = -1;
            eidsPerSecond = 1;
            currentTarget = null;
            lastWorld = mc.world;
        }

        findTarget();
        if(currentTarget == null)
            return;
        if(ensureCrystalsSelected())
            return;
        BlockPos pos = findBestPos(currentTarget);
        updateOwnCrystals();
        if(sequential && ownCrystals.size() != 0)
            return;
        if(pos != null) {
            ownCrystals.add(new DoubleTypedObject<>(pos, System.currentTimeMillis()));
            BlockUtils.clickOnBlock(pos, main);
            chain = 0;
        }
    }

    public void breakCrystal() {
        for (int i = 0; i < mc.world.loadedEntityList.size(); i++) {
            Entity e = mc.world.loadedEntityList.get(i);
            if (e instanceof EntityEnderCrystal) {
                if(ownCrystals.stream().anyMatch(c -> c.o.equals(BlockUtils.getRealPos(e.getPositionVector()).down()))) {
                    hitCrystal(e.getEntityId(), e.getPositionVector());
                    remove(BlockUtils.getRealPos(e.getPositionVector()));
                }
            }
        }
    }

    private void hitCrystal(int entityID, Vec3d pos) {
        Utils.setRotation(createRotations(createBB(pos)));
        player.connection.sendPacket(createAttackPacket(entityID));
        player.swingArm(main);
    }

    private boolean isCrystalInRange(double x, double y, double z) {
        return player.getPositionEyes(1).distanceTo(new Vec3d(x,y,z)) <= crystalRange;
    }
    
    public float getCostForPlacement(EntityLivingBase entityOther, int x, int y, int z) {
        float g = getExplosionCost(player, x, y, z);
        float h = getExplosionCost(entityOther, x, y, z);

        System.out.println(g);
        if(h < minDmg || (g > maxDmg && maxDmg != -1)) {
            return Float.POSITIVE_INFINITY;
        }
        
        return g * selfDamageCostM - h;
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
    
    @Override
    public int danger() {
        return 3;
    }

    public void selectObby() {

    }

    public enum State {
        IDLE,
        ATTACK,
        BREAK,
        FACEPLACE,
        
        ;
    }
    
    Vec3d pos = new Vec3d(0,0,0);
    
    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        try {
            if (this.enabled && TTCp.isIngame() && render) {
                Entity e = TTCp.mc.getRenderViewEntity();
                assert e != null;
                pos = e.getPositionEyes(event.getPartialTicks()).add(0, -e.getEyeHeight(), 0);
        
                EntityEnderCrystal[] crystals = TTCp.world.getEntities(EntityEnderCrystal.class, ent -> ent.getDistance(TTCp.player) < crystalRange * 5).toArray(new EntityEnderCrystal[0]);
        
                for (int i = 0 ; i < crystals.length ; i++) {
                    BlockPos bp = BlockUtils.getRealPos(crystals[i].getPositionVector());
                    float dmg = getExplosionCost(TTCp.player, bp.getX(), bp.getY(), bp.getZ());

                    float f1 = Color.RGBtoHSB(255,0,255, null)[0]-1;
                    float f2 = Color.RGBtoHSB(0,255,0, null)[0];
                    float hue = f2 + (f1 - f2) * Math.min(dmg / 20f, 1);
                    int color = 0xff000000 | Color.HSBtoRGB(hue, 1, 1);

                    drawAroundBlock(new Vector3d(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5), color);
                    depth(false);
                    EntityRenderer.drawNameplate(mc.fontRenderer, dmg + "", (float)(-pos.x + bp.getX() + 0.5), (float)(-pos.y + bp.getY() + 0.66), (float)(-pos.z + bp.getZ() + 0.5), 0, player.rotationYaw, player.rotationPitch, false, false);
                    depth(true);
                }
        
                for (int i = 0 ; i < ownCrystals.size() ; i++) {
                    BlockPos pos = ownCrystals.get(i).o;
                    drawAroundBlock(new Vector3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), 0x808080ff);
                }
            }
        } catch (NullPointerException ignored) { }
    }
    
    public void drawAroundBlock(Vector3d pos, int color) {
        try {
    
            ready();
            translate(-this.pos.x, -this.pos.y, -this.pos.z);
            color(color);
            depth(false);
            begin(GL11.GL_QUADS);
    
    
            // bottom
            put(pos.getX() - 0.5, pos.getY() - 0.01, pos.getZ() + 0.5);
            put(pos.getX() + 0.5, pos.getY() - 0.01, pos.getZ() + 0.5);
            put(pos.getX() + 0.5, pos.getY() - 0.01, pos.getZ() - 0.5);
            put(pos.getX() - 0.5, pos.getY() - 0.01, pos.getZ() - 0.5);
    
            next();

            /*
            // top
            put(pos.getX() - 0.5, pos.getY() + 1.01, pos.getZ() + 0.5);
            put(pos.getX() + 0.5, pos.getY() + 1.01, pos.getZ() + 0.5);
            put(pos.getX() + 0.5, pos.getY() + 1.01, pos.getZ() - 0.5);
            put(pos.getX() - 0.5, pos.getY() + 1.01, pos.getZ() - 0.5);
    
            next();

            // z -
            put(pos.getX() - 0.5, pos.getY() + 1.01, pos.getZ() - 0.5);
            put(pos.getX() + 0.5, pos.getY() + 1.01, pos.getZ() - 0.5);
            put(pos.getX() + 0.5, pos.getY() - 0.01, pos.getZ() - 0.5);
            put(pos.getX() - 0.5, pos.getY() - 0.01, pos.getZ() - 0.5);
    
            next();
    
            // z +
            put(pos.getX() - 0.5, pos.getY() + 1.01, pos.getZ() + 0.5);
            put(pos.getX() + 0.5, pos.getY() + 1.01, pos.getZ() + 0.5);
            put(pos.getX() + 0.5, pos.getY() - 0.01, pos.getZ() + 0.5);
            put(pos.getX() - 0.5, pos.getY() - 0.01, pos.getZ() + 0.5);
    
            next();
    
            // x -
            put(pos.getX() - 0.5, pos.getY() + 1.01, pos.getZ() - 0.5);
            put(pos.getX() - 0.5, pos.getY() + 1.01, pos.getZ() + 0.5);
            put(pos.getX() - 0.5, pos.getY() - 0.01, pos.getZ() + 0.5);
            put(pos.getX() - 0.5, pos.getY() - 0.01, pos.getZ() - 0.5);
    
            next();
    
            // x +
            put(pos.getX() + 0.5, pos.getY() + 1.01, pos.getZ() - 0.5);
            put(pos.getX() + 0.5, pos.getY() + 1.01, pos.getZ() + 0.5);
            put(pos.getX() + 0.5, pos.getY() - 0.01, pos.getZ() + 0.5);
            put(pos.getX() + 0.5, pos.getY() - 0.01, pos.getZ() - 0.5);*/
    
            end();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
