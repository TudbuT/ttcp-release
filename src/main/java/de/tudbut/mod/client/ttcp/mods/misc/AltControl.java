package de.tudbut.mod.client.ttcp.mods.misc;

import de.tudbut.timer.AsyncTask;
import de.tudbut.type.Vector3d;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.GuiPlayerSelect;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.mods.combat.KillAura;
import de.tudbut.mod.client.ttcp.mods.rendering.PlayerSelector;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Misc;
import de.tudbut.net.ic.PBIC;
import de.tudbut.obj.Atomic;
import de.tudbut.obj.Save;
import de.tudbut.tools.Queue;

import java.util.*;

import static de.tudbut.mod.client.ttcp.utils.TTCIC.*;

@Misc
public class AltControl extends Module {
    
    private static AltControl instance;
    {
        instance = this;
    }
    public static AltControl getInstance() {
        return instance;
    }
    
    private int confirmationInstance = 0;
    public int mode = -1;
    @Save
    private boolean botMain = true;
    @Save
    private boolean useElytra = true;
    private boolean stopped = true;
    private final Atomic<Vec3d> commonTarget = new Atomic<>();
    private EntityPlayer commonTargetPlayer = null;
    private long lostTimer = 0;
    public final Queue<PBIC.Packet> toSend = new Queue<>();
    
    PBIC.Server server;
    PBIC.Client client;
    
    Alt main = new Alt();
    ArrayList<Alt> alts = new ArrayList<>();
    Map<PBIC.Connection, Alt> altsMap = new HashMap<>();
    
    
    @Override
    public void init() {
        PlayerSelector.types.add(new PlayerSelector.Type(player -> {
            onChat("kill " + player.getGameProfile().getName(), ("kill " + player.getGameProfile().getName()).split(" "));
        }, "Set AltControl.Kill target"));
    
        PlayerSelector.types.add(new PlayerSelector.Type(player -> {
            onChat("follow " + player.getGameProfile().getName(), ("follow " + player.getGameProfile().getName()).split(" "));
        }, "Set AltControl.Follow target"));
    }
    
    @SuppressWarnings("unused")
    public void triggerSelectKill() {
        TTCp.mc.displayGuiScreen(
                new GuiPlayerSelect(
                        TTCp.world.playerEntities.stream().filter(
                                player -> !player.getName().equals(TTCp.player.getName())
                        ).toArray(EntityPlayer[]::new),
                        player -> {
                            if (server != null)
                                onChat("kill " + player.getName(), ("kill " + player.getName()).split(" "));
                            return true;
                        }
                )
        );
    }
    
    @SuppressWarnings("unused")
    public void triggerSelectFollow() {
        TTCp.mc.displayGuiScreen(
                new GuiPlayerSelect(
                        TTCp.world.playerEntities.toArray(new EntityPlayer[0]),
                        player -> {
                            if (server != null)
                                onChat("follow " + player.getName(), ("follow " + player.getName()).split(" "));
                            return true;
                        }
                )
        );
    }
    
    @SuppressWarnings("unused")
    public void triggerStop() {
        onChat("stop", "stop".split(" "));
    }
    
    @Override
    public void onConfigLoad() {
    }
    
