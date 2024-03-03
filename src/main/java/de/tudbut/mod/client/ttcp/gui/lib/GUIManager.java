package de.tudbut.mod.client.ttcp.gui.lib;

import de.tudbut.mod.client.ttcp.gui.GuiTTC;
import de.tudbut.mod.client.ttcp.gui.lib.component.Component;
import org.lwjgl.util.Rectangle;
import de.tudbut.mod.client.ttcp.mods.rendering.ClickGUI;
import de.tudbut.obj.TLMap;

import java.util.ArrayList;

public class GUIManager {
    
    public static int fontColorOn = 0xff00ff00;
    public static int fontColorOff = 0xffff0000;
    public static int frameColor = 0xffffffff;
    public static int frameBackground = 0xA0000000;
    public static int sliderBackground = 0xff808080;
    public static int sliderColor = 0xffffffff;
    
    public static TLMap<Rectangle, Component> renderedComponents = new TLMap<>();
    
    static Component dragging = null;
    
    public static synchronized void click(int mouseX, int mouseY, int mouseButton) {
        dragging = null;
        ArrayList<TLMap.Entry<Rectangle, Component>> entries = renderedComponents.entries();
        for (int i = 0, entriesSize = entries.size() ; i < entriesSize ; i++) {
            TLMap.Entry<Rectangle, Component> entry = entries.get(i);
            if(mouseX >= entry.key.getX() && mouseY >= entry.key.getY() && mouseX <= entry.key.getWidth() && mouseY <= entry.key.getHeight()) {
                entry.val.click(mouseX - entry.val.loc.getX(), mouseY - entry.val.loc.getY(), mouseButton);
                return;
            }
        }
    }
    
    public static synchronized void move(int mouseX, int mouseY, int mouseButton) {
        if(dragging == null) {
            ArrayList<TLMap.Entry<Rectangle, Component>> entries = renderedComponents.entries();
            for (int i = 0, entriesSize = entries.size() ; i < entriesSize ; i++) {
                TLMap.Entry<Rectangle, Component> entry = entries.get(i);
                if (mouseX >= entry.key.getX() && mouseY >= entry.key.getY() && mouseX <= entry.key.getWidth() && mouseY <= entry.key.getHeight()) {
                    dragging = entry.val;
                    break;
                }
            }
        }
        if(dragging != null) {
            dragging.move(mouseX - dragging.loc.getX(), mouseY - dragging.loc.getY(), mouseButton);
        }
    }
    
    public static void update() {
        GuiTTC.ITheme theme = ClickGUI.getInstance().getTheme();
        fontColorOn = theme.getGreenColor();
        fontColorOff = theme.getRedColor();
        frameColor = theme.getFrameColor();
        frameBackground = theme.getBackgroundColor();
    }
}
