package de.tudbut.mod.client.ttcp.mods.command;

import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Chat;
import de.tudbut.mod.client.ttcp.utils.category.Command;

@Command
public class Prefix extends Module {
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
        // Set the prefix
        TTCp.prefix = s;
    }
}
