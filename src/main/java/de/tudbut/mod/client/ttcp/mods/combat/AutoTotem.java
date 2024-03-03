package de.tudbut.mod.client.ttcp.mods.combat;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.events.EventHandler;
import de.tudbut.mod.client.ttcp.gui.GuiTTCIngame;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.mods.rendering.Notifications;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Combat;
import de.tudbut.obj.Save;
import de.tudbut.tools.Lock;
import de.tudbut.tools.ThreadPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@Combat
public class AutoTotem extends Module {
    
    public static AutoTotem instance;
    
    public static DebugProfilerAdapter profiler = new DebugProfilerAdapter("AutoTotem", "idle");
    static {
        TTCp.registerProfiler(profiler);
    }
    
    // Actual count, set by AI
    public int minCount = 0;
    // PrepCount, set by user
    @Save
    public int prepCount = 2;
    // Count, set by user
    @Save
    public int origMinCount = 0;
    // If the user seems to be restocking after respawning, if this is the case,
    // don't switch until any inventories are closed
    public boolean isRestockingAfterRespawn = false;
    // If totems should be stacked automatically
    @Save
    public boolean autoStack = false;
    // If the AutoStack should always run, regardless of the count
    private boolean autoStackIgnoreCount = false;
    // Use pop prediction
    @Save
    public boolean pingPredict = false;
    @Save
    public int sdelay = 0, pdelay = 0, cdelay = 0, ldelay = 500;
    @Save
    public boolean legacy = true;
    
    public int swapProgress = 0;
    public int countSwapped = 0;
    public int countSwappedAt;
    public long lastSwap = 0;
    
    public int fullCount = 0;
    
    public boolean[] slotsUsed = new boolean[45];
    public int[] slotsUsedAtCounts = new int[45];
    public long[] slotsUsedAtTime = new long[45];
    {
        Arrays.fill(slotsUsedAtCounts, Integer.MAX_VALUE);
        Arrays.fill(slotsUsedAtCounts, 0);
    }
    
    Lock swapLock = new Lock();
    ThreadPool swapThread = new ThreadPool(1, "Swap thread", true);
    
    // Panic mode, switch to totems instantly!
    public boolean panic = false;
    
    private boolean noTotems = true;
    
    public void renderTotems() {
        if(fullCount != 0) {
            ScaledResolution res = new ScaledResolution(mc);
            int y = res.getScaledHeight() - 16 * 2 - 3 - 8;
            int x;
            if (player.getPrimaryHand() != EnumHandSide.LEFT)
                x = res.getScaledWidth() / 2 - 91 - 26;
            else
                x = res.getScaledWidth() / 2 + 91 + 10;
            
            GuiTTCIngame.drawOffhandSlot(x-3,y-3);
            GuiTTCIngame.drawItem(x, y, 1, player, new ItemStack(Items.TOTEM_OF_UNDYING, fullCount));
        }
    }
    
    public int getTotemCount() {
        return InventoryUtils.getItemAmount(player.inventoryContainer, Items.TOTEM_OF_UNDYING);
    }
    
    public void panic() {
        // Enable panic
        panic = true;
        // Tick panic
        doSwitch(true);
        // Disable panic
        panic = false;
    }
    
    public AutoTotem() {
        instance = this;
    }
    
