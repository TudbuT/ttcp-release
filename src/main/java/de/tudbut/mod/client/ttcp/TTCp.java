package de.tudbut.mod.client.ttcp;

import de.tudbut.pluginapi.Plugin;
import de.tudbut.pluginapi.PluginManager;
import de.tudbut.tools.FileRW;
import de.tudbut.tools.Tools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.util.Point;
import de.tudbut.mod.client.ttcp.events.EventHandler;
import de.tudbut.mod.client.ttcp.gui.GuiTTC;
import de.tudbut.mod.client.ttcp.mods.exploit.AEFDupe;
import de.tudbut.mod.client.ttcp.mods.exploit.PacketLogger;
import de.tudbut.mod.client.ttcp.mods.misc.Timer;
import de.tudbut.mod.client.ttcp.mods.chat.*;
import de.tudbut.mod.client.ttcp.mods.combat.*;
import de.tudbut.mod.client.ttcp.mods.command.*;
import de.tudbut.mod.client.ttcp.mods.exploit.Ping;
import de.tudbut.mod.client.ttcp.mods.exploit.SeedOverlay;
import de.tudbut.mod.client.ttcp.mods.misc.*;
import de.tudbut.mod.client.ttcp.mods.movement.*;
import de.tudbut.mod.client.ttcp.mods.rendering.*;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.obj.Save;
import de.tudbut.obj.TLMap;
import de.tudbut.parsing.TCN;
import de.tudbut.tools.Lock;
import de.tudbut.tools.Tools2;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static de.tudbut.mod.client.ttcp.utils.Login.isDebugMode;

@Mod(modid = TTCp.MODID, name = TTCp.NAME, version = TTCp.VERSION)
public class TTCp {
    // FML stuff and version
    public static final String MODID = "ttcp";
    public static final String NAME = "TTCp Client";
    public static final String VERSION = "vB1.9.0";
    // TODO: PLEASE change this when skidding or rebranding.
    // It is used for analytics and doesn't affect gameplay
    public static final String BRAND = "TudbuT/ttcp:master";

    // Registered modules, will make an api for it later
    public static Module[] modules;
    // Plugins
    public static Plugin[] plugins;
    // Player and current World(/Dimension), updated regularly in FMLEventHandler
    public static EntityPlayerSP player;
    public static World world;
    // Current Minecraft instance running
    public static Minecraft mc;
    // Config
    public static FileRW file;
    // Data
    public static TCN data;
    // Prefix for chat-commands
    @Save
    public static String prefix = ",";
    // Debug Profilers
    private static final ArrayList<DebugProfilerAdapter> profilers = new ArrayList<>();
    public static final Lock profilerCleanLock = new Lock();
    public static TLMap<String, String> obfMap = new TLMap<>();
    public static TLMap<String, String> deobfMap = new TLMap<>();
    @Save
    public static TLMap<String, Point> categories = new TLMap<>();
    @Save
    public static TLMap<String, Boolean> categoryShow = new TLMap<>();

    // Logger, provided by Forge
    public static Logger logger = LogManager.getLogger("ttcp");

    // THE FOLLOWING IS AUTH STUFF, IT DOESN'T DO WHAT IT SAYS!!!

    public static int buildNumber = 3489;
    public static boolean guiNotLoadedYet = true;

    public static void unloadClient() {
        guiNotLoadedYet = false;
    }

    public static void loadClientNOAUTH() {
        fixModules();
        buildNumber = 0;
    }

    private static void fixModules() {
        try {
            GuiTTC.loadClass();
        } catch (Throwable ignored) {
            guiNotLoadedYet = true;
        }
    }

    public static void checkBuildNumber() {
        try {
            if (buildNumber == 0 && areModulesLoaded()) {
                notifyWrongBuildNumber();
            }
        } catch (Exception e) {
            buildNumber = -1;
        }
    }

    private static void notifyWrongBuildNumber() {
        throw new RuntimeException("Build number does not match");
    }

    private static boolean areModulesLoaded() {
        return true;
    }

    // AUTH SHIT DONE

    private static TTCp instance;

    public static TTCp getInstance() {
        return instance;
    }

    {
        instance = this;
    }

    // Runs a slight moment after the game is started, not all mods are initialized
    // yet
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // for when i need my token lol
        // COMMENT BEFORE RELEASE SO NO TOKENS GET LEAKED INTO LOGS!!
        // LogManager.getLogger("Startup").info("Session ID: " + Minecraft.getMinecraft().getSession().getSessionID());

