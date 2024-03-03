package de.tudbut.mod.client.ttcp.mods.misc;

import net.minecraft.network.Packet;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Misc;
import de.tudbut.parsing.TCN;
import de.tudbut.tools.ConfigSaverTCN2;
import de.tudbut.tools.ThreadPool;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

@Misc
public class PacketLog extends Module {
    
    TCN map = new TCN();
    
    ThreadPool pool = new ThreadPool(5, "PacketLogger thread", false);
    
    @Override
    public boolean onPacket(Packet<?> packet) {
        pool.run(() -> savePacket(packet));
        return false;
    }
    
    @Override
    public void onDisable() {
        pool.run(() -> {
            try(FileOutputStream f = new FileOutputStream("packetlog.tcn")) {
                f.write(map.toString().getBytes());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            map = new TCN();
            ChatUtils.print("PacketLog done!");
        });
    }
    
    private void savePacket(Packet<?> packet) {
        try {
            map.set(new Date().getTime() + " " + packet.getClass().getName(), ConfigSaverTCN2.write(packet, true, false));
        } catch (Throwable e) {
            ChatUtils.print("PacketLog couldn't serialize a packet! Packet was: " + packet.getClass().getName());
        }
    }
}
