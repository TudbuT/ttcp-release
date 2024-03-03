package de.tudbut.mod.client.ttcp.mods.misc;

import org.lwjgl.input.Keyboard;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.mods.chat.TPAParty;
import de.tudbut.mod.client.ttcp.mods.chat.TPATools;
import de.tudbut.mod.client.ttcp.mods.chat.Team;
import de.tudbut.mod.client.ttcp.mods.combat.AutoTotem;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.ThreadManager;
import de.tudbut.mod.client.ttcp.utils.category.Misc;
import de.tudbut.obj.Save;

@Misc
public class AutoConfig extends Module {
    
    // true for Server mode, false for Custom mode
    @Save
    private boolean mode = false;
    
    // Settings for Custom mode
    @Save
    private boolean stackedTots = false;
    @Save
    private boolean pvp = false;
    @Save
    private boolean tpa = false;
    
    // Settings for Server mode
    private Server server = Server._8b8t;
    
    @Override
    public void onEnable() {
        updateBinds();
    }
    
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new Button("Mode: " + (mode ? "Server" : "Custom"), it -> {
            mode = !mode;
            it.text = "Mode: " + (mode ? "Server" : "Custom");
            updateBinds();
        }));
        if (mode) {
            subComponents.add(new Button("Server: " + server.name, it -> {
                int i = server.ordinal();
                
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                    i--;
                else
                    i++;
                
                if (i >= Server.values().length)
                    i = 0;
                if (i < 0)
                    i = Server.values().length - 1;
                
                server = Server.values()[i];
                
                it.text = "Server: " + server.name;
            }));
        } else {
            subComponents.add(new Button("Has stacked totems: " + stackedTots, it -> {
                stackedTots = !stackedTots;
                it.text = "Has stacked totems: " + stackedTots;
            }));
            subComponents.add(new Button("PvP meta: " + (pvp ? "32k" : "Crystal"), it -> {
                pvp = !pvp;
                it.text = "PvP meta: " + (pvp ? "32k" : "Crystal");
            }));
            subComponents.add(new Button("Has /tpa: " + tpa, it -> {
                tpa = !tpa;
                it.text = "Has /tpa: " + tpa;
            }));
        }
        subComponents.add(new Button("Set", it -> {
            // Confirm set
            if (mode) {
                stackedTots = server.stackedTots;
                pvp = server.pvp;
                tpa = server.tpa;
            }
            int i = 0;
            if (stackedTots) {
                i += (pvp ? 4 : 2);
            }
            AutoTotem.getInstance().origMinCount = i;
            
            Team.getInstance().enabled = tpa;
            if (!tpa)
                TPAParty.getInstance().enabled = false;
            TPATools.getInstance().enabled = tpa;
            
            ThreadManager.run(() -> {
                it.text = "Done";
                try {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                it.text = "Set";
            });
        }));
    }
    
    @Override
    public void onSubTick() {
    
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
    
    @Override
    public void onConfigLoad() {
        updateBinds();
    }
    
    // Settings for the unique servers
    private enum Server {
        _8b8t
                ("8b8t.xyz",
                 true, true, true),
        
        _5b5t
                ("5b5t.net",
                 false, false, false),
        
        _0t0t
                ("0b0t.org",
                 false, false, true),
        
        _2b2t
                ("2b2t.org",
                 false, false, false),
        
        crystalpvp
                ("crystalpvp.cc",
                 false, false, false),
        
        ;
        
        String name;
        
        // Settings
        boolean stackedTots;
        boolean pvp;
        boolean tpa;
        
        Server(String name, boolean stackedTots, boolean pvp, boolean tpa) {
            this.name = name;
            this.stackedTots = stackedTots;
            this.pvp = pvp;
            this.tpa = tpa;
        }
    }
}
