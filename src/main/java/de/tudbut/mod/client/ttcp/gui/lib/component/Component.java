package de.tudbut.mod.client.ttcp.gui.lib.component;

import de.tudbut.mod.client.ttcp.gui.GuiTTC;
import de.tudbut.mod.client.ttcp.gui.lib.GUIManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import org.lwjgl.util.Point;
import org.lwjgl.util.Rectangle;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.obj.Save;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Component {
    
    public Point loc;
    
    FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
    public ArrayList<Component> subComponents = new ArrayList<>();
    public String text = "";
    public boolean green = false;
    @Save
    public boolean subComponentsShown = false;
    public boolean displayConfirmation = false;
    private final Button[] confirmationButtons = new Button[3];
    
    {
        if(!(this instanceof Button)) {
            confirmationButtons[0] = new Button("Are you sure?", it -> { });
            confirmationButtons[1] = new Button("Yes", it -> {
                displayConfirmation = false;
                onConfirm(true);
            });
            confirmationButtons[2] = new Button("No", it -> {
                displayConfirmation = false;
                onConfirm(false);
            });
        }
    }
    
    public void render(int x, AtomicInteger y, int sub, boolean isLastInList, int yLineSize) {
        loc = new Point(x + 8 + sub * 8, y.get());
        GUIManager.renderedComponents.set(new Rectangle(x + sub * 8, y.get(), x + (200 - sub * 8), y.get() + size()), this);
        if(isLastInList) {
            Gui.drawRect(x + 2 + sub * 8, y.get(), x + 2 + sub * 8 + 1, y.get() + 4, GUIManager.frameColor);
        }
        else {
            Gui.drawRect(x + 2 + sub * 8, y.get(), x + 2 + sub * 8 + 1, y.get() + yLineSize, GUIManager.frameColor);
        }
        Gui.drawRect(x + 2 + sub * 8, y.get(), x + 2 + sub * 8 + 1, y.get() + subSizes() + (isLastInList ? 5 : size()), GUIManager.frameColor);
        Gui.drawRect(x + 2 + sub * 8, y.get() + 4, x + 5 + sub * 8 + 1, y.get() + 4 + 1, GUIManager.frameColor);
        fontRenderer.drawString(text, x + 8 + sub * 8, y.get(), green ? GUIManager.fontColorOn : GUIManager.fontColorOff);
        draw(x + 8 + sub * 8, y.get());
        y.addAndGet(size());
        if(subComponentsShown) {
            List<Component> subComponents = this.subComponents;
            if(displayConfirmation) {
                subComponents = Arrays.asList(confirmationButtons);
            }
            for (int i = 0 ; i < subComponents.size() ; i++) {
                Component component = subComponents.get(i);
                component.render(
                        x, y, sub + 1,
                        i == subComponents.size() - 1,
                        i == subComponents.size() - 1 && isLastInList && component.subComponents.size() == 0 ? 4 : component.size()
                );
            }
        }
    }
    
    public void draw(int x, int y) {
    
    }
    
    protected int subSizes() {
        int size = 0;
        if(subComponentsShown) {
            if(displayConfirmation)
                return 30;
            for (int i = 0 ; i < subComponents.size() ; i++) {
                size += subComponents.get(i).size() + subComponents.get(i).subSizes();
            }
        }
        return size;
    }
    
    protected int size() {
        return 10;
    }
    
    public void update() { }
    
    public void click(int x, int y, int mouseButton) {
        if(mouseButton == 0) {
            green = !green;
        }
        if(mouseButton == 1 || mouseButton == 2) {
            subComponentsShown = !subComponentsShown;
        }
    }
    
    public void move(int x, int y, int mouseButton) { }
    
    public void onConfirm(boolean result) { }
}
