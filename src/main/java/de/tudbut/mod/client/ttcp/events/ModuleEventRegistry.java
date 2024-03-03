package de.tudbut.mod.client.ttcp.events;

import de.tudbut.mod.client.ttcp.utils.Module;

import java.util.ArrayList;

public class ModuleEventRegistry {
    
    public static ArrayList<Module> disableOnNewPlayer = new ArrayList<>();
    
    static void onNewPlayer() {
        for (Module module : disableOnNewPlayer) {
            if(module.enabled) {
                module.toggle();
            }
        }
    }
}
