package de.tudbut.mod.client.ttcp.mods.movement;

import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Movement;

@Movement
public class Velocity extends Module {
    
    //boolean gotKB = false;
    
    @SubscribeEvent
    public void onKB(LivingKnockBackEvent event) {
        if(enabled && event.getEntity() == player)
            event.setCanceled(true);
    }
    
    @Override
    public boolean onPacket(Packet<?> packet) {
        return packet instanceof SPacketEntityVelocity;
    }
    
    @Override
    public void onEnable() {
    }
    
    @Override
    public void onSubTick() {
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
}
