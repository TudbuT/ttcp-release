package de.tudbut.mod.client.ttcp.mods.combat;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.mods.rendering.Notifications;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.debug.DebugProfiler;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.category.Combat;
import de.tudbut.tools.Lock;
import de.tudbut.tools.ThreadPool;

import java.util.ArrayList;

@Combat
public class LegacyAutoTotem extends Module {

    static LegacyAutoTotem instance;

    public static DebugProfiler profiler = new DebugProfiler("LegacyAutoTotem", "idle");

    // Actual count, set by AI
    public int minCount = 0;
    // Count, set by user
    public int origMinCount = 0;
    // If the user seems to be restocking after respawning, if this is the case,
    // don't switch until any inventories are closed
    public boolean isRestockingAfterRespawn = false;
    // If totems should be stacked automatically
    public boolean autoStack = false;
    // If the AutoStack should always run, regardless of the count
    private boolean autoStackIgnoreCount = false;

    public int delay = 0;

    Lock swapLock = new Lock();
    ThreadPool swapThread = new ThreadPool(1, "Swap thread", true);

    private boolean noTotems = true;

    public LegacyAutoTotem() {
        instance = this;
    }
    
    public static LegacyAutoTotem getInstance() {
        return instance;
    }
    
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createInt(0, 12, "Count", this, "origMinCount"));
        subComponents.add(Setting.createBoolean("AutoStack (WIP)", this, "autoStack"));
        subComponents.add(Setting.createInt(0, 5000, "Delay", this, "delay"));
        subComponents.add(new Button("AutoStack now", it -> {
            autoStackIgnoreCount = true;
            autoStack();
            autoStackIgnoreCount = false;
        }));
        subComponents.add(new Button("Actual count: " + minCount, it -> {
        
        }));
    }
    
    // Run checks and AI
    @Override
    public void onSubTick() {
        if (TTCp.isIngame()) {
            if (!swapLock.isLocked()) {
                EntityPlayerSP player = TTCp.player;
        
        
                profiler.next("RestockCheck");
                if ((isRestockingAfterRespawn() || isRestockingAfterRespawn)) {
                    // Don't switch yet
                    return;
                }
        
                // Run AI
                if (noTotems) {
                    profiler.next("TotCountUpdate");
                    updateTotCount();
                }
                profiler.next("AutoStack");
                if (autoStack)
                    autoStack();
        
                profiler.next("Check");
                ItemStack stack = player.getHeldItemOffhand();
                int minCount = this.minCount;
                if (stack.getCount() <= minCount) {
                    // Switch!
            
                    profiler.next("Switch.GetSlot");
                    Integer slot = InventoryUtils.getSlotWithItem(
                            player.inventoryContainer,
                            Items.TOTEM_OF_UNDYING,
                            new int[] { InventoryUtils.OFFHAND_SLOT },
                            minCount + 1,
                            64
                    );
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
            
                    profiler.next("Switch.Swap");
                    swapLock.lock(2000);
                    swapThread.run(() -> {
                        // Switch a new totem stack to the offhand
                        InventoryUtils.inventorySwap(slot, InventoryUtils.OFFHAND_SLOT, delay, 300, 100);
                        swapLock.lock(1000);
                    });
            
            
                    profiler.next("Switch.Notify");
                    Notifications.add(new Notifications.Notification("Switched to next TotemStack"));
                }
            }
        }
        profiler.next("idle");
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
        
        // Is the player-set count usable?
        if (
                InventoryUtils.getSlotWithItem(
                        player.inventoryContainer,
                        Items.TOTEM_OF_UNDYING,
                        new int[]{InventoryUtils.OFFHAND_SLOT},
                        origMinCount + 1,
                        64
                ) != null
        ) {
            minCount = origMinCount;
            updateBinds();
            return;
        }
        
        // Look for a stack of the AI-set count
        Integer i = InventoryUtils.getSlotWithItem(
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
            updateBinds();
            
            if (minCount < 0) {
                // No stacks left
                minCount = 0;
                updateBinds();
                return; // Sorry
            }
        }
        
        // Found!
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
}
