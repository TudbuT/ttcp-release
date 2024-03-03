package de.tudbut.mod.client.ttcp.mods.rendering;

import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Render;

import java.util.ArrayList;
import java.util.Date;

@Render
public class Notifications extends Module {
    public static class Notification {
        public String text;
        private final int time;
        private final long start = new Date().getTime();
        
        public Notification(String text) {
            this(text, 5000);
        }
    
        public Notification(String text, int ms) {
            this.text = text;
            this.time = ms;
        }
        
    }
    
    // Placeholder module with a few vars, usage in GuiTTCIngame
    
    private static final ArrayList<Notification> notifications = new ArrayList<>();
    
    @Override
    public void onTick() {
        for (int i = 0; i < notifications.size(); i++) {
            if(new Date().getTime() - notifications.get(i).start >= notifications.get(i).time) {
                notifications.remove(i);
                i--;
            }
        }
    }
    
    public static ArrayList<Notification> getNotifications() {
        //noinspection unchecked yes this works ffs!
        return (ArrayList<Notification>) notifications.clone();
    }
    
    public static void add(Notification notification) {
        notifications.add(0, notification);
    }
}
