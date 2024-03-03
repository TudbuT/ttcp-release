package de.tudbut.mod.client.ttcp.utils;

import de.tudbut.tools.Tools;
import de.tudbut.debug.DebugProfiler;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.parsing.TCN;
import de.tudbut.tools.ConfigSaverTCN;
import de.tudbut.tools.ConfigSaverTCN2;

public class ConfigUtils {

    public static String serializeString() {
        return Tools.mapToString(serialize().toMap());
    }

    public static void deserializeString(String config) {
        deserialize(TCN.readMap(Tools.stringToMap(config)));
    }

    public static TCN serialize() {
        TCN config = new TCN();
        try {
            config.set("client", ConfigSaverTCN2.write(TTCp.getInstance(), false, true));
            for(Module module : TTCp.modules) {
                try {
                    config.set(module.toString(), ConfigSaverTCN2.write(module, false, true));
                } catch (Throwable e) {
                    throw new Exception(module.toString(), e);
                }
            }
        } catch (Exception e) {
            System.err.println("Unable to serialize TTCp config");
            e.printStackTrace();
        }
        return config;
    }

    public static void deserialize(TCN config) {
        try {
            ConfigSaverTCN2.read(config.getSub("client"), TTCp.getInstance());
            for(Module module : TTCp.modules) {
                try {
                    if(config.get(module.toString()) != null)
                        ConfigSaverTCN2.read(config.getSub(module.toString()), module);
                } catch (Throwable e) {
                    throw new Exception(module.toString(), e);
                }
            }
        } catch (Exception e) {
            System.err.println("Broken TTCp config");
            e.printStackTrace();
        }
    }
    




    public static String make(TTCp ttcp) {
        return Tools.mapToString(makeTCN(ttcp).toMap());
    }
    
    public static TCN makeTCN(TTCp ttcp) {
        TCN tcn = new TCN();
    
        tcn.set("init", "true");
        
        makeClient(ttcp, tcn);
        makeModules(tcn);
        
        return tcn;
    }
    
    private static void makeClient(TTCp ttcp, TCN tcn) {
        try {
            TCN cfg = ConfigSaverTCN.saveConfig(ttcp);
            
            tcn.set("client", cfg);
        } catch (Exception e) {
            System.err.println("Couldn't save config of client");
            e.printStackTrace();
            tcn.set("init", null);
        }
    }
    
    private static void makeModules(TCN tcn) {
        TCN cfg = new TCN();
    
        for (int i = 0; i < TTCp.modules.length; i++) {
            Module module = TTCp.modules[i];

            try {
                module.onConfigSave();
                TCN moduleTCN = ConfigSaverTCN.saveConfig(module);
                cfg.set(module.toString(), moduleTCN);
            } catch (Exception e) {
                System.err.println("Couldn't save config of module " + module.toString());
                e.printStackTrace();
                tcn.set("init", null);
            }
        }
        
        tcn.set("modules", cfg);
    }
    
    public static void load(TTCp ttcp, String config) {
        try {
            System.out.println("Reading as TCNMap...");
            TCN tcn = TCN.readMap(Tools.stringToMap(config));
            if (!tcn.getBoolean("init"))
                throw new Exception();
            System.out.println("Done");
            loadTCN(ttcp, tcn);
        }
        catch (Exception e0) {
            System.err.println("Couldn't load config as TCNMap");
            try {
                System.out.println("Reading as TCN...");
                TCN tcn = TCN.read(config);
                System.out.println("Done");
                loadTCN(ttcp, tcn);
            }
            catch (Exception e1) {
                System.err.println("Couldn't load config");
            }
        }
    }
    
    public static void loadTCN(TTCp ttcp, TCN tcn) {
        loadClient(ttcp, tcn);
        loadModules(tcn);
    }
    
    private static void loadClient(TTCp ttcp, TCN tcn) {
        try {
            ConfigSaverTCN.loadConfig(ttcp, tcn.getSub("client"));
        } catch (Exception e) {
            System.err.println("Couldn't load config of client");
            e.printStackTrace();
        }
    }
    
    private static void loadModules(TCN tcn) {
        tcn = tcn.getSub("modules");
    
        DebugProfiler profiler = new DebugProfiler("ConfigLoadProfiler", "init");
        
        for (int i = 0; i < TTCp.modules.length; i++) {
            Module module = TTCp.modules[i];
            profiler.next(module.toString());

            if(module.enabled) {
                module.enabled = false;
                module.green = false;
                module.onDisable();
            }
            
            try {
                ConfigSaverTCN.loadConfig(module, tcn.getSub(module.toString()));
                try {
                    if (module.enabled)
                        module.onEnable();
                } catch (NullPointerException ignored) { }
                module.onConfigLoad();
            }
            catch (Exception e) {
                module.enabled = module.green = module.defaultEnabled();
                System.err.println("Couldn't load config of module " + module.toString());
                e.printStackTrace();
            }
        }
        
        profiler.endAll();
        System.out.println(profiler.getResults());
        profiler.delete();
    }
}
