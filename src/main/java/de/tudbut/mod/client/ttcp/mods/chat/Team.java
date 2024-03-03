package de.tudbut.mod.client.ttcp.mods.chat;

import net.minecraft.client.network.NetworkPlayerInfo;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.ThreadManager;
import de.tudbut.mod.client.ttcp.utils.category.Chat;
import de.tudbut.obj.Save;

import java.util.ArrayList;
import java.util.Objects;

@Chat
public class Team extends Module {
    
    static Team instance;
    // Team members
    @Save
    public ArrayList<String> names = new ArrayList<>();
    // What should be allowed to the team members?
    @Save
    private boolean tpa = true;
    @Save
    private boolean tpaHere = false;
    
    public Team() {
        instance = this;
    }
    
    public static Team getInstance() {
        return instance;
    }
    
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new Button("Accept /tpa: " + tpa, it -> {
            tpa = !tpa;
            it.text = "Accept /tpa: " + tpa;
        }));
        subComponents.add(new Button("Accept /tpahere: " + tpaHere, it -> {
            tpaHere = !tpaHere;
            it.text = "Accept /tpahere: " + tpaHere;
        }));
        subComponents.add(new Button("Send /tpahere (/tpahere)", text -> onChat("", new String[]{"tpahere"})));
        subComponents.add(new Button("Send /tpahere (/tpa)", text -> onChat("", new String[]{"here"})));
        subComponents.add(new Button("Send DM", text -> ChatUtils.print("§c§lUse " + TTCp.prefix + "team dm <message>")));
        subComponents.add(new Button("Show list", text -> onChat("", new String[]{"list"})));
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
            case "settpa":
                // Enable/Disable TPA for Team members
                tpa = Boolean.parseBoolean(args[1]);
                ChatUtils.print("Done!");
                break;
            case "settpahere":
                // Enable/Disable TPAHere for Team members
                tpaHere = Boolean.parseBoolean(args[1]);
                ChatUtils.print("Done!");
                break;
            case "tpahere":
                // Send /tpahere to everyone in the team
                ChatUtils.print("Sending...");
                // This would stop the game if it wasn't in a separate thread
                ThreadManager.run(() -> {
                    // Loop through all players
                    for (NetworkPlayerInfo info : Objects.requireNonNull(TTCp.mc.getConnection()).getPlayerInfoMap().toArray(new NetworkPlayerInfo[0])) {
                        // Is the player a team member?
                        if (names.contains(info.getGameProfile().getName())) {
                            try {
                                // Send to the player
                                TTCp.mc.player.sendChatMessage("/tpahere " + info.getGameProfile().getName());
                                // Notify the user
                                ChatUtils.print("Sent to " + info.getGameProfile().getName());
                                // I hate antispam
                                Thread.sleep(TPATools.getInstance().delay);
                            }
                            catch (Throwable ignore) { }
                        }
                    }
                    ChatUtils.print("Done!");
                });
                break;
            case "here":
                ChatUtils.print("Sending...");
                // This would stop the game if it wasn't in a separate thread
                ThreadManager.run(() -> {
                    // Loop through all players
                    for (NetworkPlayerInfo info : Objects.requireNonNull(TTCp.mc.getConnection()).getPlayerInfoMap().toArray(new NetworkPlayerInfo[0])) {
                        // Is the player a team member?
                        if (names.contains(info.getGameProfile().getName())) {
                            try {
                                // Send to the player
                                TTCp.mc.player.sendChatMessage("/tell " + info.getGameProfile().getName() + " TTCp[0]");
                                // Notify the user
                                ChatUtils.print("Sent to " + info.getGameProfile().getName());
                                // I hate antispam
                                Thread.sleep(TPATools.getInstance().delay);
                            }
                            catch (Throwable ignore) { }
                        }
                    }
                    ChatUtils.print("Done!");
                });
                break;
            case "go":
                // This would stop the game if it wasn't in a separate thread
                ThreadManager.run(() -> {
                    // Loop through all players
                    for (NetworkPlayerInfo info : Objects.requireNonNull(TTCp.mc.getConnection()).getPlayerInfoMap().toArray(new NetworkPlayerInfo[0])) {
                        // Is it the right team member
                        if (info.getGameProfile().getName().equals(args[1])) {
                            try {
                                // Send to the player
                                TTCp.mc.player.sendChatMessage("/tell " + info.getGameProfile().getName() + " TTCp[1]");
                            }
                            catch (Throwable ignore) { }
                        }
                    }
                    ChatUtils.print("Sent!");
                });
                break;
            case "dm":
                ChatUtils.print("Sending...");
                // This would stop the game if it wasn't in a separate thread
                ThreadManager.run(() -> {
                    // Loop through all players
                    for (NetworkPlayerInfo info : Objects.requireNonNull(TTCp.mc.getConnection()).getPlayerInfoMap().toArray(new NetworkPlayerInfo[0])) {
                        // Is the player a team member?
                        if (names.contains(info.getGameProfile().getName())) {
                            try {
                                // Send to the player
                                TTCp.mc.player.sendChatMessage("/tell " + info.getGameProfile().getName() + " " + s.substring("dm ".length()));
                                // Notify the user
                                ChatUtils.print("Sent to " + info.getGameProfile().getName());
                                // I hate antispam
                                Thread.sleep(TPATools.getInstance().delay);
                            }
                            catch (Throwable ignore) { }
                        }
                    }
                    ChatUtils.print("Done!");
                });
                break;
            case "settings":
                // Print the member list and settings
                ChatUtils.print("TPA: " + (tpa ? "enabled" : "disabled"));
                ChatUtils.print("TPAhere: " + (tpaHere ? "enabled" : "disabled"));
            case "list":
                // Print the member list
                StringBuilder toPrint = new StringBuilder("Team members: ");
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
    public boolean onServerChat(String s, String formatted) {
        if (tpa && s.contains("has requested to teleport to you.") && names.stream().anyMatch(name -> s.startsWith(name + " ") || s.startsWith("~" + name + " "))) {
            TTCp.player.sendChatMessage("/tpaccept");
        }
        if (tpaHere && s.contains("has requested that you teleport to them.") && names.stream().anyMatch(name -> s.startsWith(name + " ") || s.startsWith("~" + name + " "))) {
            TTCp.player.sendChatMessage("/tpaccept");
        }
        
        try {
            // See if it is a DM from a Team member
            String name = names.stream().filter(
                    theName ->
                            s.startsWith(theName + " whispers:") ||
                            s.startsWith("~" + theName + " whispers:") ||
                            s.startsWith(theName + " whispers to you:") ||
                            s.startsWith("~" + theName + " whispers to you:") ||
                            s.startsWith("From " + theName + ":") ||
                            s.startsWith("From ~" + theName + ":")
            ).iterator().next();
            if (name != null) {
                String msg = s.split(": ")[1];
                if (msg.startsWith("TTCp")) { // Control codes from team members
                    if (msg.equals("TTCp[0]") && tpaHere) {
                        TTCp.player.sendChatMessage("/tpa " + name);
                        ChatUtils.print("Sent TPA to " + name + ".");
                    }
                    if (msg.equals("TTCp[1]") && tpa) {
                        TTCp.player.sendChatMessage("/tpahere " + name);
                        ChatUtils.print("Sent TPAHere to " + name + ".");
                    }
                    if (msg.equals("TTCp[3]")) {
                        ChatUtils.print("§c§lYou have been removed from the Team of " + name + "! \n" +
                                        "§cRun ,team remove " + name + " to remove them as well!");
                    }
                    // Cancel the display of the default message
                    return true;
                }
                
                ChatUtils.print("§b§lDM from team member: §r<" + name + "> " + s.substring(s.indexOf(": ") + 2));
                // Cancel the display of the default message
                return true;
            }
            // DM parsing of people outside the team
            for (NetworkPlayerInfo info : Objects.requireNonNull(TTCp.mc.getConnection()).getPlayerInfoMap().toArray(new NetworkPlayerInfo[0])) {
                String theName = info.getGameProfile().getName();
                // Is it a DM, if yes, is this the player it came from?
                if (s.startsWith(theName + " whispers:") ||
                    s.startsWith("~" + theName + " whispers:") ||
                    s.startsWith(theName + " whispers to you:") ||
                    s.startsWith("~" + theName + " whispers to you:") ||
                    s.startsWith("From " + theName + ":") ||
                    s.startsWith("From ~" + theName + ":")) {
                    try {
                        String msg = s.split(": ")[1];
                        if (msg.startsWith("TTCp")) { // Control codes from non-members
                            if (msg.equals("TTCp[2]")) {
                                ChatUtils.print("§c§lYou have been added to the Team of " + theName + "! \n" +
                                                "§cRun ,team add " + theName + " to add them as well!");
                            }
                            // Cancel the display of the default message
                            return true;
                        }
                    }
                    catch (Throwable ignore) { }
                }
            }
        }
        catch (Exception ignore) { }
        return false;
    }
    
    @Override
    public void onConfigLoad() {
        updateBinds();
    }
}
