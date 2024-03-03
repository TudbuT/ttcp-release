package de.tudbut.mod.client.ttcp.mods.command;

import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Command;
import de.tudbut.tools.Time;

import java.text.DateFormat;
import java.util.Date;

@Command
public class Api extends Module {
    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public boolean doStoreEnabled() {
        return false;
    }
    
    @Override
    public boolean defaultEnabled() {
        return true;
    }
    
    /*
    @Override
    public void onChat(String s, String[] args) {
        TudbuTAPI.getUser(s)
                 .then(this::printData)
                 .err(Throwable::printStackTrace)
                 .err(e -> ChatUtils.print("Couldn't find that player on " + TudbuTAPI.HOST + ":" + TudbuTAPI.PORT))
                 .ok();
    }
    
    private void printData(User user) {
        DateFormat f = DateFormat.getDateTimeInstance();
    
        String s = "";
        s += "Last login: " + f.format(user.getLastLogin()) + " (" +
             Time.ydhms((new Date().getTime() - user.getLastLogin().getTime()) / 1000).split("y ")[1] +
             " ago)\n";
        s += "Playtime: " + Time.ydhms(user.getPlaytimeSeconds()) + "\n";
        s += "Premium: " + (user.isPremium() ? "Yes" : "No") + "\n";
        s += "Last playing: " + f.format(user.getLastPlay()) + " (" +
             Time.ydhms((new Date().getTime() - user.getLastPlay().getTime()) / 1000).split("y ")[1] + " ago for " +
             Time.ydhms(Math.abs(user.getLastPlay().getTime() - user.getLastLogin().getTime()) / 1000).split("y ")[1] + ")\n";
        s += "Online: " + (new Date().getTime() - user.getLastPlay().getTime() < 2000 ? "Yes" : "No") + "\n";
        s += "Version: " + user.getVersion().toString();
        
        ChatUtils.print(s);
    }
    
     */
}
