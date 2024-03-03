package de.tudbut.mod.client.ttcp.mods.misc;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Misc;

import java.util.Objects;

/**
 * @author TudbuT
 * @since 16 Feb 2022
 */

@Misc
public class Highway extends Module {
    int stage = -1;
    // 0 = Break
    // 1 = Remove Lava
    // 2 = Break
    // 3 = Place bottom
    // 4 = Place side 1
    // 5 = Place side 2
    
    int y = -1;
    boolean wait = false;
    EnumFacing lastDirection;
    
    @Override
    public boolean doStoreEnabled() {
        return false;
    }
    
    @Override
    public void onEnable() {
        stage = -1;
        y = ((int) mc.player.posY);
        getFill().place = false;
        getBreak().doBreak = false;
        nextStage();
    }
    
    public EnumFacing direction() {
        return mc.player.getHorizontalFacing();
    }
    
    private Fill getFill() {
        return TTCp.getModule(Fill.class);
    }
    
    private Break getBreak() {
        return TTCp.getModule(Break.class);
    }
    
    public void selectObby() {
        Integer obbySlot = InventoryUtils.getSlotWithItem(TTCp.player.inventoryContainer, Blocks.OBSIDIAN, Utils.range(0, 8), 1, 64);
        
        if(obbySlot == null) {
            InventoryUtils.setCurrentSlot(8);
            BlockPos pos = BlockUtils.getRealPos(mc.player.getPositionVector());
            getFill().placeBlockIfPossible(pos.getX(), pos.getY() + 2, pos.getZ());
            pos = pos.add(0,2,0);
            player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING));
            BlockUtils.clickOnBlock(pos, EnumHand.MAIN_HAND);
            wait = true;
        }
        
        ResourceLocation slotType = TTCp.player.inventoryContainer.getSlot(36 + 7).getStack().getItem().getRegistryName();
        if (obbySlot != null && (slotType == null || !slotType.toString().equals(Objects.requireNonNull(Blocks.OBSIDIAN.getRegistryName()).toString())))
            InventoryUtils.inventorySwap(obbySlot, 36 + 7, 0, 0, 0);
    }

    boolean needToFixY = false;
    int fixYTimer = 0;
    
    public void onTick() {
        if(((int) mc.player.posY) < y) {
            needToFixY = true;
        }
        if(needToFixY && ((int) mc.player.posY) == y && player.onGround) {
            player.travel(0,0,-1);
            needToFixY = false;
            fixYTimer = 10;
        }
        if(needToFixY) {
            if (player.onGround) player.jump();
            player.travel(0,0,-1);
            return;
        }
        if(fixYTimer > 0) {
            fixYTimer--;
            player.travel(0,0,-1);
            return;
        }
        EnumFacing direction = direction();
        if(mc.player.rotationPitch < -60) {
            direction = lastDirection;
            mc.player.rotationPitch = 20;
        }
        mc.player.rotationYaw = direction.getHorizontalAngle();
        if(wait) {
            getObby();
            return;
        }
        if(InventoryUtils.getCurrentSlot() == 7)
            selectObby();
        if(stage == 1)
            removeLava();
        else if(stageDone()) {
            nextStage();
        }
        lastDirection = direction;
    }
    
    int i = 9;
    
    private void getObby() {
        if(mc.currentScreen instanceof GuiContainer) {
            Item item = player.inventory.getStackInSlot(i).getItem();
            if(
                    item.getRegistryName().equals(Blocks.NETHERRACK.getRegistryName()) ||
                    item.getRegistryName().equals(Blocks.COBBLESTONE.getRegistryName()) ||
                    item.getRegistryName().equals(Blocks.STONE.getRegistryName())
            ) {
                InventoryUtils.drop(((GuiContainer) mc.currentScreen).inventorySlots.windowId, i + 18); // These numbers are magic, do not question!
            }
            if(++i >= 45) { // These numbers are magic, do not question!
                for (int i = 0 ; i < 27 ; i++) { // These numbers are magic, do not question!
                    InventoryUtils.clickSlot(((GuiContainer) mc.currentScreen).inventorySlots.windowId, i, ClickType.QUICK_MOVE, 0);
                }
                TTCp.player.closeScreen();
                i = 9;
                wait = false;
            }
        }
    }
    
    private void nextStage() {
        getBreak().done = false;
        getFill().done = false;
        switch (++stage) {
            case 16:
                stage = 0;
            case 0:
            case 2:
                getFill().place = false;
                getBreak().doBreak = true;
                breag();
                break;
            case 1:
                getFill().place = true;
                getBreak().doBreak = false;
                InventoryUtils.setCurrentSlot(7);
                break;
            case 3:
            case 6:
                getFill().place = true;
                getBreak().doBreak = false;
                InventoryUtils.setCurrentSlot(7);
                selectObby();
                placeBottom();
                break;
            case 4:
            case 7:
                selectObby();
                InventoryUtils.setCurrentSlot(7);
                placeSide( true);
                break;
            case 5:
            case 8:
                selectObby();
                InventoryUtils.setCurrentSlot(7);
                placeSide(false);
                break;
            case 9:
                player.setSprinting(true);
            case 10:
            case 11:
            case 12:
            case 13:
                player.travel(0,0,1);
                break;
            default:
                player.motionX = 0;
                player.motionY = 0;
                player.motionZ = 0;
                break;
        }
        ChatUtils.chatPrinterDebug().println("Next stage: " + stage);
    }
    
    private void placeSide(boolean first) {
        EnumFacing direction = direction();
        Fill placer = getFill();
        selectObby();
        BlockPos pos = BlockUtils.getRealPos(mc.player.getPositionVector());
        switch (direction) {
            case UP:
            case DOWN:
                pos = null;
                break;
            case EAST:  // X+
                if(first)
                    pos = pos.add( 1,0,-2);
                else
                    pos = pos.add( 1,0, 2);
                break;
            case WEST:  // X-
                if(first)
                    pos = pos.add(-1,0,-2);
                else
                    pos = pos.add(-1,0, 2);
                break;
            case NORTH: // Z-
                if(first)
                    pos = pos.add(-2,0,-1);
                else
                    pos = pos.add( 2,0,-1);
                break;
            case SOUTH: // Z+
                if(first)
                    pos = pos.add(-2,0, 1);
                else
                    pos = pos.add( 2,0, 1);
                break;
        }
        if(pos != null)
            placer.start = placer.end = pos;
    }
    
    private void placeBottom() {
        EnumFacing direction = direction();
        Fill placer = getFill();
        BlockPos pos = BlockUtils.getRealPos(mc.player.getPositionVector());
        switch (direction) {
            case UP:
            case DOWN:
                break;
            case EAST:  // X+
                placer.start = pos.add( 1,-1,-2);
                placer.end   = pos.add( 1,-1, 2);
                break;
            case WEST:  // X-
                placer.start = pos.add(-1,-1,-2);
                placer.end   = pos.add(-1,-1, 2);
                break;
            case NORTH: // Z-
                placer.start = pos.add(-2,-1,-1);
                placer.end   = pos.add( 2,-1,-1);
                break;
            case SOUTH: // Z+
                placer.start = pos.add(-2,-1, 1);
                placer.end   = pos.add( 2,-1, 1);
                break;
        }
    }
    
    private void removeLava() {
        int px = (int)player.posX, py = (int)player.getPositionEyes(1).y, pz = (int)player.posZ;
        for (int iy = 0; iy <= 10; iy++) {
            for (int iz = 0; iz <= 10; iz++) {
                for (int ix = 0; ix <= 10; ix++) {
                    int x = px + ix - 5, y = py + iy - 5, z = pz + iz - 5;
                    BlockPos pos = new BlockPos(x, y, z);
                    IBlockState state = mc.world.getBlockState(pos);
                    if((state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.WATER) && state.getBlock().getMetaFromState(state) == 0) {
                        if(getFill().placeBlockIfPossible(x,y,z)) {
                            return;
                        }
                    }
                }
            }
        }
        stage++;
    }
    
    private void breag() {
        InventoryUtils.setCurrentSlot(2);
        EnumFacing direction = direction();
        Break breaker = getBreak();
        BlockPos pos = BlockUtils.getRealPos(mc.player.getPositionVector());
        switch (direction) {
            case UP:
            case DOWN:
                break;
            case EAST:  // X+
                breaker.start = pos.add( 1,-1,-2);
                breaker.end   = pos.add( 1, 2, 2);
                break;
            case WEST:  // X-
                breaker.start = pos.add(-1,-1,-2);
                breaker.end   = pos.add(-1, 2, 2);
                break;
            case NORTH: // Z-
                breaker.start = pos.add(-2,-1,-1);
                breaker.end   = pos.add( 2, 2,-1);
                break;
            case SOUTH: // Z+
                breaker.start = pos.add(-2,-1, 1);
                breaker.end   = pos.add( 2, 2, 1);
                break;
        }
    }
    
    private boolean stageDone() {
        return ((getBreak().done) || (getFill().done)) && stage != 1;
    }
}
