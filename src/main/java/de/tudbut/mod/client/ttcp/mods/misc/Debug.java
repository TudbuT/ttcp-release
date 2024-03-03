package de.tudbut.mod.client.ttcp.mods.misc;

import de.tudbut.debug.DebugProfiler;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.DebugProfilerAdapter;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Misc;

import java.io.PrintStream;

@Misc
public class Debug extends Module {
    static Debug instance;
    
    public Debug() {
        instance = this;
    }
    
    public static Debug getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        for (DebugProfilerAdapter profiler : TTCp.getProfilers()) {
            profiler.fallthrough = false;
        }
    }
    @Override
    public void onDisable() {
        for (DebugProfilerAdapter profiler : TTCp.getProfilers()) {
            profiler.fallthrough = true;
        }
    }

    @Override
    public void init() {
        for (DebugProfilerAdapter profiler : TTCp.getProfilers()) {
            profiler.fallthrough = !enabled;
        }
    }

    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public void onSubTick() {
    
    }
    
    @Override
    public void onEveryChat(String s, String[] args) {
        PrintStream out = ChatUtils.chatPrinter();
        DebugProfiler[] profilers = TTCp.getProfilers();
        for (int i = 0 ; i < profilers.length ; i++) {
            out.println(profilers[i].toString());
        }
    }
}