    public void updateBinds() {
        customKeyBinds.set("kill", new KeyBind(null, toString() + "::triggerSelectKill", false));
        customKeyBinds.set("follow", new KeyBind(null, toString() + "::triggerSelectFollow", false));
        customKeyBinds.set("stop", new KeyBind(null, toString() + "::triggerStop", false));
        
        
        subComponents.clear();
        
        if(mode == -1) {
            subComponents.add(new Button("Main mode", it -> {
                if (mode != -1)
                    return;
        
                displayConfirmation = true;
                confirmationInstance = 0;
            }));
            subComponents.add(new Button("Alt mode", it -> {
                if (mode != -1)
                    return;
        
                displayConfirmation = true;
                confirmationInstance = 1;
            }));
        }
        else {
            subComponents.add(new Button("End connection", it ->
                    onChat("end", "end".split(" "))));
            subComponents.add(new Button("List", it ->
                    onChat("list", "list".split(" "))));
        }
        if(mode == 0) {
            subComponents.add(new Button("TPA alts here", it ->
                    onChat("tpa", "tpa".split(" "))));
            subComponents.add(new Button("Stop alts", it ->
                    onChat("stop", "stop".split(" "))));
            subComponents.add(new Button("Follow me", it ->
                    onChat("follow", "follow".split(" "))));
            subComponents.add(new Button("Send client config", it ->
                    onChat("send", "send".split(" "))));
            subComponents.add(new Button("Use elytra: " + useElytra, it -> {
                onChat("telytra", "telytra".split(" "));
                it.text = "Use elytra: " + useElytra;
            }));
            subComponents.add(new Button("Bot main: " + botMain, it -> {
                botMain = !botMain;
                it.text = "Bot main: " + botMain;
            }));
        }
        
        subComponents.add(new Button("Show GUIs", it -> {}) {
            {
                subComponents.add(Setting.createKey("Kill", customKeyBinds.get("kill")));
                subComponents.add(Setting.createKey("Follow", customKeyBinds.get("follow")));
            }
        });
        subComponents.add(Setting.createKey("Stop", customKeyBinds.get("stop")));
    }
    
    public boolean isAlt(EntityPlayer player) {
        try {
            for (int i = 0; i < alts.size(); i++) {
                if (alts.get(i).uuid.equals(player.getGameProfile().getId())) {
                    return true;
                }
            }
            return player.getGameProfile().getId().equals(main.uuid);
        } catch (NullPointerException e) {
            for (int i = 0; i < alts.size(); i++) {
                if (alts.get(i).name.equals(player.getGameProfile().getName())) {
                    return true;
                }
            }
            return player.getGameProfile().getName().equals(main.name);
        }
    }
    
    @Override
    public void onConfirm(boolean result) {
        if(result) {
            switch (confirmationInstance) {
                case 0:
                    onChat("server", "server".split(" "));
                    break;
                case 1:
                    onChat("client", "client".split(" "));
                    break;
            }
        }
    }
    
    @Override
    public void onEnable() {
    }
    
    @Override
    public void onTick() {
        if(useElytra && !stopped) {
            if(TTCp.isIngame()) {
                NetworkPlayerInfo[] players = Objects.requireNonNull(TTCp.mc.getConnection()).getPlayerInfoMap().toArray(new NetworkPlayerInfo[0]);
                
                if (main.uuid.equals(TTCp.player.getUniqueID())) {
                    if (new Date().getTime() - lostTimer > 10000) {
                        FlightBot.setSpeed(1.00);
                    } else if (new Date().getTime() - lostTimer > 5000) {
                        FlightBot.setSpeed(0.75);
                    }
                }
    
                // Target is in rd
                if (commonTargetPlayer != null && TTCp.world.getPlayerEntityByName(commonTargetPlayer.getName()) != null)
                    follow();
                // Target is not in rd, but isnt stopped
                else if (new Date().getTime() - lostTimer > 5000) {
                    FlightBot.deactivate();
                    commonTargetPlayer = null;
                    commonTarget.set(null);
                    // Isnt main
                    if (!main.uuid.equals(TTCp.player.getUniqueID())) {
                        // Follow main
                        
                        // Is main on same world & last lost query is 5 secs in past
                        if (
                                TTCp.world.getPlayerEntityByName(main.name) == null &&
                                new Date().getTime() - lostTimer > 5000 &&
                                Arrays.stream(players).anyMatch(
                                        player -> player.getGameProfile().getId().equals(main.uuid)
                                )
                        ) {
                            try {
                                // Send lost query
                                sendPacket(PacketsCS.LOST, "");
                            }
                            catch (PBIC.PBICException.PBICWriteException e) {
                                e.printStackTrace();
                            }
                            lostTimer = new Date().getTime();
                        } else
                            follow(main.name);
                    }
                }
            }
        }
    }
    
