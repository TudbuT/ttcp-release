package de.tudbut.mod.client.ttcp.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.mods.chat.*;
import de.tudbut.mod.client.ttcp.mods.combat.AutoCrystal;
import de.tudbut.mod.client.ttcp.mods.combat.KillAura;
import de.tudbut.mod.client.ttcp.mods.rendering.Freecam;
import de.tudbut.mod.client.ttcp.mods.rendering.HUD;
import de.tudbut.mod.client.ttcp.utils.*;

import java.util.Date;

public class EventHandler {
    
    public static long[] ping = {-1, 1, 1};
    public static float tps = 20.0f;
    private static long lastTick = -1;
    private static long joinTime = 0;
    private boolean isDead = true;
    public static final DebugProfilerAdapter profilerPackets = new DebugProfilerAdapter("Packets", "idle");
    public static final DebugProfilerAdapter profilerTicks = new DebugProfilerAdapter("Ticks", "idle");
    public static final DebugProfilerAdapter profilerChat = new DebugProfilerAdapter("Chat", "idle");
    public static final DebugProfilerAdapter profilerChatReceive = new DebugProfilerAdapter("ChatReceive", "idle");
    public static final DebugProfilerAdapter profilerRenderHUD = new DebugProfilerAdapter("RenderHUD", "idle");
    static {
        TTCp.registerProfiler(profilerPackets);
        TTCp.registerProfiler(profilerTicks);
        TTCp.registerProfiler(profilerChat);
        TTCp.registerProfiler(profilerChatReceive);
        TTCp.registerProfiler(profilerRenderHUD);
    }
    
    public static boolean onPacket(Packet<?> packet) {
        synchronized (profilerPackets) {
            boolean b = false;
            
            if(packet instanceof SPacketTimeUpdate) {
                long time = System.currentTimeMillis();
                if(lastTick != -1 && new Date().getTime() - joinTime > 5000) {
                    long diff = time - lastTick;
                    if(diff > 50) {
                        tps = (tps + ((1000f / diff) * 20f)) / 2;
                    }
                }
                else {
                    tps = 20.0f;
                }
                lastTick = time;
            }
    
            for (int i = 0 ; i < TTCp.modules.length ; i++) {
                if (TTCp.modules[i].enabled)
                    try {
                        profilerPackets.next(TTCp.modules[i] + " " + packet.getClass().getName());
                        if (TTCp.modules[i].onPacket(packet))
                            b = true;
                    }
                    catch (Exception e) {
                        e.printStackTrace(ChatUtils.chatPrinterDebug());
                    }
            }
            profilerPackets.next("idle");
            
            return b;
        }
    }
    
    @SubscribeEvent
    public void onEvent(Event event) {
        /*if(TTCp.plugins != null)
            for (int i = 0; i < TTCp.plugins.length; i++) {
                if(TTCp.plugins[i] != null)
                    TTCp.plugins[i].onEvent(new PluginForgeEvent<>(event));
            }*/
    }
    
