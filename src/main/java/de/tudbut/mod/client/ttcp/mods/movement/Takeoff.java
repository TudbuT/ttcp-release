package de.tudbut.mod.client.ttcp.mods.movement;

import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.FlightBot;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Movement;
import de.tudbut.obj.Atomic;

@Movement
public class Takeoff extends Module {
    
    boolean isTakingOff = false;
    
    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public void onEnable() {
        ChatUtils.print("Starting elytra...");
        isTakingOff = true;
        FlightBot.activate(new Atomic<>(TTCp.mc.player.getPositionVector().add(0, 4, 0)));
        ChatUtils.print("Bot started.");
    }
    
    @Override
    public void onDisable() {
        isTakingOff = false;
        enabled = false;
        FlightBot.deactivate();
    }
    
    @Override
    public void onTick() {
        if(!FlightBot.isFlying() && isTakingOff && TTCp.player.isElytraFlying()) {
            FlightBot.deactivate();
            isTakingOff = false;
            enabled = false;
            onDisable();
            ChatUtils.print("Elytra started.");
        }
    }
    
    @Override
    public void onChat(String s, String[] args) {
    }
}
