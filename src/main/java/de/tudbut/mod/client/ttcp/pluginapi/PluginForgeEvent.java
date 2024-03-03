package de.tudbut.mod.client.ttcp.pluginapi;

import de.tudbut.pluginapi.PluginEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

public class PluginForgeEvent<T extends Event> extends PluginEvent {

    public final T forgeEvent;
    
    public PluginForgeEvent(T forgeEvent) {
        this.forgeEvent = forgeEvent;
    }
}