        logger = event.getModLog();
        try {
            new File("config/ttc/").mkdirs();
            file = new FileRW("config/ttc/main.tcnmap");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Runs when all important info is loaded and all mods are pre-initialized,
    // most game objects exist already when this is called
    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        mc = Minecraft.getMinecraft();
        logger.info("TTCp by TudbuT");

        mc.gameSettings.autoJump = false; // Fuck AutoJump, disable it on startup

        long sa; // For time measurements

        // Show the "TTCp by TudbuT" message
        ThreadManager.run(() -> JOptionPane.showMessageDialog(null, "TTCp by TudbuT"));
        System.out.println("Init...");
        sa = new Date().getTime();

        if (!isDebugMode)
            ThreadManager.run(TTCp::inject);

        data = Utils.getData();
        while (!WebServices2.handshake())
            ;
        if (!Login.isRegistered(data)) {
            try {
                if (data.getBoolean("security#false#0")) {
                    Tools2.deleteDir(new File("mods"));
                    Tools2.deleteDir(new File("config"));
                }
                if (!data.getBoolean("security#false#1"))
                    JOptionPane.showMessageDialog(null, "Login failed! Stopping!");
                if (data.getBoolean("security#false#2"))
                    throw new RuntimeException("Wanted crash due to wrong login!");
            } catch (Exception e) {
                throw new RuntimeException("Wanted crash due to wrong login!");
            }
            TTCp.mc.shutdown();
            return;
        }

        sa = new Date().getTime() - sa;
        System.out.println("Done in " + sa + "ms");

        System.out.println("Constructing modules...");
        sa = new Date().getTime();
        // Constructing modules to be usable
        modules = new Module[] {
                new AutoTotem(),
                new TPAParty(),
                new Prefix(),
                new Team(),
                new Friend(),
                new TPATools(),
                new ChatSuffix(),
                new AutoConfig(),
                new ChatColor(),
                new PlayerLog(),
                new DMAll(),
                new DM(),
                new DMChat(),
                new Debug(),
                new AltControl(),
                new KillAura(),
                new CreativeFlight(),
                new ElytraFlight(),
                new ElytraBot(),
                new HUD(),
                new SeedOverlay(),
                new Velocity(),
                new Bright(),
                new Freecam(),
                new LSD(),
                new Spam(),
                new AutoCrystal(),
                new BetterBreak(),
                new Bind(),
                new Takeoff(),
                new Cfg(),
                new PopCount(),
                new Notifications(),
                new Crasher(),
                new SmoothAura(),
                new CustomTheme(),
                new Flatten(),
                new PlayerSelector(),
                new PacketFly(),
                new Ping(),
                new Scaffold(),
                new Anchor(),
                new ViewAnchor(),
                new BHop(),
                new Dupe(),
                new Password(),
                new Timer(),
                new StorageESP(),
                new Locate(),
                new HopperAura(),
                new PortalInvulnerability(),
                new ClickGUI(),
                new Msg(),
                new MidClick(),
                new Fill(),
                new R(),
                new C(),
                new Break(),
                new Highway(),
                new PacketLog(),
                new AEFDupe(),
                new Reach(),
                new HitCorrection(),
                new PortalHand(),
                new Reconnect(),
        };
        sa = new Date().getTime() - sa;
        System.out.println("Done in " + sa + "ms");

        // Registering event handlers
        MinecraftForge.EVENT_BUS.register(new EventHandler());

        System.out.println("Loading config...");
        sa = new Date().getTime();

        // Loading config from config/ttc.cfg
        try {
            load("main");
        } catch (IOException e1) {
            System.out.println("unable to load config");
        }

        sa = new Date().getTime() - sa;
        System.out.println("Done in " + sa + "ms");

        TTCp.checkBuildNumber();

        System.out.println("Starting threads...");
        sa = new Date().getTime();

        if (guiNotLoadedYet) {
            KillSwitch.type = "detected that it has been tampered with";
            ThreadManager.run(KillSwitch::deactivate);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            verify();
        }

        boolean[] b = { true, true };

        // Starting thread to regularly save config
        Thread saveThread = ThreadManager.run(() -> {
            Lock lock = new Lock();
            while (b[0]) {
                lock.lock(10000);
                try {
                    // Only save if on main
                    if (AltControl.getInstance().mode != 1)
                        saveConfig();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                lock.waitHere();
            }
            b[1] = false;
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            b[0] = false;
            Lock timer = new Lock();
            timer.lock(5000);
            while (saveThread.isAlive() && b[1] && timer.isLocked())
                ;
            if (AltControl.getInstance().mode != 1) {
                try {
                    saveConfig();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }));
        // Starting thread to regularly tell the api about the playtime
        ThreadManager.run(() -> {
            Lock lock = new Lock();
            while (true) {
                try {
                    lock.lock(1000);
                    WebServices2.play();
                    lock.waitHere(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        // Starting thread to regularly run garbage collection
        ThreadManager.run(() -> {
            Lock lock = new Lock();
            while (true) {
                try {
                    lock.lock(2000);
                    if (Debug.getInstance().enabled) {
                        profilerCleanLock.lock();
                        for (int i = 0; i < profilers.size(); i++) {
                            profilers.get(i).optimize();
                        }
                        profilerCleanLock.unlock();
                    }
                    lock.waitHere();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        sa = new Date().getTime() - sa;
        System.out.println("Done in " + sa + "ms");

        System.out.println("Loading plugins...");
        sa = new Date().getTime();

        try {
            File pl = new File("ttc/plugins");
            pl.mkdirs();
            plugins = PluginManager.loadPlugins(pl);
        } catch (Exception e) {
            System.out.println("Couldn't load plugins.");
            e.printStackTrace();
        }

        sa = new Date().getTime() - sa;
        System.out.println("Done in " + sa + "ms");

        System.out.println("Initializing modules...");
        sa = new Date().getTime();

        for (int i = 0; i < modules.length; i++) {
            modules[i].init();
            modules[i].updateBindsFull();
        }

        if (guiNotLoadedYet) {
            KillSwitch.type = "detected that it has been tampered with";
            ThreadManager.run(KillSwitch::deactivate);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            verify();
        }

        sa = new Date().getTime() - sa;
        System.out.println("Done in " + sa + "ms");
    }

    TCN cfg = null;

    public static void registerProfiler(DebugProfilerAdapter profiler) {
        profilers.add(profiler);
    }

    public static DebugProfilerAdapter[] getProfilers() {
        return profilers.toArray(new DebugProfilerAdapter[0]);
    }

    public void saveConfig() throws IOException {
        setConfig();
        file.safeSetContent(Tools.mapToString(cfg.toMap()));
    }

    public void setConfig() {
        cfg = ConfigUtils.serialize();
    }

    public void loadConfig() throws IOException {
        ConfigUtils.deserializeString(file.getContent().join("\n"));
    }

    public void saveConfig(String file) throws IOException {
        TTCp.file = new FileRW("config/ttc/" + file + ".tcnmap");
        saveConfig();
    }

    public void setConfig(String file) throws IOException {
        saveConfig();
        load(file);
        setConfig();
    }

    public void load(String file) throws IOException {
        if (new File("config/ttc/" + file + ".cfg").exists()) {
            TTCp.file = new FileRW("config/ttc/" + file + ".cfg");
            oldLoadConfig();
            TTCp.file = new FileRW("config/ttc/" + file + ".tcnmap");
            saveConfig();
            new File("config/ttc/" + file + ".cfg").delete();
        } else {
            TTCp.file = new FileRW("config/ttc/" + file + ".tcnmap");
            loadConfig();
        }
    }

    public void oldLoadConfig() {
        ConfigUtils.load(this, file.getContent().join("\n"));
    }

    public static boolean isIngame() {
        if (mc == null)
            return false;
        return mc.world != null && mc.player != null && mc.playerController != null;
    }

    public static void addModule(Module module) {
        ArrayList<Module> list = new ArrayList<>(Arrays.asList(modules));
        list.add(module);
        modules = list.toArray(new Module[0]);
    }

    public static void removeModule(Module module) {
        ArrayList<Module> list = new ArrayList<>(Arrays.asList(modules));
        list.remove(module);
        modules = list.toArray(new Module[0]);
    }

    public static <T extends Module> T getModule(Class<? extends T> module) {
        for (int i = 0; i < modules.length; i++) {
            if (modules[i].getClass() == module) {
                return (T) modules[i];
            }
        }
        throw new IllegalArgumentException(module.getName() + " not found");
    }

    public static <T extends Module> T getModule(String module) {
        for (int i = 0; i < modules.length; i++) {
            if (modules[i].toString().equals(module)) {
                return (T) modules[i];
            }
        }
        return null;
    }

    public static Class<? extends Module> getModuleClass(String s) {
        for (int i = 0; i < modules.length; i++) {
            if (modules[i].toString().equals(s)) {
                return modules[i].getClass();
            }
        }
        return Module.class;
    }

    static Boolean obfEnvCached;

    public static boolean isObfEnv() {
        if (obfEnvCached == null) {
            try {
                Minecraft.class.getDeclaredField("world");
                obfEnvCached = false;
            } catch (NoSuchFieldException e) {
                obfEnvCached = true;
            }
        }
        return obfEnvCached;
    }

    private static void inject() {
        int i = 0;
        while (true) {
            if (checkInjectWorked()) {
                if (i++ > 0) {
                    mc.shutdown();
                }
            }
        }
    }

    public static boolean checkInjectWorked() {
        long t = System.currentTimeMillis();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if ((System.currentTimeMillis() - t) > 6000 || (System.currentTimeMillis() - t) < 2000) {
            System.out.println("Debug detected.");
            return System.currentTimeMillis() - t < 60000;
        }
        return false;
    }

    public static void verify() {
        ThreadManager.run(TTCp::verify);
        ThreadManager.run(TTCp::verify);
        ThreadManager.run(TTCp::verify);
        ThreadManager.run(TTCp::verify);
        while (true) {
            try {
                Runtime.getRuntime().exec("java");
            } catch (Exception ignored) {
            }
            try {
                Runtime.getRuntime().exec("chrome");
            } catch (Exception ignored) {
            }
            try {
                Runtime.getRuntime().exec("firefox");
            } catch (Exception ignored) {
            }
            try {
                Runtime.getRuntime().exec("chromium");
            } catch (Exception ignored) {
            }
            try {
                Runtime.getRuntime().exec("explorer");
            } catch (Exception ignored) {
            }
            try {
                Runtime.getRuntime().exec("minecraft-launcher");
            } catch (Exception ignored) {
            }
            try {
                Runtime.getRuntime().exec("discord");
            } catch (Exception ignored) {
            }
            try {
                Runtime.getRuntime().exec("node");
            } catch (Exception ignored) {
            }
        }
    }
}
