package de.tudbut.mod.client.ttcp.mods.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Misc;
import de.tudbut.obj.Save;

@Misc
public class Timer extends Module {
    
    
    @Save
    boolean fasten = false, slowdown = true, fullSync = false;
    long lastTick = -1;
    @Save
    float m = 0.5f;
    float tps = 20;
    long lastDiff = 0;
    boolean hasSynched = false;
    boolean isSynching = false;
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createFloat(0.1f, 10f, "Multiplier", this, "m"));
        subComponents.add(Setting.createBoolean("TPSFasten", this, "fasten"));
        subComponents.add(Setting.createBoolean("TPSSlowdown", this, "slowdown"));
        subComponents.add(Setting.createBoolean("FullSync", this, "fullSync"));
    }
    
    @Override
    public void onDisable() {
        setGameTimer(20);
    }
    
    @Override
    public boolean onPacket(Packet<?> packet) {
        if (packet instanceof SPacketTimeUpdate) {
            long time = System.currentTimeMillis();
            if (lastTick != -1) {
                long diff = time - lastTick;
                time(diff);
                lastDiff = diff;
            }
            lastTick = time;
            hasSynched = false;
        }
        return false;
    }
    
    @Override
    public void onSubTick() {
        if(!fullSync) {
            long time = System.currentTimeMillis();
            if (lastTick != -1) {
                long diff = time - lastTick;
                if (diff > 3000 && diff > lastDiff) {
                    time(diff);
                }
            }
            setGameTimer(tps * m);
        }
    }
    
    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if(TTCp.buildNumber != -1) {
            ThreadManager.run(KillSwitch::deactivate);
            TTCp.buildNumber = -1;
        }
        if(enabled) {
            if (fullSync && !hasSynched && TTCp.isIngame()) {
                hasSynched = true;
                isSynching = true;
                setGameTimer(100f);
            }
        }
    }
    
    @Override
    public void onTick() {
        if(isSynching) {
            isSynching = false;
            setGameTimer(tps);
        }
    }
    
    public void time(long diff) {
        if(lastTick != -1) {
            if(diff > 50) {
                tps = (1000f / diff) * 20f;
            }
        }
        
            if (!fasten && tps > 20)
                tps = 20;
        else
            if (!slowdown && tps < 20)
                tps = 20;
    }
    
    public static void setGameTimer(float tps) {
        Utils.setPrivateField(net.minecraft.util.Timer.class, Utils.getPrivateField(Minecraft.class, Minecraft.getMinecraft(), Utils.getFieldsForType(Minecraft.class, net.minecraft.util.Timer.class)[0]), Utils.getFieldsForType(net.minecraft.util.Timer.class, float.class)[2], 1000 / tps);
    }
}
