package de.tudbut.mod.client.ttcp.mods.movement;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.events.EventHandler;
import de.tudbut.mod.client.ttcp.mods.combat.AutoTotem;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Movement;
import de.tudbut.obj.Save;
import de.tudbut.tools.Lock;
import de.tudbut.tools.ThreadPool;

import java.util.Date;

@Movement
public class Scaffold extends Module {
    BlockPos last = null;
    long lastJump = 0;
    Lock swapLock = new Lock();
    ThreadPool swapThread = new ThreadPool(1, "Swap thread", true);
    
    
    @Save
    boolean tower = false;
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createBoolean("Tower", this, "tower"));
    }
    
    @Override
    public void onSubTick() {
        EntityPlayerSP player = TTCp.player;
        World world = TTCp.world;
        
        if(player.posY == (double) (int) player.posY)
            lastJump = 0;
    
        //noinspection ConstantConditions
        if (!(boolean) Utils.getPrivateField(EntityLivingBase.class, player, Utils.getFieldsForType(EntityLivingBase.class, boolean.class)[2])) {
            if (new Date().getTime() - lastJump > 500) {
                player.motionY = 0;
                player.onGround = true;
            }
        }
        else {
            lastJump = new Date().getTime();
        }
        Vec3d vec = player.getPositionVector();
        BlockPos pos;
    
        if(tower && player.movementInput.jump && player.motionX == 0 && player.motionZ == 0) {
            lastJump = new Date().getTime();
            player.motionY = 0.42F;
            player.onGround = false;
            pos = BlockUtils.getRealPos(vec.add(0,-0.2,0)).down();
        }
        else
            pos = BlockUtils.getRealPos(vec).down();
        
        if(world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
            
            if(player.getHeldItemMainhand().getCount() < 5 && player.getHeldItemMainhand().getCount() != 0) {
                Integer slot = InventoryUtils.getSlotWithItem(
                        player.inventoryContainer,
                        player.getHeldItemMainhand().getItem(),
                        new int[]{InventoryUtils.OFFHAND_SLOT},
                        5,
                        64
                );
                if(slot != null && !swapLock.isLocked()) {
                    swapLock.lock(1500);
                    swapThread.run(() -> {
                        InventoryUtils.inventorySwap(slot, player.inventory.currentItem + 36, AutoTotem.getInstance().sdelay, AutoTotem.getInstance().pdelay, AutoTotem.getInstance().cdelay);
                        if(EventHandler.ping[0] > 0)
                            swapLock.lock((int) EventHandler.ping[0]);
                    });
                }
            }
            else if(player.getHeldItemMainhand().getCount() == 0)
                toggle();
    
            if(player.movementInput.jump) {
                if(player.posY >= 0.4 || (Math.abs(Math.abs(player.posY) - Math.abs((long) player.posY)) < 0.05) || (Math.abs(Math.abs(player.posY) - Math.abs((long) player.posY)) > 0.25)) {
                    return;
                }
            }
        
            if(BlockUtils.placeBlock(pos, EnumHand.MAIN_HAND, true, false)) {
                if (player.onGround || tower)
                    player.motionY = 0;
                last = pos;
            }
            else {
                int dx = pos.getX() - last.getX();
                int dy = pos.getY() - last.getY();
                int dz = pos.getZ() - last.getZ();
                boolean b = false;
                for (int x = 1 ; x <= Math.abs(dx); x++) {
                    int n = dx < 0 ? -1 : 1;
                    if(!BlockUtils.placeBlock(last.add(n * x, 0, 0), EnumHand.MAIN_HAND, true, false))
                        b = true;
                }
                for (int y = 1 ; y <= Math.abs(dy); y++) {
                    int n = dy < 0 ? -1 : 1;
                    if(!BlockUtils.placeBlock(last.add(dx, n * y, 0), EnumHand.MAIN_HAND, true, false))
                        b = true;
                }
                for (int z = 1 ; z <= Math.abs(dz); z++) {
                    int n = dz < 0 ? -1 : 1;
                    if(!BlockUtils.placeBlock(last.add(dx, dy, n * z), EnumHand.MAIN_HAND, true, false))
                        b = true;
                }
                if(!b) {
                    last = pos;
                }
            }
        }
    }
}
