package de.tudbut.mod.client.ttcp.mods.chat;

import de.tudbut.tools.Tools;
import net.minecraft.client.network.NetworkPlayerInfo;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Chat;
import de.tudbut.obj.Save;
import de.tudbut.obj.TLMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

@Chat
public class Spam extends Module {

    @Save
    public TLMap<String, Spammer> toSpam = new TLMap<>();
    
    public Spammer current;
    long last = 0;
    
    public void onTick() {
        NetworkPlayerInfo[] players = Objects.requireNonNull(TTCp.mc.getConnection()).getPlayerInfoMap().toArray(new NetworkPlayerInfo[0]);
        if(current != null && new Date().getTime() - last > current.delay) {
            if(current.current >= current.toSpam.size())
                current.current = 0;
            if(current.current >= current.toSpam.size())
                return;
            String alphabet = "abcdefghijklmnopqrstuvwxyz";
            String pool = alphabet + alphabet.toUpperCase() + "0123456789     ,.-#+";
            String player = players[(int) (players.length * Math.random())].getGameProfile().getName();
            TTCp.player.sendChatMessage(current.toSpam.get(current.current++).replaceAll("%random10", Tools.randomString(10, pool)).replaceAll("%random20", Tools.randomString(20, pool)).replaceAll("%random30", Tools.randomString(30, pool)).replaceAll("%player", player));
            last = new Date().getTime();
        }
    }

    @Override
    public void onChat(String s, String[] args) {

    }

    @Override
    public void onEveryChat(String s, String[] args) {
        if(args.length == 0 || args[0].equals("help")) {
            ChatUtils.print("§a,spam help:");
            ChatUtils.print("");
            ChatUtils.print(",spam add <name>");
            ChatUtils.print("  Create a spammer");
            ChatUtils.print(",spam list");
            ChatUtils.print("  List the available spammers");
            ChatUtils.print(",spam remove <name>");
            ChatUtils.print("  Delete a spammer");
            ChatUtils.print(",spam set <name>");
            ChatUtils.print("  Set the spammer to use");
            ChatUtils.print(",spam + <name> <text...>");
            ChatUtils.print("  Text can have: ");
            ChatUtils.print("    %random10 -> random string of length 10");
            ChatUtils.print("    %random20 -> random string of length 20");
            ChatUtils.print("    %random30 -> random string of length 30");
            ChatUtils.print("    %player   -> random playername");
            ChatUtils.print(",spam delay <name> <seconds>");
            ChatUtils.print("  Set the frequency of messages of the spammer");
            return;
        }
        if(args[0].equals("list")) {
            if(args.length == 1) {
                for(TLMap.Entry<String, Spammer> it : toSpam.entries()) {
                    ChatUtils.print(it.key);
                }
            }
            else {
                for(String it : toSpam.get(args[1]).toSpam) {
                    ChatUtils.print(it);
                }
            }
        }
        if(args[0].equals("add")) {
            toSpam.set(args[1], new Spammer());
            ChatUtils.print("Done!");
        }
        if(args[0].equals("remove")) {
            toSpam.set(args[1], null);
            ChatUtils.print("Done!");
        }
        if(args[0].equals("set")) {
            current = toSpam.get(args[1]);
            ChatUtils.print("Done!");
        }
        if(args[0].equals("+")) {
            toSpam.get(args[1]).toSpam.add(s.substring(s.indexOf("+") + args[1].length() + 3));
            ChatUtils.print("Done!");
        }
        if(args[0].equals("delay")) {
            toSpam.get(args[1]).delay = (int) (Float.parseFloat(args[2]) * 1000);
            ChatUtils.print("Done!");
        }
    }

    public static class Spammer {
        int delay = 5000;
        int current = 0;
        ArrayList<String> toSpam = new ArrayList<>();
    }
}
