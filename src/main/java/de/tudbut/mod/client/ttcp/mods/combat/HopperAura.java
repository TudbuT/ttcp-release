package de.tudbut.mod.client.ttcp.mods.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerHopper;
import net.minecraft.inventory.Slot;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.GuiPlayerSelect;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.mods.command.Friend;
import de.tudbut.mod.client.ttcp.mods.chat.Team;
import de.tudbut.mod.client.ttcp.mods.misc.AltControl;
import de.tudbut.mod.client.ttcp.mods.rendering.PlayerSelector;
import de.tudbut.mod.client.ttcp.mods.rendering.Notifications;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Combat;
import de.tudbut.obj.DoubleTypedObject;
import de.tudbut.obj.Save;
import de.tudbut.parsing.TudSort;
import de.tudbut.tools.Lock;
import de.tudbut.tools.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Combat
public class HopperAura extends Module {
    @Save
    int delay = 300;
    @Save
    int randomDelay = 0;
    @Save
    public int attack = 0;
    @Save
    boolean threadMode = false;
    @Save
    boolean swing = true;
    @Save
    boolean superAttack = false;
    @Save
    boolean batch = false;
    boolean cBatch = false;
    @Save
    boolean tpsSync = false;
    @Save
    int iterations = 1;
    @Save
    int iterationDelay = 0;
    Lock switchTimer = new Lock();
    
    BlockPos currentHopper = null;
    Container hopper = null;
    public static State state = State.WAITING;
    
    public enum State {
        PLACING,
        ATTACKING,
        REPLACING,
        OPENING,
        WAITING,
        IDLE,
        ;
    }
    
    static boolean paused = false;
    static boolean hopperNeedsOpening = false;
    static boolean guiNeedsClosing = false;
    Lock placeLock = new Lock();
    Lock closeLock = new Lock();
    Lock emptyLock = new Lock();
    boolean empty = false;
    ArrayList<BlockPos> validHoppers = new ArrayList<>();
    ArrayList<DoubleTypedObject<BlockPos, Lock>> digging = new ArrayList<>();
    
    public static void pause() {
        paused = true;
        instance.reloadHopper();
    }
    
    public static void resume() {
        paused = false;
        instance.reloadHopper();
    }
    
    public enum SwitchType {
        HOTBAR,
        SWAP,
        ;
    }
    
    public void reloadHopper() {
        if(enabled) {
            if (currentHopper == null) {
                hopperNeedsOpening = createHopper();
                emptyLock.unlock();
                empty = false;
            }
            else if ((!(player.openContainer instanceof ContainerHopper) && !hopperNeedsOpening) || (player.getPositionEyes(0).distanceTo(new Vec3d(currentHopper).add(.5,.5,.5)) >= 4)) {
                if (mc.world.getBlockState(currentHopper).getBlock() == Blocks.HOPPER && player.getPositionEyes(0).distanceTo(new Vec3d(currentHopper).add(.5,.5,.5)) < 4) {
                    ChatUtils.print("§aReopening hopper");
                    hopper = null;
                    openHopper(currentHopper);
                    emptyLock.unlock();
                    empty = false;
                }
                else {
                    ChatUtils.print("§aPrevious hopper unusable, removing");
                    hopper = null;
                    currentHopper = null;
                }
            }
        }
    }
    
