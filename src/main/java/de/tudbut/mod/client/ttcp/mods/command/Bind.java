package de.tudbut.mod.client.ttcp.mods.command;

import org.lwjgl.input.Keyboard;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Command;

@Command
public class Bind extends Module {
    @Override
    public boolean defaultEnabled() {
        return true;
    }
    
    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public void onSubTick() {
    
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
    
    @Override
    public void onEveryChat(String s, String[] args) {
        if(s.equals("help")) {
            
            ChatUtils.print("§a§lBinds");
            for (int i = 0; i < TTCp.modules.length; i++) {
                ChatUtils.print("§aModule: " + TTCp.modules[i].toString());
                if(TTCp.modules[i].key.key != null)
                    ChatUtils.print("State: " + Keyboard.getKeyName(TTCp.modules[i].key.key));
                for (String kb : TTCp.modules[i].customKeyBinds.keys()) {
                    if(TTCp.modules[i].customKeyBinds.get(kb).key != null)
                        ChatUtils.print("Function " + kb + ": " + Keyboard.getKeyName(TTCp.modules[i].customKeyBinds.get(kb).key));
                    else
                        ChatUtils.print("Function " + kb);
                }
            }
            
            return;
        }
        
        for (int i = 0; i < TTCp.modules.length; i++) {
            if (args[0].equalsIgnoreCase(TTCp.modules[i].toString().toLowerCase())) {
                if(args.length == 2) {
                    int key = Keyboard.getKeyIndex(args[1].toUpperCase());
                    if(key == Keyboard.KEY_NONE) {
                        TTCp.modules[i].customKeyBinds.get(args[1]).key = null;
                    }
                    else
                        TTCp.modules[i].key.key = key;
                }
                else if (args.length == 3) {
                    if (TTCp.modules[i].customKeyBinds.keys().contains(args[1])) {
                        TTCp.modules[i].customKeyBinds.get(args[1]).key = Keyboard.getKeyIndex(args[2].toUpperCase());
                    }
                    else {
                        ChatUtils.print("Function not found");
                    }
                }
                else
                    TTCp.modules[i].key.key = null;
            }
        }
    }
}
