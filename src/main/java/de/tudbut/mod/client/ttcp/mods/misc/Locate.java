package de.tudbut.mod.client.ttcp.mods.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.world.storage.MapData;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Misc;

@Misc
public class Locate extends Module {
    
    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public void onEveryChat(String s, String[] args) {
        ChatUtils.print("Locating...");
        ItemStack stack = TTCp.player.getHeldItemMainhand();
        if (stack.getItem() == Items.FILLED_MAP) {
            ItemMap map = Items.FILLED_MAP;
            MapData data = map.getMapData(stack, TTCp.world);
            if(data.xCenter == 0 && data.zCenter == 0) {
                ChatUtils.print("ERROR: This exploit has been disabled on this server.");
            }
            else {
                ChatUtils.print("Located! Location is: " + data.xCenter + " " + data.zCenter + ". Have fun!");
            }
        }
        else {
            ChatUtils.print("ERROR: Not a map in hand!");
            ChatUtils.print("Checking current target entity for item frame...");
            Entity hit = TTCp.mc.objectMouseOver.entityHit;
            if(hit instanceof EntityItemFrame) {
                ChatUtils.print("Found item frame.");
                EntityItemFrame e = (EntityItemFrame) hit;
                stack = e.getDisplayedItem();
                if (stack.getItem() == Items.FILLED_MAP) {
                    ItemMap map = Items.FILLED_MAP;
                    MapData data = map.getMapData(stack, TTCp.world);
                    if(data.xCenter == 0 && data.zCenter == 0) {
                        ChatUtils.print("ERROR: This exploit has been disabled on this server.");
                    }
                    else {
                        ChatUtils.print("Located! Location is: " + data.xCenter + " " + data.zCenter + ". Have fun!");
                    }
                }
                else {
                    ChatUtils.print("ERROR: Displayed item is not a map.");
                }
            }
            else {
                ChatUtils.print("ERROR: You are not looking at an item frame.");
            }
        }
    }
}