    public static AutoTotem getInstance() {
        return instance;
    }
    
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createInt(0, 12, "PrepCount", this, "prepCount"));
        subComponents.add(Setting.createInt(0, 12, "Count", this, "origMinCount", () -> {
            updateTotCount();
            updateBinds();
        }));
        subComponents.add(Setting.createBoolean("AutoStack (WIP)", this, "autoStack"));
        subComponents.add(Setting.createBoolean("PingPredict", this, "pingPredict"));
        subComponents.add(Setting.createInt(0, 500, "SwitchDelay", this, "sdelay"));
        subComponents.add(Setting.createInt(0, 500, "PostDelay", this, "pdelay"));
        subComponents.add(Setting.createInt(0, 5000, "CooldownDelay", this, "cdelay"));
        subComponents.add(Setting.createInt(0, 1000, "LockDelay", this, "ldelay"));
        subComponents.add(Setting.createBoolean("Fast mode", this, "legacy"));
        subComponents.add(new Button("AutoStack now", it -> {
            autoStackIgnoreCount = true;
            autoStack();
            autoStackIgnoreCount = false;
        }));
        subComponents.add(new Button("Actual count: " + minCount, it -> { }));
        customKeyBinds.setIfNull("panic", new KeyBind(null, this + "::panic", true));
        subComponents.add(Setting.createKey("Panic", customKeyBinds.get("panic")));
    }
    
    public void doSwitch(boolean takeMax) {
        // Switch!

        KillAura.getInstance().noSwitch = true;
        ItemStack stack = player.inventory.getStackInSlot(InventoryUtils.OFFHAND_SLOT);
    
        profiler.next("Switch.GetSlot");
        Integer slot = null;
        if(takeMax) {
            int biggestCount = 0;
            int slotNum = -1;
            for (int i = 0 ; i < 5 * 9 ; i++) {
                ItemStack itemStack = player.inventoryContainer.getInventory().get(i);
                if(itemStack.getItem().equals(Items.TOTEM_OF_UNDYING)) {
                    if(itemStack.getCount() > biggestCount) {
                        slotNum = i;
                        biggestCount = itemStack.getCount();
                    }
                }
            }
            if(slotNum != -1 && slotNum != InventoryUtils.OFFHAND_SLOT)
                slot = slotNum;
        }
        else {
            slot = InventoryUtils.getSlotWithItem(
                    player.inventoryContainer,
                    Items.TOTEM_OF_UNDYING,
                    getUnusableSlots(),
                    minCount + 1,
                    64
            );
        }
        if (slot == null) {
            profiler.next("Switch.NotifyEmpty");
            if (!noTotems)
                Notifications.add(new Notifications.Notification("No more totems! Couldn't switch!"));
            noTotems = true;
            profiler.next("idle");
            return; // Oh no!! No totems left!
        }
        else
            noTotems = false;
    
        slotsUsed[slot] = true;
    
        swapLock.lock(2000);
        int finalSlot = slot;
        swapThread.run(() -> {
            if(legacy) {
                profiler.next("Switch.Notify");
                Notifications.add(new Notifications.Notification("Switching to next TotemStack..."));
                // Switch a new totem stack to the offhand
                profiler.next("Switch.Swap");
                InventoryUtils.inventorySwap(finalSlot, InventoryUtils.OFFHAND_SLOT, sdelay, pdelay, cdelay);
                swapLock.lock(ldelay);
                profiler.next("Switch.Notify");
                Notifications.add(new Notifications.Notification("Switched to next TotemStack"));
            }
            else {
                swapProgress = 1;
                countSwappedAt = stack.getCount();
                countSwapped = TTCp.player.inventory.getStackInSlot(finalSlot).getCount();
                slotsUsedAtTime[finalSlot] = System.currentTimeMillis();
                lastSwap = System.currentTimeMillis();
                profiler.next("Switch.Notify");
                Notifications.add(new Notifications.Notification("Switching to next TotemStack..."));
                // Switch a new totem stack to the offhand
                profiler.next("Switch.Swap");
                InventoryUtils.inventorySwap(finalSlot, InventoryUtils.OFFHAND_SLOT, sdelay, pdelay, cdelay);
                swapLock.lock(ldelay);
                profiler.next("Switch.Notify");
                Notifications.add(new Notifications.Notification("Switched to next TotemStack"));
                swapProgress = 2;
            }
            KillAura.getInstance().noSwitch = false;
        });
    }

    @Override
    public void onEverySubTick() {
        if (TTCp.isIngame()) {
            fullCount = getTotemCount();
        }
    }
    
    @Override
    public void onTick() {
        if(TTCp.isIngame()) {
            if (noTotems || minCount != origMinCount)
                updateTotCount();
        }
    }
    
    // Run checks and AI
    @Override
    public void onSubTick() {
        if (TTCp.isIngame()) {
            
            if (!swapLock.isLocked() || panic) {
                EntityPlayerSP player = TTCp.player;
                
                
                profiler.next("RestockCheck");
                if ((isRestockingAfterRespawn() || isRestockingAfterRespawn)) {
                    // Don't switch yet
                    return;
                }
                
                // Run AI
                if (noTotems || minCount != origMinCount) {
                    profiler.next("TotCountUpdate");
                    reindex();
                    swapLock.unlock();
                    swapProgress = 0;
                }
                profiler.next("AutoStack");
                if (autoStack)
                    autoStack();
                
                profiler.next("Check");
                ItemStack stack = player.getHeldItemOffhand();
                if(!legacy) {
                    if (swapProgress == 2 || lastSwap <= System.currentTimeMillis() - 2000) {
                        PopCount.Counter counter = TTCp.getModule(PopCount.class).counters.get(TTCp.player);
                        if (stack.getCount() < countSwapped && stack.getCount() > Math.max(countSwappedAt, minCount) || lastSwap <= System.currentTimeMillis() - 2000) {
                            swapProgress = 0;
                            reindex();
                        }
                    }
                }
                KillAura.getInstance().noSwitch = stack.getCount() <= prepCount + minCount;
                if (
                        (
                                panic ||
                                (stack.getCount() <= minCount) ||
                                (checkPingPop() && (stack.getCount() <= (minCount + 1)))
                        ) && (swapProgress == 0 || legacy)
                ) {
                    doSwitch(false);
                }
            }
        }
        profiler.next("idle");
    }
    
    public void reindex() {
        // TODO: This is stupid, i need to remake this!

        for (int i = 0 ; i < slotsUsed.length ; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            slotsUsedAtCounts[i] = Math.min(stack.getCount(), slotsUsedAtCounts[i]);
            if(stack.getItem() == Items.TOTEM_OF_UNDYING && stack.getCount() > slotsUsedAtCounts[i] && slotsUsed[i] && slotsUsedAtTime[i] < System.currentTimeMillis() - 5000) {
                slotsUsedAtCounts[i] = stack.getCount();
                slotsUsed[i] = false;
            }
        }
    }
    
    private int[] getUnusableSlots() {
        reindex();
        ArrayList<Integer> slots = new ArrayList<>(Collections.singletonList(InventoryUtils.OFFHAND_SLOT));
    
        for (int i = 0 ; i < slotsUsed.length ; i++) {
            if(slotsUsed[i])
                slots.add(i);
        }
        
        int[] theSlots = new int[slots.size()];
        for (int i = 0 ; i < theSlots.length ; i++) {
            theSlots[i] = slots.get(i);
        }
        return theSlots;
    }
    
    private boolean checkPingPop() {
        if(!pingPredict || minCount == 0)
            return false;
        PopCount popCount = TTCp.getModule(PopCount.class);
        PopCount.Counter counter = popCount.counters.get(TTCp.player);
        if(counter != null && counter.isPopping()) {
            long d = counter.predictNextPopDelay();
            return d <= Math.max(EventHandler.ping[0], 0);
        }
        return false;
    }
    
    // Tests if the player is likely to be restocking after having a empty inventory,
    // does NOT check for a respawn, but very likely will only be true after a respawn!
    public boolean isRestockingAfterRespawn() {
        EntityPlayerSP player = TTCp.player;
        
        // Set false if the container was closed, this will make it start switching again
        GuiScreen screen = TTCp.mc.currentScreen;
        if (
                !(
                        screen instanceof GuiContainer && !(
                                screen instanceof GuiInventory ||
                                screen instanceof GuiContainerCreative
                        )
                )
        ) {
            isRestockingAfterRespawn = false;
            return false;
        }
        
        // Any slot with totems
        Integer slot0 = InventoryUtils.getSlotWithItem(
                player.inventoryContainer,
                Items.TOTEM_OF_UNDYING,
                new int[]{},
                1,
                64
        );
        // No totems, return true
        if (slot0 == null) {
            isRestockingAfterRespawn = true;
            return true;
        }
        // Any slot with totems excluding slot0
        Integer slot1 = InventoryUtils.getSlotWithItem(
                player.inventoryContainer,
                Items.TOTEM_OF_UNDYING,
                new int[]{slot0},
                1,
                64
        );
        // Only one stack of totems, return true
        if (slot1 == null) {
            isRestockingAfterRespawn = true;
            return true;
        }
        
        // There is two or more stacks, return false, seems normal
        return false;
    }
    
    // AI, finds out the amount to switch at, looks for lowest amount of totems in inventory
    public void updateTotCount() {
        EntityPlayerSP player = TTCp.player;
        Integer i;
        
        slotsUsed = new boolean[45];
        slotsUsedAtCounts = new int[45];
        slotsUsedAtTime = new long[45];
        
        int x = minCount;
        minCount = origMinCount;
        
        // Look for a stack of the AI-set count
        i = InventoryUtils.getSlotWithItem(
                player.inventoryContainer,
                Items.TOTEM_OF_UNDYING,
                new int[]{InventoryUtils.OFFHAND_SLOT},
                minCount + 1,
                64
        );
        // If it doesnt exist, step down the count until a stack exist or the count hits 0
        while (i == null) {
            // Step down
            minCount--;
            // Check
            i = InventoryUtils.getSlotWithItem(
                    player.inventoryContainer,
                    Items.TOTEM_OF_UNDYING,
                    new int[]{InventoryUtils.OFFHAND_SLOT},
                    minCount + 1,
                    64
            );
            
            if (minCount < 0) {
                // No stacks left
                minCount = 0;
                break;
            }
        }
        
        
        // Found!
        if(minCount != x)
            updateBinds();
    }
    
    public void autoStack() {
        
        if(minCount == 0)
            return;
        
        EntityPlayerSP player = TTCp.player;
        ArrayList<Integer> slots = new ArrayList<>();
        // The minimal amount that is required to stack totems
        int min = 2;
        // Only restack when totems are likely not a normal stack
        int max = 24;
        // TMP variable
        Integer slot;
        
        // Runs 50 times
        for (int i = 0; i < 50; i++) {
            
            // Drop unusable stacks
            ArrayList<Integer> dropped = new ArrayList<>();
            if (slots.size() != 0) {
                
                for (int j = 0; j < 100; j++) {
                    // Next
                    slot = InventoryUtils.getSlotWithItem(
                            player.inventoryContainer,
                            Items.TOTEM_OF_UNDYING,
                            Utils.objectArrayToNativeArray(dropped.toArray(new Integer[0])),
                            0,
                            min - 1
                    );
                    
                    if (slot == null)
                        break;
                    
                    // Drop stack contents of the slot
                    InventoryUtils.drop(slot);
                    System.out.println("Dropped item in " + slot);
                    dropped.add(slot);
                }
                
            }
            
            if(origMinCount == minCount && !autoStackIgnoreCount)
                return;
            
            
            // Get slots with totems
            slots.clear();
            for (int j = 0; j < 100; j++) {
                slot = InventoryUtils.getSlotWithItem(
                        player.inventoryContainer,
                        Items.TOTEM_OF_UNDYING,
                        Utils.objectArrayToNativeArray(slots.toArray(new Integer[0])),
                        min,
                        max
                );
                if(slot == null)
                    break;
                
                slots.add(slot);
            }
            
            // Combine totems
            while (slots.size() >= 2) {
                // Get empty slot
                slot = InventoryUtils.getSlotWithItem(player.inventoryContainer, Items.AIR, 0);
                if (slot == null) {
                    InventoryUtils.drop(slots.get(0));
                    slots.remove(0);
                    continue;
                }
                System.out.println("Combining " + slots.get(0) + " and " + slots.get(1) + " to " + slot);
                // Pick first stack
                InventoryUtils.clickSlot(slots.get(0), ClickType.PICKUP, 0);
                // Pick second stack
                InventoryUtils.clickSlot(slots.get(1), ClickType.PICKUP, 0);
                // Put result in empty slot
                InventoryUtils.clickSlot(slot, ClickType.PICKUP, 0);
                // Drop junk
                InventoryUtils.drop(slots.get(1));
                
                slots.remove(0);
                slots.remove(0);
            }
        }
    }
    
    @Override
    public void onChat(String s, String[] args) {
        if (s.startsWith("count "))
            try {
                origMinCount = minCount = Integer.parseInt(s.substring("count ".length()));
                ChatUtils.print("Set!");
            }
            catch (Exception e) {
                ChatUtils.print("ERROR: NaN");
            }
        if(s.startsWith("debug"))
            ChatUtils.print(profiler.getTempResults().toString());
        updateBinds();
    }
    
    @Override
    public void onConfigLoad() {
        updateBinds();
    }
}