    // Fired when enter is pressed in chat
    @SubscribeEvent
    public void onChat(ClientChatEvent event) {
        synchronized (profilerChat) {
            // Only for TTCp commands
            if (event.getOriginalMessage().startsWith(TTCp.prefix)) {
                profilerChat.next("command " + event.getOriginalMessage());
                // Don't send
                event.setCanceled(true);
                ChatUtils.print("Blocked message");
                // When canceled, the event blocks adding the message to the chat history,
                // so it'll cause confusion if this line doesn't exist
                ChatUtils.history(event.getOriginalMessage());
        
                // The command without the prefix
                String s = event.getOriginalMessage().substring(TTCp.prefix.length());
        
                try {
                    // Toggle a module
                    if (s.startsWith("t ")) {
                        for (int i = 0 ; i < TTCp.modules.length ; i++) {
                            if (TTCp.modules[i].toString().equalsIgnoreCase(s.substring("t ".length()))) {
                                ChatUtils.print(String.valueOf(!TTCp.modules[i].enabled));
                        
                                if (TTCp.modules[i].enabled = !TTCp.modules[i].enabled)
                                    TTCp.modules[i].onEnable();
                                else
                                    TTCp.modules[i].onDisable();
                            }
                        }
                    }
            
                    // Ignore any commands and say something
                    if (s.startsWith("say ")) {
                        TTCp.player.sendChatMessage(s.substring("say ".length()));
                        ChatUtils.history(event.getOriginalMessage());
                    }
            
                    if (s.equals("help")) {
                        //String help = Utils.getRemote("help.chat.txt", false);
                        //if (help == null) {
                        ChatUtils.print("Unable retrieve help message! Check your connection!");
                        //} else {
                        //help = help.replaceAll("%p", TTCp.prefix);
                        //ChatUtils.print(help);
                        //}
                    }

                    // Module-specific commands
                    for (int i = 0 ; i < TTCp.modules.length ; i++) {
                        if (s.toLowerCase().equals(TTCp.modules[i].toString().toLowerCase()) || s.toLowerCase().startsWith(TTCp.modules[i].toString().toLowerCase() + " ")) {
                            System.out.println("Passing command to " + TTCp.modules[i].toString());
                            try {
                                String args = s.substring(TTCp.modules[i].toString().length() + 1);
                                if (TTCp.modules[i].enabled)
                                    TTCp.modules[i].onChat(args, args.split(" "));
                                TTCp.modules[i].onEveryChat(args, args.split(" "));
                            }
                            catch (StringIndexOutOfBoundsException e) {
                                String args = "";
                                if (TTCp.modules[i].enabled)
                                    TTCp.modules[i].onChat(args, new String[0]);
                                TTCp.modules[i].onEveryChat(args, new String[0]);
                            }
                        }
                    }
                }
                catch (Exception e) {
                    ChatUtils.print("Command failed!");
                    e.printStackTrace(ChatUtils.chatPrinterDebug());
                }
        
                profilerChat.next("idle");
            }
            // A lil extra for the DM module
            else if (DM.getInstance().enabled) {
                profilerChat.next("dm");
                event.setCanceled(true);
                ChatUtils.history(event.getOriginalMessage());
                ThreadManager.run(() -> {
                    for (int i = 0 ; i < DM.getInstance().users.length ; i++) {
                        TTCp.player.sendChatMessage("/tell " + DM.getInstance().users[i] + " " + event.getOriginalMessage());
                        try {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                            e.printStackTrace(ChatUtils.chatPrinterDebug());
                        }
                    }
                });
                profilerChat.next("idle");
            }
            // A lil extra for the DMChat module
            else if (DMChat.getInstance().enabled) {
                profilerChat.next("dm");
                event.setCanceled(true);
                ChatUtils.history(event.getOriginalMessage());
                ThreadManager.run(() -> {
                    ChatUtils.print("<" + TTCp.player.getName() + "> " + event.getOriginalMessage());
                    for (int i = 0 ; i < DMChat.getInstance().users.length ; i++) {
                        TTCp.player.sendChatMessage("/tell " + DMChat.getInstance().users[i] + " " + event.getOriginalMessage());
                        try {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                });
                profilerChat.next("idle");
            }
            // Don't add chatcolor to commands!
            else if (!event.getOriginalMessage().startsWith("/") && !event.getOriginalMessage().startsWith(".") && !event.getOriginalMessage().startsWith("#")) {
                profilerChat.next("command");
                event.setCanceled(true);
                TTCp.player.sendChatMessage(ChatColor.getInstance().get() + event.getMessage() + ChatSuffix.getInstance().get(ChatSuffix.getInstance().chance));
        
                ChatUtils.history(event.getOriginalMessage());
                profilerChat.next("idle");
            }
        }
    }
    
    // When a message is received, those will often require parsing
    @SubscribeEvent
    public void onServerChat(ClientChatReceivedEvent event) {
        synchronized (profilerChatReceive) {
            profilerChatReceive.next("checkCaptcha");
            // BayMax AC will ask you for a captcha when you chat too much or spam,
            // this will automatically solve it
            if (event.getMessage().getUnformattedText().startsWith("BayMax") && event.getMessage().getUnformattedText().contains("Please type '")) {
                String key = event.getMessage().getUnformattedText().substring("BayMax _ Please type '".length(), "BayMax _ Please type '".length() + 4);
                TTCp.player.sendChatMessage(key);
                ChatUtils.print("Auto-solved");
            }
            if (event.getMessage().getUnformattedText().startsWith("Please type '") && event.getMessage().getUnformattedText().endsWith("' to continue sending messages/commands.")) {
                String key = event.getMessage().getUnformattedText().substring("Please type '".length(), "Please type '".length() + 6);
                TTCp.player.sendChatMessage(key);
                ChatUtils.print("Auto-solved");
            }
            // Trigger module event for server chat, the modules can cancel display of the message
            for (int i = 0 ; i < TTCp.modules.length ; i++) {
                if (TTCp.modules[i].enabled) {
                    profilerChatReceive.next("module " + TTCp.modules[i]);
                    if (TTCp.modules[i].onServerChat(event.getMessage().getUnformattedText(), event.getMessage().getFormattedText()))
                        event.setCanceled(true);
                }
            }
            profilerChatReceive.next("idle");
        }
    }
    
    // When the client joins a server
    @SubscribeEvent
    public void onJoinServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        ChatUtils.print("§a§lTTC has a Discord server: https://discord.gg/2WsVCQDpwy!");
        
        tps = 20.0f;
        lastTick = -1;
        joinTime = new Date().getTime();
    
        ModuleEventRegistry.onNewPlayer();
        
        // Check for a new version
        ThreadManager.run(() -> {
            try {
                Thread.sleep(10000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                e.printStackTrace(ChatUtils.chatPrinterDebug());
            }
            while (TTCp.mc.world != null) {
                String s = Utils.getLatestVersion();
                if (s == null) {
                    ChatUtils.print("Unable to check for a new version! Check your connection!");
                } else if (!s.equals(TTCp.VERSION)) {
                    ChatUtils.print(
                            "§a§lA new TTCp version was found! Current: " +
                            TTCp.VERSION +
                            ", New: " +
                            s
                    );
                }
                try {
                    for (int i = 0; i < 60; i++) {
                        Thread.sleep(1000);
                        if(i % 5 == 0) {
                            try {
                                ServerData serverData = TTCp.mc.getCurrentServerData();
                                if (serverData != null) {
                                    new Thread(() -> {
                                        long[] ping = Utils.getPingToServer(serverData);
                                        if(ping[0] != -1)
                                            EventHandler.ping = ping;
                                    }).start();
                                }
                                else {
                                    EventHandler.ping = new long[]{0,1,1};
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if(TTCp.mc.world == null)
                            break;
                    }
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                    e.printStackTrace(ChatUtils.chatPrinterDebug());
                }
            }
        });
    }
    
    // When any entity appears on screen, useful for setting player and world
    @SubscribeEvent
    public void onJoin(EntityJoinWorldEvent event) {
        // Setting player and world
        TTCp.player = Minecraft.getMinecraft().player;
        TTCp.world = Minecraft.getMinecraft().world;
    }
    
    // When the player dies, NOT called by FML
    public void onDeath(EntityPlayer player) {
        if(TPAParty.getInstance().disableOnDeath) {
            TPAParty.getInstance().enabled = false;
            TPAParty.getInstance().onDisable();
        }
        AutoCrystal.getInstance().enabled = false;
        AutoCrystal.getInstance().onDisable();
        KillAura.getInstance().enabled = false;
        KillAura.getInstance().onDisable();
        if(Freecam.getInstance().enabled) {
            Freecam.getInstance().enabled = false;
            Freecam.getInstance().onDisable();
        }
    
        ModuleEventRegistry.onNewPlayer();
        BlockPos pos = player.getPosition();
        ChatUtils.print("§c§l§k|||§c§l You died at " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }
    
    boolean allowHUDRender = false;
    
    @SubscribeEvent
    public void onHUDRender(RenderGameOverlayEvent.Post event) {
        synchronized (profilerRenderHUD) {
            profilerRenderHUD.next("render");
            if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
                if (allowHUDRender) {
                    allowHUDRender = false;
                    HUD.getInstance().renderHUD();
                }
            }
            profilerRenderHUD.next("idle");
        }
    }
    
    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        allowHUDRender = true;
    }
    
    @SubscribeEvent
    public void onOverlay(RenderBlockOverlayEvent event) {
        event.setCanceled(true);
    }
    
    // Fired every tick
    @SubscribeEvent
    public void onSubTick(TickEvent event) {
        try {
            synchronized (profilerTicks) {
                if (TTCp.mc.world == null || TTCp.mc.player == null)
                    return;
                EntityPlayerSP player = TTCp.player;
                if (player == null || event.side == Side.SERVER)
                    return;
    
                for (int i = 0 ; i < TTCp.modules.length ; i++) {
                    TTCp.modules[i].player = player;
                    profilerTicks.next("Tick " + TTCp.modules[i]);
                    if (TTCp.modules[i].enabled)
                        try {
                            TTCp.modules[i].onSubTick();
                        }
                        catch (Exception e) {
                            e.printStackTrace(ChatUtils.chatPrinterDebug());
                        }
                    TTCp.modules[i].onEverySubTick();
                }
                profilerTicks.next("idle");
            }
        } catch (Exception ignored) { }
    }
    
    // Fired every tick
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        try {
            synchronized (profilerTicks) {
                if (TTCp.mc.world == null || TTCp.mc.player == null)
                    return;
    
                if (event.phase != TickEvent.Phase.START)
                    return;
    
                if (event.type != TickEvent.Type.CLIENT)
                    return;
                
    
                EntityPlayerSP player = TTCp.player;
                if (player == null || event.side == Side.SERVER)
                    return;
                
                profilerTicks.next("TPS");
                long time = System.currentTimeMillis();
                long diff = time - lastTick;
                float f = ((1000f / diff) * 20f);
                if(f < tps - 2) {
                    tps = (tps + f) / 2;
                }
                profilerTicks.next("KillSwitchCheck");
                if(KillSwitch.running && !KillSwitch.lock.isLocked())
                    throw new RuntimeException("KillSwitch triggered!");
                
                profilerTicks.next("DeathCheck");
                if (player.getHealth() <= 0) {
                    if (!isDead) {
                        isDead = true;
                        // >:(
                        onDeath(player);
                    }
                }
                else {
                    isDead = false;
                }
                profilerTicks.next("ParticleLoop");
                ParticleLoop.run();
                for (int i = 0 ; i < TTCp.modules.length ; i++) {
                    TTCp.modules[i].player = player;
                    profilerTicks.next("Keybinds");
                    TTCp.modules[i].key.onTick();
        
                    try {
                        for (String key : TTCp.modules[i].customKeyBinds.keys()) {
                            if (TTCp.modules[i].enabled || TTCp.modules[i].customKeyBinds.get(key).alwaysOn) {
                                TTCp.modules[i].customKeyBinds.get(key).onTick();
                            }
                        }
                        profilerTicks.next("Tick " + TTCp.modules[i]);
                        if (TTCp.modules[i].enabled) {
                            TTCp.modules[i].onTick();
                        }
                        TTCp.modules[i].onEveryTick();
                    }
                    catch(NullPointerException ignored) {
                    
                    }
                    catch (Exception e) {
                        e.printStackTrace(ChatUtils.chatPrinterDebug());
                    }
                }
                profilerTicks.next("idle");
            }
        } catch (Exception ignored) { }
    }
}
