package de.tudbut.mod.client.ttcp.mods.command;

import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Command;

import java.io.File;
import java.io.IOException;

import static de.tudbut.mod.client.ttcp.TTCp.modules;

@Command
public class Cfg extends Module {
    String cfg = "main";
    
    @Override
    public boolean defaultEnabled() {
        return true;
    }
    
    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public void onChat(String s, String[] args) {
        if(args.length == 2) {
            if (args[0].equals("use")) {
                if (new File("config/ttc/" + args[1] + ".cfg").exists() || new File("config/ttc/" + args[1] + ".tcnmap").exists()) {
                    ChatUtils.print("Loading config " + args[1]);
                    try {
                        TTCp.getInstance().setConfig(args[1]);
                        cfg = args[1];
                        for (int i = 0 ; i < modules.length ; i++) {
                            modules[i].init();
                            modules[i].updateBindsFull();
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    ChatUtils.print("Done!");
                }
                else {
                    ChatUtils.print("That config doesn't exist, try `cfg save " + args[1] + "`!");
                }
            }
            if (args[0].equals("save")) {
                ChatUtils.print("Saving to " + args[1]);
                try {
                    TTCp.getInstance().saveConfig(args[1]);
                    cfg = args[1];
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                ChatUtils.print("Done!");
            }
        } else {
            ChatUtils.print("Current: " + cfg);
        }
    }
}
