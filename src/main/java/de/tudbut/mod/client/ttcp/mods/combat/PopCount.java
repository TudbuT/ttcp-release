package de.tudbut.mod.client.ttcp.mods.combat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.mods.rendering.Notifications;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Combat;
import de.tudbut.obj.Save;
import de.tudbut.obj.TLMap;

import java.util.Date;

@Combat
public class  PopCount extends Module {
    public TLMap<EntityPlayer, Counter> counters = new TLMap<>();
    
    @Save
    public boolean autoToxic = false;
    
    @Save
    public boolean countOwn = true;
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new Button("Reset", text -> counters = new TLMap<>()));
        subComponents.add(Setting.createBoolean("AutoToxic", this, "autoToxic"));
        subComponents.add(Setting.createBoolean("CountOwn", this, "countOwn"));
    }
    
    @Override
    public void onEveryTick() {
        try {
            TLMap<EntityPlayer, Counter> counters = this.counters;
            EntityPlayer[] visiblePlayers = TTCp.world.playerEntities.toArray(new EntityPlayer[0]);
    
            EntityPlayer[] players = counters.keys().toArray(new EntityPlayer[0]);
            for (int i = 0; i < visiblePlayers.length; i++) {
                boolean b = false;
                for (int j = 0 ; j < players.length ; j++) {
                    if (counters.get(players[j]).name.equals(visiblePlayers[i].getName())) {
                        counters.get(players[j]).player = visiblePlayers[i];
                        if(players[j] != visiblePlayers[i]) {
                            counters.set(visiblePlayers[i], counters.get(players[j]));
                            counters.set(players[j], null);
                        }
                        b = true;
                    }
                }
                if (!b) {
                    counters.set(visiblePlayers[i], new Counter(visiblePlayers[i]));
                }
            }
    
            players = counters.keys().toArray(new EntityPlayer[0]);
            for (int i = 0; i < players.length; i++) {
                Counter counter = counters.get(players[i]);
                counter.reload(autoToxic, enabled, countOwn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
    
    public static class Counter {
        
        private EntityPlayer player;
        private final String name;
        private int totCountLast = -1;
        private int switches = 0;
        private int pops = 0;
        private boolean autoToxic;
        private long lastPop = 0;
        private float popDelay = 2000f;
    
        public Counter(EntityPlayer player) {
            this.player = player;
            this.name = player.getName();
        }
        
        public void reload(boolean autoToxic, boolean enabled, boolean countOwn) {
            this.autoToxic = autoToxic;
            if (player.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING || player.getHeldItemOffhand().getItem() == Items.AIR) {
                if (totCountLast == -1) {
                    totCountLast = player.getHeldItemOffhand().getCount();
                    lastPop = new Date().getTime();
                }
                reload0(enabled, countOwn);
            }
    
        }
        
        private void reload0(boolean enabled, boolean countOwn) {
            int totCount = player.getHeldItemOffhand().getCount();
            if (totCount > totCountLast) {
                switches++;
                if (totCount != 1) {
                    if (enabled && (countOwn || player.getEntityId() != TTCp.player.getEntityId())) {
                        ChatUtils.printChatAndHotbar("§a§l" + player.getName() + " switched (now " + switches + " switches)");
                        Notifications.add(new Notifications.Notification(player.getName() + " switched (now " + switches + " switches)"));
                    }
                }
            }
            if(totCount < totCountLast) {
                pops += totCountLast - totCount; // Dont just add, add the diff so its not lag-dependent
                if(enabled && (countOwn || player.getEntityId() != TTCp.player.getEntityId())) {
                    ChatUtils.printChatAndHotbar("§a§l" + player.getName() + " popped " + ( totCountLast - totCount ) + " (now " + pops + " pops)");
                    Notifications.add(new Notifications.Notification(player.getName() + " popped " + ( totCountLast - totCount ) + " (now " + pops + " pops)"));
                    if (autoToxic && player != TTCp.player && player.getDistance(TTCp.player) < 10)
                        ChatUtils.simulateSend("EZ pop " + player.getName() + "! TTC on top!", false);
                }
                float timeSinceLastPop = new Date().getTime() - lastPop;
                if(timeSinceLastPop < 8000) {
                    popDelay = (popDelay * 4 + timeSinceLastPop) / 5;
                }
                lastPop = new Date().getTime();
            }
            totCountLast = totCount;
        }
        
        public int getSwitches() {
            return switches;
        }
        
        public int getPops() {
            return pops;
        }
        
        public float getPopDelay() {
            return popDelay;
        }
        
        public float popsPerSecond() {
            return 1000 / popDelay;
        }
        
        public long predictNextPop() {
            return (long) (lastPop + popDelay);
        }
    
        public long predictNextPopDelay() {
            return Math.max(0, (long) (lastPop + popDelay) - new Date().getTime());
        }
        
        public float predictPopProgress() {
            long l = new Date().getTime() - lastPop;
            return Math.min(1, l * 1f / popDelay * 1f);
        }
        
        public boolean isPopping() {
            return new Date().getTime() - lastPop < 8000;
        }
    }
}
