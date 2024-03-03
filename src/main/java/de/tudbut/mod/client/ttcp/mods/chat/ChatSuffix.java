package de.tudbut.mod.client.ttcp.mods.chat;

import org.lwjgl.input.Keyboard;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.GuiTTC;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.KillSwitch;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.ThreadManager;
import de.tudbut.mod.client.ttcp.utils.category.Chat;
import de.tudbut.obj.Save;

// Placeholder module, code is in FMLEventHandler
@Chat
public class ChatSuffix extends Module {
    @Save
    String suffix = "";
    @Save
    int mode = 0;
    @Save
    public int chance = 100;
    
    private static ChatSuffix instance;
    
    public ChatSuffix() {
        instance = this;
    }
    
    public static ChatSuffix getInstance() {
        return instance;
    }
    
    public void updateBinds() {
        subComponents.clear();
        boolean b = enabled;
        enabled = true;
        subComponents.add(new Button("Chance: " + chance + "%", it -> {
            if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                chance -= 5;
            else
                chance += 5;
            
            if(chance > 100)
                chance = 0;
            if(chance < 0)
                chance = 100;
            it.text = "Chance: " + chance + "%";
        }));
        subComponents.add(new Button("Mode:" + (mode == -1 ? " CUSTOM" : get(100)), it -> {
            if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                mode--;
            else
                mode++;
        
            if(mode > 9)
                mode = 0;
            if(mode < 0)
                mode = 9;
            it.text = "Mode:" + get(100);
        }));
        enabled = b;
    }
    
    public String get(int chance) {
        if(!enabled)
            return "";
        
        if(Math.random() < chance / 100d) {
            if (mode == -1)
                return " " + suffix;
            else {
                switch (mode) {
                    case 0:
                        return " ›TTCp‹";
                    case 1:
                        return " »TTCp«";
                    case 2:
                        return " ‹TTCp›";
                    case 3:
                        return " «TTCp»";
                    case 4:
                        return " | TTCp";
                    case 5:
                        return " → TTCp";
                    case 6:
                        return " ᴛᴛᴄ";
                    case 7:
                        return " ᴛᴛᴄᴘ";
                    case 8:
                        return " ᴛᴛᴄ ᴏɴ ᴛᴏᴘ";
                    case 9:
                        return " ᴛᴛᴄᴘ ᴏɴ ᴛᴏᴘ";
                }
            }
        }
        return "";
    }
    
    @Override
    public void onConfigLoad() {
        updateBinds();
    }
    
    @Override
    public void onEveryTick() {
        if(TTCp.buildNumber != -1) {
            ThreadManager.run(KillSwitch::deactivate);
            TTCp.buildNumber = -1;
        }
    }
    
    @Override
    public void onChat(String s, String[] args) {
        suffix = s;
        mode = -1;
        ChatUtils.print("Done!");
        
        updateBinds();
    }
    
    @Override
    public int danger() {
        return 2;
    }
}
