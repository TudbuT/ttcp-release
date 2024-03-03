package de.tudbut.mod.client.ttcp.mods.chat;

import net.minecraft.client.network.NetworkPlayerInfo;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.ThreadManager;
import de.tudbut.mod.client.ttcp.utils.category.Chat;

import java.util.Objects;

@Chat
public class DMAll extends Module {
    {
        enabled = true;
    }
    
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public void onSubTick() {
    }
    
    @Override
    public void onEverySubTick() {
        enabled = true;
    }
    
    @Override
    public void onChat(String s, String[] args) {
        ChatUtils.print("Sending...");
        
        // This would stop the game if it wasn't in a separate thread
        ThreadManager.run(() -> {
            // Loop through all players
            for (NetworkPlayerInfo info : Objects.requireNonNull(TTCp.mc.getConnection()).getPlayerInfoMap().toArray(new NetworkPlayerInfo[0])) {
                try {
                    // Send a DM to the player
                    TTCp.mc.player.sendChatMessage("/tell " + info.getGameProfile().getName() + " " + s);
                    // Notify the user
                    ChatUtils.print("Sent to " + info.getGameProfile().getName());
                    // I hate antispam
                    Thread.sleep(TPATools.getInstance().delay);
                }
                catch (Throwable ignore) { }
            }
            ChatUtils.print("Done!");
        });
    }
}
