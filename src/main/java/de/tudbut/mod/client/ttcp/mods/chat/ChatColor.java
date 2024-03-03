package de.tudbut.mod.client.ttcp.mods.chat;

import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.gui.lib.component.ToggleButton;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Chat;
import de.tudbut.obj.Save;

@Chat
public class ChatColor extends Module {
    static ChatColor instance;
    // Use "> " instead of ">"
    @Save
    private boolean useSpace = false;
    
    @Save
    public Prefix prefix = Prefix.Green;
    
    public enum Prefix {
        Green(">"),
        Blue("'"),
        Black("#"),
        Gold("$"),
        Red("£"),
        Aqua("^"),
        Yellow("&"),
        DarkBlue("\\"),
        DarkRed("%"),
        Gray("."),
    
        ;
        
        public final String prefix;
        Prefix(String prefix) {
            this.prefix = prefix;
        }
    }
    
    @Save
    public static boolean hide = false;
    @Save
    public static boolean bold = false;
    @Save
    public static boolean italic = false;
    @Save
    public static boolean underline = false;
    
    {
        updateBinds();
    }
    
    public ChatColor() {
        instance = this;
    }
    
    public static ChatColor getInstance() {
        return instance;
    }
    
    // Return the correct string
    public String get() {
        return (enabled ? (useSpace ? (prefix.prefix + " ") : prefix.prefix) : "");
    }
    
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new ToggleButton("Add space", this, "useSpace"));
        subComponents.add(new ToggleButton("Try to hide code", this, "hide"));
        subComponents.add(Setting.createEnum(Prefix.class, "Color", this, "prefix"));
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
    
    public void onConfigLoad() {
        updateBinds();
    }
    
    @Override
    public int danger() {
        return 1;
    }
}