    // When the client receives a packet
    public void onPacketSC(PacketSC packet) {
        if (client == null)
            throw new RuntimeException();
        try {
            ChatUtils.chatPrinterDebug().println("Received packet[" + packet.type() + "]{" + packet.content() + "}");
            
            switch (packet.type()) {
                case INIT:
                    main = new Alt();
                    sendPacket(PacketsCS.NAME, TTCp.mc.getSession().getProfile().getName());
                    break;
                case NAME:
                    main.name = packet.content();
                    ChatUtils.print("Connection to main " + main.name + " established!");
                    sendPacket(PacketsCS.UUID, TTCp.mc.getSession().getProfile().getId().toString());
                    break;
                case UUID:
                    main.uuid = UUID.fromString(packet.content());
                    ChatUtils.print("Got UUID from main " + main.name + ": " + packet.content());
                    sendPacket(PacketsCS.KEEPALIVE, "");
                    break;
                case TPA:
                    ChatUtils.print("TPA'ing main account...");
                    TTCp.player.sendChatMessage("/tpa " + main.name);
                    break;
                case EXECUTE:
                    ChatUtils.print("Sending message received from main account...");
                    ChatUtils.simulateSend(packet.content(), false);
                    break;
                case LIST:
                    TTCp.logger.info("Received alt list from main.");
                    Map<String, String> map0 = Utils.stringToMap(packet.content());
            
                    alts.clear();
                    int len = map0.keySet().size();
                    for (int i = 0; i < len; i++) {
                        Alt alt;
                        alts.add(alt = new Alt());
                
                        Map<String, String> map1 = Utils.stringToMap(map0.get(String.valueOf(i)));
                        alt.name = map1.get("name");
                        alt.uuid = UUID.fromString(map1.get("uuid"));
                    }
                    break;
                case KILL:
                    ChatUtils.print("Killing player " + packet.content());
                    kill(packet.content());
                    break;
                case FOLLOW:
                    ChatUtils.print("Following " + packet.content());
                    follow(packet.content());
                    break;
                case STOP:
                    stop(packet.content());
                    break;
                case CONFIG:
                    //TTCp.cfg = Utils.stringToMap(packet.content());
                    //TTCp.getInstance().saveConfig();
                    break;
                case WALK:
                    useElytra = false;
                    FlightBot.deactivate();
                    break;
                case ELYTRA:
                    if(!useElytra && !stopped)
                        ChatUtils.simulateSend("#stop", false);
                    useElytra = true;
                    break;
                case KEEPALIVE:
                    sendPacket(PacketsCS.KEEPALIVE, "");
                    break;
                case POSITION:
                    if(commonTargetPlayer == null && !stopped) {
                        Vector3d vec = Vector3d.fromMap(Utils.stringToMap(packet.content()));
                        FlightBot.deactivate();
                        commonTarget.set(new Vec3d(vec.getX(), vec.getY() + 2, vec.getZ()));
                        FlightBot.activate(commonTarget);
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // When the server receives a packet
    public void onPacketCS(PacketCS packet, PBIC.Connection connection) throws PBIC.PBICException.PBICWriteException {
        ChatUtils.chatPrinterDebug().println("Received packet[" + packet.type() + "]{" + packet.content() + "}");
        switch (packet.type()) {
            case NAME:
                altsMap.get(connection).name = packet.content();
                ChatUtils.print("Connection to alt " + packet.content() + " established!");
                connection.writePacket(getPacketSC(PacketsSC.NAME, TTCp.mc.getSession().getProfile().getName()));
                break;
            case UUID:
                altsMap.get(connection).uuid = UUID.fromString(packet.content());
                ChatUtils.print("Got UUID from alt " + altsMap.get(connection).name + ": " + packet.content());
                connection.writePacket(getPacketSC(PacketsSC.UUID, TTCp.mc.getSession().getProfile().getId().toString()));
                
                sendList();
                
                break;
            case KEEPALIVE:
                ThreadManager.run(() -> {
                    try {
                        Thread.sleep(10000);
                        connection.writePacket(getPacketSC(PacketsSC.KEEPALIVE, ""));
                    }
                    catch (PBIC.PBICException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                break;
            case LOST:
                EntityPlayerSP player = TTCp.player;
                if(player != null && TTCp.world != null) {
                    connection.writePacket(getPacketSC(PacketsSC.POSITION, new Vector3d(player.posX, player.posY, player.posZ).toString()));
                }
                FlightBot.setSpeed(0.5);
                lostTimer = new Date().getTime();
                break;
        }
    }
    
    public void sendPacketSC(PacketsSC type, String content) {
        if(server.connections.size() == 0)
            return;
        
        AsyncTask<Object> task = new AsyncTask<>(() -> {
            ChatUtils.chatPrinterDebug().println("Sending packet[" + type.name() + "]{" + content + "}");
            try {
                PBIC.Connection[] connections = server.connections.toArray(new PBIC.Connection[0]);
                for (int i = 0; i < connections.length; i++) {
                    try {
                        connections[i].writePacket(getPacketSC(type, content));
                    }
                    catch (Exception ignore) { }
                }
            } catch (Throwable e) {
                return e;
            }
            return new Object();
        });
        task.setTimeout(server.connections.size() * 1500L);
        pce(task.waitForFinish(0));
    }
    
    public void sendPacketDelayedSC(PacketsSC type, String content) {
        if(server.connections.size() == 0)
            return;
        AsyncTask<Object> task = new AsyncTask<>(() -> {
            ChatUtils.chatPrinterDebug().println("Sending packet[" + type.name() + "]{" + content + "}");
            try {
                PBIC.Connection[] connections = server.connections.toArray(new PBIC.Connection[0]);
                for (int i = 0; i < connections.length; i++) {
                    try {
                        connections[i].writePacket(getPacketSC(type, content));
                        Thread.sleep(500);
                    }
                    catch (Exception ignore) { }
                }
            } catch (Throwable e) {
                return e;
            }
            return new Object();
        });
        task.setTimeout(server.connections.size() * 2000L);
        task.then(this::pce);
    }
    
    private void pce(Object r) {
        if(r instanceof Throwable || r == null) {
            ChatUtils.chatPrinterDebug().println("§c§lError during communication!");
            String etype;
            if(r == null) {
                etype = "ETimeout";
            }
            else if(r instanceof Exception) {
                etype = "EExceptionSend {" + ((Exception) r).getMessage() + "}";
                ((Throwable) r).printStackTrace(ChatUtils.chatPrinterDebug());
            }
            else {
                etype = "EErrorSend {" + ((Throwable) r).getMessage() + "}";
                ((Throwable) r).printStackTrace(ChatUtils.chatPrinterDebug());
            }
            ChatUtils.chatPrinterDebug().println(etype);
        }
    }
    
    public void sendPacket(PacketsCS type, String content) throws PBIC.PBICException.PBICWriteException {
        ChatUtils.chatPrinterDebug().println("Sending packet[" + type.name() + "]{" + content + "}");
        if(client == null)
            throw new RuntimeException();
        client.connection.writePacket(getPacketCS(type, content));
    }
    
    @Override
    public void onChat(String s, String[] args) {
        try {
            if (s.equals("server") && server == null) {
                main = new Alt();
                main.name = TTCp.mc.getSession().getProfile().getName();
                main.uuid = TTCp.mc.getSession().getProfile().getId();
                
                altsMap = new HashMap<>();
                
                server = new PBIC.Server(50278);
                server.onJoin.add(() -> {
                    PBIC.Connection theConnection = server.lastConnection;
                    AsyncTask<Object> task = new AsyncTask<>(() -> {
                        ChatUtils.chatPrinterDebug().println("Sending packet[INIT]{}");
                        try {
                            theConnection.writePacket(getPacketSC(PacketsSC.INIT, ""));
                        } catch (Throwable e) {
                            return e;
                        }
                        ChatUtils.chatPrinterDebug().println("Done");
                        return new Object();
                    });
                    task.setTimeout(1500L);
                    pce(task.waitForFinish(0));
    
                    altsMap.put(theConnection, new Alt());
    
                    while (true) {
                        String string = "UNKNOWN";
                        try {
                            PBIC.Packet packet = theConnection.readPacket();
                            string = packet.getContent();
                            onPacketCS(getPacketCS(packet), theConnection);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            System.err.println("Packet: " + string);
                        }
                    }
                });
                server.start();
                
                mode = 0;
                
                ChatUtils.print("§aServer started");
            }
            if (args[0].equals("client") && client == null) {
                if(args.length == 2)
                    client = new PBIC.Client(args[1], 50278);
                else if(args.length == 3)
                    client = new PBIC.Client(args[1], Integer.parseInt(args[2]));
                else
                    client = new PBIC.Client("127.0.0.1", 50278);
                ChatUtils.print("Client started");
                ThreadManager.run("TTCIC client receive thread", () -> {
                    while (true) {
                        String string = "UNKNOWN";
                        try {
                            PBIC.Packet packet = client.connection.readPacket();
                            string = packet.getContent();
                            onPacketSC(getPacketSC(packet));
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            System.err.println("Packet: " + string);
                            onChat("end", "end".split(" "));
                        }
                    }
                });
                mode = 1;
            }
            
            if(args.length >= 2) {
                if(args[0].equals("send") && s.contains(" ")) {
                    String st = s.substring(s.indexOf(" ") + 1);
                    sendPacketSC(PacketsSC.EXECUTE, st);
                    ChatUtils.simulateSend(st, false);
                }
                if(args[0].equals("kill") && s.contains(" ")) {
                    sendList();
                    String st = s.substring(s.indexOf(" ") + 1);
                    if(useElytra) {
                        sendPacketSC(PacketsSC.ELYTRA, "");
                    } else {
                        sendPacketSC(PacketsSC.WALK, "");
                    }
                    sendPacketSC(PacketsSC.KILL, st);
                    if(botMain) {
                        kill(st);
                    }
                }
                if(args[0].equals("stop") && s.contains(" ")) {
                    String st = s.substring(s.indexOf(" ") + 1);
                    sendPacketSC(PacketsSC.STOP, st);
                    ChatUtils.print("Stopping killing player " + st);
                    if(botMain) {
                        stop(st);
                    }
                }
    
                if (args[0].equals("follow")) {
                    if(useElytra) {
                        sendPacketSC(PacketsSC.ELYTRA, "");
                    } else {
                        sendPacketSC(PacketsSC.WALK, "");
                    }
                    sendPacketSC(PacketsSC.FOLLOW, args[1]);
                    follow(args[1]);
                }
            }
            
            if (s.equals("stop")) {
                if(useElytra) {
                    sendPacketSC(PacketsSC.ELYTRA, "");
                } else {
                    sendPacketSC(PacketsSC.WALK, "");
                }
                sendPacketSC(PacketsSC.STOP, "");
                ChatUtils.print("Stopping killing/following all players");
                if(botMain) {
                    stop(null);
                }
            }
    
            if (s.equals("send")) {
                TTCp.getInstance().setConfig();
                //sendPacketSC(PacketsSC.CONFIG, Utils.mapToString(TTCp.cfg));
                ChatUtils.print("Sending config to all alts");
            }
            
            if (s.equals("tpa")) {
                sendList();
                sendPacketDelayedSC(PacketsSC.TPA, "");
            }
    
            if (s.equals("follow")) {
                if(useElytra) {
                    sendPacketSC(PacketsSC.ELYTRA, "");
                } else {
                    sendPacketSC(PacketsSC.WALK, "");
                }
                sendPacketSC(PacketsSC.FOLLOW, main.name);
            }
            
            if(s.equals("telytra")) {
                useElytra = !useElytra;
            }
            
            if (s.equals("end")) {
    
                alts.clear();
                while (toSend.hasNext()) toSend.next();
                altsMap.clear();
                stopped = false;
                useElytra = false;
                commonTargetPlayer = null;
                commonTarget.set(null);
                stopped = false;
                main = new Alt();
                
                if(client != null)
                    client.close();
                client = null;
                if(server != null)
                    server.close();
                server = null;
                mode = -1;
                
                alts = new ArrayList<>();
                altsMap = new HashMap<>();
            }
            
            if(s.equals("list")) {
                StringBuilder string = new StringBuilder("List:");
                if(server != null) {
                    for (int i = 0; i < server.connections.size(); i++) {
                        PBIC.Connection connection = server.connections.get(i);
                        Alt alt = altsMap.get(connection);
                        if(alt == null || alt.name == null)
                            onChat("end", "end".split(" "));
                        else
                            string.append(" ").append(alt.name).append(",");
                    }
                }
                if(client != null) {
                    for (int i = 0; i < alts.size(); i++) {
                        Alt alt = alts.get(i);
                        if(alt == null || alt.name == null)
                            onChat("end", "end".split(" "));
                        else
                            string.append(" ").append(alt.name).append(",");
                    }
                }
                if(string.toString().contains(","))
                    string = new StringBuilder(string.substring(0, string.length() - 2));
                ChatUtils.print(string.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        updateBinds();
    }
    
    private void sendList() {
        if(server.connections.size() == 0)
            return;
        
        Map<String, String> map0 = new HashMap<>();
        PBIC.Connection[] keys = altsMap.keySet().toArray(new PBIC.Connection[0]);
        alts.clear();
        for (int i = 0; i < keys.length; i++) {
            Alt alt = altsMap.get(keys[i]);
            alts.add(alt);
            
            Map<String, String> map1 = new HashMap<>();
            map1.put("name", alt.name);
            map1.put("uuid", alt.uuid.toString());
            
            map0.put(String.valueOf(i), Utils.mapToString(map1));
        }
        sendPacketSC(PacketsSC.LIST, Utils.mapToString(map0));
    }
    
    public void follow(String name) {
        if(TTCp.player.getName().equals(name))
            return;
        commonTargetPlayer = TTCp.world.getPlayerEntityByName(name);
        follow();
    }
    
    public void kill(String name) {
        follow(name);
        KillAura aura = KillAura.getInstance();
        aura.enabled = true;
        aura.onEnable();
        aura.targets.add(name);
    }
    
    public void stop(String name) {
        KillAura aura = KillAura.getInstance();
        commonTargetPlayer = null;
        commonTarget.set(null);
        stopped = true;
        FlightBot.deactivate();
        if(!useElytra)
            ChatUtils.simulateSend("#stop", false);
        if(name == null || name.equals("")) {
            aura.targets.clear();
            aura.enabled = false;
            aura.onDisable();
        }
        else {
            aura.targets.remove(name);
            aura.targets.trimToSize();
            if (aura.targets.size() != 0) {
                ChatUtils.print("Killing player " + name);
                follow(aura.targets.get(0));
            }
        }
    }
    
    public void follow() {
        if(commonTargetPlayer == null) {
            FlightBot.deactivate();
            return;
        }
        
        stopped = false;
        
        try {
            if (useElytra) {
                FlightBot.deactivate();
                FlightBot.activate(commonTarget);
                commonTarget.set(commonTargetPlayer.getPositionVector().add(0, 2, 0));
            } else
                ChatUtils.simulateSend("#follow player " + commonTargetPlayer.getName(), false);
        } catch (Exception e) {
            e.printStackTrace(ChatUtils.chatPrinter());
        }
    }
    
    @Override
    public boolean onServerChat(String s, String formatted) {
        if (
                s.contains("has requested to teleport to you.") &&
                alts.stream().anyMatch(alt -> s.startsWith(alt.name + " ") || s.startsWith("~" + alt.name + " "))
        ) {
            TTCp.player.sendChatMessage("/tpaccept");
        }
        return false;
    }
    
    @Override
    public void onDisable() {
        onChat("end", null);
    }
    
    public static class Alt {
        public String name;
        public UUID uuid;
    }
}