    private void openHopper(BlockPos theHopper) {
        state = State.OPENING;
        mc.player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING));
        player.setSneaking(false);
        mc.player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SNEAKING));
        player.setSneaking(false);
        BlockUtils.clickOnBlock(theHopper, EnumHand.MAIN_HAND);
        guiNeedsClosing = true;
        state = State.WAITING;
    }
    
    private BlockPos getBestHopperPos() {
        state = State.PLACING;
        if (Notifications.getNotifications().stream().noneMatch(notification -> notification.text.equals("Trying to place new hopper"))) {
            Notifications.add(new Notifications.Notification("Trying to place new hopper", 1000));
        }
        BlockPos p = BlockUtils.getRealPos(player.getPositionVector());
        ArrayList<BlockPos> possible = new ArrayList<>();
        for (int z = -3 ; z <= 3 ; z++) {
            for (int y = -2 ; y <= 3 ; y++) {
                for (int x = -3 ; x <= 3 ; x++) {
                    int
                            ix = p.getX() + x,
                            iy = p.getY() + y,
                            iz = p.getZ() + z;
                    BlockPos pos = new BlockPos(ix,iy,iz);
                    if(mc.world.getBlockState(pos).getMaterial().isReplaceable() && player.getPositionEyes(0).distanceTo(new Vec3d(pos).add(.5, .5, .5)) < 3) {
                        if(mc.world.getBlockState(pos.up()).getMaterial().isReplaceable() && player.getPositionEyes(0).distanceTo(new Vec3d(pos.up()).add(.5, .5, .5)) < 3) {
                            if(
                                    BlockUtils.getPossibleSides(pos).size() >= 1 &&
                                    mc.world.playerEntities.stream().noneMatch(
                                            entityPlayer ->
                                                    entityPlayer.getEntityBoundingBox().intersects(new AxisAlignedBB(pos)) ||
                                                    entityPlayer.getEntityBoundingBox().intersects(new AxisAlignedBB(pos.up()))
                                    )
                            )
                                possible.add(pos);
                        }
                    }
                }
            }
        }
        if(possible.size() == 0) {
            ChatUtils.print("Can't find a suitable position");
            return null;
        }
        return TudSort.sort(possible.toArray(new BlockPos[0]), blockPos -> {
            List<EntityPlayer> playerEntities = mc.world.playerEntities;
            double d = 0;
            for (int i = 0, playerEntitiesSize = playerEntities.size() ; i < playerEntitiesSize ; i++) {
                EntityPlayer aPlayer = playerEntities.get(i);
                if(aPlayer == player || Arrays.stream(Utils.getAllies()).anyMatch(player -> player == aPlayer))
                    d -= BlockUtils.getRealPos(aPlayer.getPositionVector()).distanceSq(blockPos) / 2;
                else
                    d += BlockUtils.getRealPos(aPlayer.getPositionVector()).distanceSq(blockPos);
            }
            return (long) (d * 100);
        })[0];
    }
    
    private boolean createHopper() {
        state = State.PLACING;
        currentHopper = null;
        if(validHoppers.size() > 0) {
            for (int i = 0 ; i < validHoppers.size() ; i++) {
                BlockPos pos = validHoppers.get(i);
                if (mc.world.getBlockState(pos).getBlock() == Blocks.HOPPER) {
                    if (player.getPositionEyes(0).distanceTo(new Vec3d(pos).add(.5, .5, .5)) < 3) {
                        ChatUtils.print("§aReusing a hopper");
                        placeLock.lock(1000);
                        hopper = null;
                        currentHopper = pos;
                        openHopper(currentHopper);
                        return true;
                    }
                }
                else {
                    validHoppers.remove(i--);
                    /*DoubleTypedObject<BlockPos, Lock> dig;
                    digging.add(dig = new DoubleTypedObject<>(pos, new Lock()));
                    dig.t.lock(20000);
                    player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, EnumFacing.UP));*/
                }
            }
        }
        BlockPos found = getBestHopperPos();
        if(found != null) {
            Integer slot0 = InventoryUtils.getSlotWithItem(player.inventoryContainer, Blocks.HOPPER, new int[0], 1, 64);
            Integer slot1 = InventoryUtils.getSlotWithItem(player.inventoryContainer, Blocks.BLACK_SHULKER_BOX, new int[0], 1, 64);
            if(slot0 != null && slot1 != null) {
                currentHopper = found;
                validHoppers.add(currentHopper);
                
                // Do stuff
                if(hopper != null) {
                    player.connection.sendPacket(new CPacketCloseWindow(hopper.windowId));
                    hopper = null;
                }
                mc.playerController.windowClick(player.inventoryContainer.windowId, slot0, InventoryUtils.getCurrentSlot(), ClickType.SWAP, player);
                BlockUtils.placeBlock(found, EnumHand.MAIN_HAND, true, true);
                BlockUtils.placeBlock(found, EnumHand.MAIN_HAND, true, false);
                mc.player.connection.sendPacket(new CPacketEntityAction(player, CPacketEntityAction.Action.START_SNEAKING));
                player.setSneaking(true);
                mc.playerController.windowClick(player.inventoryContainer.windowId, slot1, InventoryUtils.getCurrentSlot(), ClickType.SWAP, player);
                BlockUtils.placeBlock(found.up(), EnumHand.MAIN_HAND, true, true);
                BlockUtils.placeBlock(found.up(), EnumHand.MAIN_HAND, true, false);
                
                // Revert state
                mc.playerController.windowClick(player.inventoryContainer.windowId, slot1, InventoryUtils.getCurrentSlot(), ClickType.SWAP, player);
                mc.playerController.windowClick(player.inventoryContainer.windowId, slot0, InventoryUtils.getCurrentSlot(), ClickType.SWAP, player);
    
                ChatUtils.print("§aPlaced new hopper");
                placeLock.lock(1000);
                state = State.WAITING;
                return true;
            }
        }
        return false;
    }
    
    Queue<EntityLivingBase> toAttack = new Queue<>();
    public ArrayList<String> targets = new ArrayList<>();
    Lock threadLock = new Lock(true);
    Lock timer = new Lock();
    Thread thread = new Thread(() -> {
        while (true) {
            try {
                threadLock.waitHere();
                timer.waitHere((int) (delay / 6 * Utils.tpsMultiplier()));
                if (enabled)
                    onTick();
                else
                    timer.lock((int) (delay / 2 * Utils.tpsMultiplier()));
            } catch (Exception ignore) { }
        }
    }, "HopperAura"); { thread.start(); }
    
    {
        customKeyBinds.set("select", new KeyBind(null, toString() + "::triggerSelect", false));
    }
    
    @Override
    public void init() {
        PlayerSelector.types.add(new PlayerSelector.Type(player -> {
            targets.clear();
            targets.add(player.getGameProfile().getName());
        }, "Set HopperAura target"));
    }
    
    @SuppressWarnings("unused")
    public void triggerSelect() {
        targets.clear();
        TTCp.mc.displayGuiScreen(
                new GuiPlayerSelect(
                        TTCp.world.playerEntities.stream().filter(
                                player -> !player.getName().equals(TTCp.player.getName())
                        ).toArray(EntityPlayer[]::new),
                        player -> {
                            targets.remove(player.getName());
                            targets.add(player.getName());
                            
                            return false;
                        }
                )
        );
    }
    
    static HopperAura instance;
    {
        instance = this;
    }
    public static HopperAura getInstance() {
        return instance;
    }
    
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new Button("Delay: " + delay, it -> {
            if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                delay -= 25;
            else
                delay += 25;
            
            if(delay < 50)
                delay = 1000;
            if(delay > 1000)
                delay = 50;
            
            it.text = "Delay: " + delay;
        }));
        subComponents.add(new Button("Attack " + (attack == 0 ? "all" : (attack == 1 ? "players" : "targets")), it -> {
            attack++;
            if(attack > 2)
                attack = 0;
            
            it.text = "Attack " + (attack == 0 ? "all" : (attack == 1 ? "players" : "targets"));
        }));
        subComponents.add(Setting.createInt(0, 500, "RandomDelay", this, "randomDelay"));
        subComponents.add(Setting.createBoolean("Thread mode", this, "threadMode"));
        subComponents.add(Setting.createBoolean("Swing", this, "swing"));
        subComponents.add(Setting.createBoolean("Batches", this, "batch"));
        subComponents.add(Setting.createBoolean("TPSSync", this, "tpsSync"));
        subComponents.add(Setting.createInt(1, 10, "Iterations (i/a)", this, "iterations"));
        subComponents.add(Setting.createInt(0, 100, "IterationDelay", this, "iterationDelay"));
    }
    
    @Override
    public void onTick() {
        if(threadMode)
            threadLock.unlock();
        else
            threadLock.lock();
        
        if (threadMode && !(Thread.currentThread() == thread)) {
            return;
        }
    
        for (int i = 0 ; i < digging.size() ; i++) {
            if(!digging.get(i).t.isLocked()) {
                player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, digging.get(i).o, EnumFacing.UP));
                digging.remove(i--);
            }
        }
        
        if(!placeLock.isLocked() && hopperNeedsOpening) {
            currentHopper = null;
            hopper = null;
            hopperNeedsOpening = false;
        }
        
        if(hopperNeedsOpening && currentHopper != null) {
            if(mc.world.getBlockState(currentHopper).getBlock() == Blocks.HOPPER) {
                hopperNeedsOpening = false;
                openHopper(currentHopper);
                state = State.WAITING;
            }
        }
        
        if(Minecraft.getMinecraft().currentScreen instanceof GuiContainer && guiNeedsClosing && currentHopper != null || closeLock.isLocked()) {
            if(!closeLock.isLocked())
                closeLock.lock(500);
            hopper = ((GuiContainer) Minecraft.getMinecraft().currentScreen).inventorySlots;
            Minecraft.getMinecraft().displayGuiScreen(null);
            guiNeedsClosing = false;
        }
        
        {
            if (!toAttack.hasNext()) {
                EntityPlayer[] players = TTCp.world.playerEntities.toArray(new EntityPlayer[0]);
                for (int i = 0 ; i < players.length ; i++) {
                    if (
                            players[i].getDistance(TTCp.player) < 6 &&
                            !Team.getInstance().names.contains(players[i].getGameProfile().getName()) &&
                            !Friend.getInstance().names.contains(players[i].getGameProfile().getName()) &&
                            !players[i].getGameProfile().getName().equals(TTCp.mc.getSession().getProfile().getName()) &&
                            !AltControl.getInstance().isAlt(players[i])
                    ) {
                        if (!targets.isEmpty() || attack == 2) {
                            if (targets.contains(players[i].getGameProfile().getName())) {
                                toAttack.add(players[i]);
                            }
                        }
                        else
                            toAttack.add(players[i]);
                    }
                }
            }
            if (!toAttack.hasNext() && attack == 0) {
                EntityLivingBase[] entities = Utils.getEntities(EntityLivingBase.class, EntityLivingBase::isEntityAlive);
                for (int i = 0 ; i < entities.length ; i++) {
                    if (
                            entities[i].getDistance(TTCp.player) < 6 &&
                            !(entities[i] instanceof EntityPlayer)
                    ) {
                        toAttack.add(entities[i]);
                    }
                }
            }
        }
        
        if(!switchTimer.isLocked()) {
            switchTimer.lock();
        }
        if (!timer.isLocked()) {
            int e = extraDelay();
            switchTimer.lock(delay(e) / 3);
            timer.lock(delay(e));
            if(cBatch = !cBatch && batch) {
                timer.lock(delay(e) * 2);
                switchTimer.lock(delay(e) * 2 / 3);
            }
            
            if(toAttack.hasNext())
                attackNext();
            else
                state = State.IDLE;
        }
    }
    
    private int delay(int e) {
        return (int) (delay + e * (tpsSync ? Utils.tpsMultiplier() : 1));
    }
    
    private int extraDelay() {
        return (int) (randomDelay * Math.random());
    }
    
    private boolean getWeapon() {
        reloadHopper();
        if(currentHopper == null) {
            return false;
        }
        if(hopper == null)
            return false;
        if(player.getHeldItemMainhand().getCount() >= 1)
            return true;
        state = State.REPLACING;
        List<Slot> inventorySlots = hopper.inventorySlots;
        boolean d = false;
        for (int i = 0 ; i < 5 ; i++) {
            Slot slot = inventorySlots.get(i);
            if(slot.getHasStack() && slot.getStack().getCount() >= 1) {
                mc.playerController.windowClick(hopper.windowId, i, 0, ClickType.PICKUP, player);
                mc.playerController.windowClick(hopper.windowId, 32 + InventoryUtils.getCurrentSlot(), 1, ClickType.PICKUP, player);
                mc.playerController.windowClick(hopper.windowId, i, 0, ClickType.PICKUP, player);
                mc.playerController.windowClick(hopper.windowId, -999, 0, ClickType.PICKUP, player);
                d = true;
                break;
            }
        }
        if(!d) {
            if(!empty) {
                empty = true;
                emptyLock.lock(1000);
            }
            if(empty && !emptyLock.isLocked()) {
                currentHopper = null;
                hopper = null;
                validHoppers.remove(currentHopper);
                emptyLock.unlock();
                empty = false;
            }
        }
        else {
            emptyLock.unlock();
            empty = false;
        }
        return d;
    }
    
    public void attackNext() {
        EntityLivingBase entity = toAttack.next();
    
        state = State.ATTACKING;
        if(!superAttack || entity.hurtTime <= 0) {
            for (int i = 0 ; i < iterations ; i++) {
                if(!getWeapon()) {
                    state = State.WAITING;
                    continue;
                }
                BlockUtils.lookAt(entity.getPositionVector().add(0, (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) / 2, 0));
                TTCp.mc.playerController.attackEntity(TTCp.player, entity);
                if (swing)
                    TTCp.player.swingArm(EnumHand.MAIN_HAND);
                
                if(iterations > 1) {
                    try {
                        Thread.sleep(iterationDelay);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
    
    @Override
    public void onConfigLoad() {
        updateBinds();
    }
    
    @Override
    public int danger() {
        return 4;
    }
}
