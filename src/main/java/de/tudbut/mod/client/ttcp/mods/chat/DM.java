package de.tudbut.mod.client.ttcp.mods.chat;

import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Chat;

import java.util.Arrays;

@Chat
public class DM extends Module {
    public static DM instance;
    public String[] users = new String[0];
    
    {
        instance = this;
    }
    
    public static DM getInstance() {
        return instance;
    }
    
    @Override
    public void onSubTick() {
    }
    
    @Override
    public void onChat(String s, String[] args) { }
    
    @Override
    public void onEveryChat(String s, String[] args) {
        users = args;
    }
    
    @Override
    public boolean onServerChat(String s, String formatted) {
        try {
            // See if it is a DM from a DM partner
            String name = Arrays.stream(users).filter(
                    theName ->
                            s.startsWith(theName + " whispers:") ||
                            s.startsWith("~" + theName + " whispers:") ||
                            s.startsWith(theName + " whispers to you:") ||
                            s.startsWith("~" + theName + " whispers to you:") ||
                            s.startsWith("From " + theName + ":") ||
                            s.startsWith("From ~" + theName + ":")
            ).iterator().next();
            if (name != null) {
                ChatUtils.print("§b§lDM from conversation partner: §r<" + name + "> " + s.substring(s.indexOf(": ") + 2));
                // Cancel the display of the default message
                return true;
            }
        }
        catch (Exception ignore) { }
        return false;
    }
    
    @Override
    public int danger() {
        return 1;
    }
}
