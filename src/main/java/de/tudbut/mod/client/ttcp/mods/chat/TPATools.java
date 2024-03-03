package de.tudbut.mod.client.ttcp.mods.chat;

import net.minecraft.client.network.NetworkPlayerInfo;
import org.lwjgl.input.Keyboard;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.ThreadManager;
import de.tudbut.mod.client.ttcp.utils.category.Chat;
import de.tudbut.obj.Save;

import java.util.Objects;

@Chat
public class TPATools extends Module {
    static TPATools instance;
    // I hate antispam
    @Save
    public int delay = 1000;
    private boolean stop = false;
    
    public TPATools() {
        instance = this;
    }
    
    public static TPATools getInstance() {
        return instance;
    }
    
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new Button("Send /tpa to everyone", text -> onChat("tpa", null)));
        subComponents.add(new Button("Send /tpahere to everyone", text -> onChat("tpahere", null)));
        subComponents.add(new Button("Delay: " + delay, it -> {
            // I hate antispam
        
        
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                delay -= 1000;
            else
                delay += 1000;
        
            if (delay > 5000)
                delay = 1000;
            if (delay < 1000)
                delay = 5000;
            it.text = "Delay: " + delay;
        }));
        subComponents.add(new Button("Stop", it -> {
            stop = true;
            TTCp.player.sendChatMessage("/tpacancel");
        
            ThreadManager.run(() -> {
                it.text = "Done";
                try {
                    Thread.sleep(2000 + delay);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stop = false;
                it.text = "Stop";
            });
        }));
    }
    
    @Override
    public void onDisable() {
    }
    
    @Override
    public void onEnable() {
    }
    
    @Override
    public void onSubTick() {
    
    }
    
    @Override
    public void onEverySubTick() { }
    
    @Override
    public void onChat(String s, String[] args) {
        if (s.equalsIgnoreCase("delay")) {
            // I hate antispam
            delay = Integer.parseInt(args[1]);
            ChatUtils.print("Set!");
        }
        
        if (s.equalsIgnoreCase("tpa")) {
            ChatUtils.print("Sending...");
            // This would stop the game if it wasn't in a separate thread
            ThreadManager.run(() -> {
                // Loop through all players
                for (NetworkPlayerInfo info : Objects.requireNonNull(TTCp.mc.getConnection()).getPlayerInfoMap().toArray(new NetworkPlayerInfo[0])) {
                    if (stop)
                        return;
                    try {
                        // Send /tpa <player>
                        TTCp.mc.player.sendChatMessage("/tpa " + info.getGameProfile().getName());
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
        if (s.equalsIgnoreCase("tpahere")) {
            ChatUtils.print("Sending...");
            // This would stop the game if it wasn't in a separate thread
            ThreadManager.run(() -> {
                // Loop through all players
                for (NetworkPlayerInfo info : Objects.requireNonNull(TTCp.mc.getConnection()).getPlayerInfoMap().toArray(new NetworkPlayerInfo[0])) {
                    if (stop)
                        return;
                    try {
                        // Send /tpahere <player>
                        TTCp.mc.player.sendChatMessage("/tpahere " + info.getGameProfile().getName());
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
        updateBinds();
    }
    
    public void onConfigLoad() {
        updateBinds();
    }
}
