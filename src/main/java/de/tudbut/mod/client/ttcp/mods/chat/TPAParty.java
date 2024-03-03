package de.tudbut.mod.client.ttcp.mods.chat;

import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Chat;
import de.tudbut.obj.Save;

@Chat
public class TPAParty extends Module {
    
    static TPAParty instance;
    @Save
    public boolean disableOnDeath = true;
    
    public TPAParty() {
        instance = this;
    }

    public void updateButtons() {
        subComponents.clear();
        subComponents.add(Setting.createBoolean("DeathDisable", this, "disableOnDeath"));
    }
    
    public static TPAParty getInstance() {
        return instance;
    }
    
    @Override
    public void onSubTick() {
    
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
    
    @Override
    public boolean onServerChat(String s, String formatted) {
        if (s.contains("/tpaccept") && !s.startsWith("<")) {
            // Accept TPA requests
            TTCp.player.sendChatMessage("/tpaccept");
        }
        return false;
    }
    
    @Override
    public int danger() {
        return 4;
    }
}
