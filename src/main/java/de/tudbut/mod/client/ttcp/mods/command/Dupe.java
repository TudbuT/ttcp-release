package de.tudbut.mod.client.ttcp.mods.command;

import de.tudbut.timer.AsyncTask;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.*;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.InventoryUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Command;

import java.io.IOException;
import java.io.InputStream;

@Command
public class Dupe extends Module {
    
    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public void onEveryChat(String s, String[] args) {
        if (args.length == 0) {
            args = new String[]{ "8b8t" };
        }
        switch (args[0]) {
            case "Pkick1":
                ChatUtils.simulateSend("§", false);
                break;
            case "Pkick2":
                TTCp.player.connection.sendPacket(new CPacketUseEntity(TTCp.player));
                break;
            case "Pkick3":
                TTCp.player.connection.sendPacket(new Packet<INetHandler>() {
                    @Override
                    public void readPacketData(PacketBuffer buf) throws IOException {
                    
                    }
                
                    @Override
                    public void writePacketData(PacketBuffer buf) throws IOException {
                        buf.writeBytes(new InputStream() {
                            @Override
                            public int read() throws IOException {
                                return (int) ( Math.random() * 255 );
                            }
                        }, Integer.MAX_VALUE);
                    }
                
                    @Override
                    public void processPacket(INetHandler handler) {
                    
                    }
                });
                break;
            case "8b8t":
                ChatUtils.print("Please wait...");
                int i = InventoryUtils.getCurrentSlot();
                Integer twood = null;
                for(int n = 0; n < 64 && twood == null; n++) {
                    twood = InventoryUtils.getSlotWithItem(player.inventoryContainer, Blocks.PLANKS, 64 - n);
                }
                if(twood == null) {
                    ChatUtils.print("Error: No planks!");
                    break;
                }
                int wood = twood;
                new AsyncTask<>(() -> {
                    InventoryUtils.clickSlot(wood, ClickType.PICKUP, 0);
                    float r = player.rotationPitch;
                    player.rotationPitch = 90;
                    player.connection.sendPacket(new CPacketPlayer.Rotation(player.rotationYaw, player.rotationPitch, true));
                    Thread.sleep(200);
                    InventoryUtils.clickSlot(-999, ClickType.QUICK_CRAFT, 0);
                    InventoryUtils.clickSlot(2, ClickType.QUICK_CRAFT, 1);
                    Thread.sleep(100);
                    InventoryUtils.clickSlot(4, ClickType.QUICK_CRAFT, 1);
                    Thread.sleep(200);
                    InventoryUtils.clickSlot(-999, ClickType.QUICK_CRAFT, 2);
                    Thread.sleep(700);
                    InventoryUtils.clickSlot(i + 36, ClickType.THROW, 1);
                    Thread.sleep(700);
                    InventoryUtils.clickSlot(2, ClickType.QUICK_MOVE, 0);
                    Thread.sleep(400);
                    InventoryUtils.clickSlot(4, ClickType.QUICK_MOVE, 0);
                    while(player.inventory.getCurrentItem().isEmpty()) { Thread.sleep(5); }
                    Thread.sleep(50);
                    player.rotationPitch = r;
                    player.connection.sendPacket(new CPacketPlayer.Rotation(player.rotationYaw, player.rotationPitch, true));
                    return null;
                });
                break;
        }
    
    }
}
