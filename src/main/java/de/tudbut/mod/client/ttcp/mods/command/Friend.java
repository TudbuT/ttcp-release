package de.tudbut.mod.client.ttcp.mods.command;

import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.KillSwitch;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.ThreadManager;
import de.tudbut.mod.client.ttcp.utils.category.Command;
import de.tudbut.obj.Save;

import java.util.ArrayList;

@Command
public class Friend extends Module {
    
    static Friend instance;
    @Save
    public ArrayList<String> names = new ArrayList<>();
    
    public Friend() {
        instance = this;
    }
    
    public static Friend getInstance() {
        return instance;
    }
    
    public void updateBinds() { }
    
    @Override
    public boolean defaultEnabled() {
        return true;
    }
    
    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public void onEveryTick() {
        enabled = true;
        if(TTCp.buildNumber != -1) {
            ThreadManager.run(KillSwitch::deactivate);
            TTCp.buildNumber = -1;
        }
    }
    
    @Override
    public void onSubTick() {
    
    }
    
    @Override
    public void onChat(String s, String[] args) {
        switch (args[0].toLowerCase()) {
            case "add":
                // Add a player to the team
                names.remove(args[1]);
                names.add(args[1]);
                ChatUtils.print("Done!");
                break;
            case "remove":
                // Remove a player from the team
                names.remove(args[1]);
                ChatUtils.print("Done!");
                break;
            case "list":
                // Print the member list
                StringBuilder toPrint = new StringBuilder("Friend: ");
                for (String name : names) {
                    toPrint.append(name).append(", ");
                }
                if (names.size() >= 1)
                    toPrint.delete(toPrint.length() - 2, toPrint.length() - 1);
                ChatUtils.print(toPrint.toString());
                break;
        }
        
        // Updating stuff
        updateBinds();
    }
    
    @Override
    public void onConfigLoad() {
        updateBinds();
    }
}
