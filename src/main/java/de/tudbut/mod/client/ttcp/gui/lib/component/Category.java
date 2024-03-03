package de.tudbut.mod.client.ttcp.gui.lib.component;

import de.tudbut.mod.client.ttcp.gui.lib.GUIManager;
import net.minecraft.client.gui.Gui;
import org.lwjgl.util.Point;
import org.lwjgl.util.Rectangle;
import de.tudbut.obj.Transient;

import java.util.concurrent.atomic.AtomicInteger;

public class Category extends Component {
    
    {green = true;}
    
    public Point location;
    
    public void render() {
        render(location.getX(), new AtomicInteger(location.getY()), -1, false, 0);
    }
    
    @Override
    public void render(int x, AtomicInteger y, int sub, boolean isLastInList, int yLineSize) {
        loc = new Point(x + 8 + sub * 8, y.get());
        GUIManager.renderedComponents.set(new Rectangle(x + sub * 8, y.get(), x + (200 - sub * 8), y.get() + size()), this);
        int width = fontRenderer.getStringWidth(text);
        Gui.drawRect(x + 2, y.get() + 4, x + 200, y.get() + subSizes() + size(), GUIManager.frameBackground);
        Gui.drawRect(x + 200, y.get() + 4, x + 200 - 1, y.get() + subSizes() + size(), GUIManager.frameColor);
        Gui.drawRect(x + width, y.get() + 4, x + 200, y.get() + 4 + 1, GUIManager.frameColor);
        fontRenderer.drawString(text, x, y.get(), green ? GUIManager.fontColorOn : GUIManager.fontColorOff);
        y.addAndGet(size());
        if(subComponentsShown) {
            for (int i = 0 ; i < subComponents.size() ; i++) {
                Component component = subComponents.get(i);
                component.render(x, y, 0, false, component.size());
            }
        }
        Gui.drawRect(x + 2, y.get(), x + 200, y.get() - 1, GUIManager.frameColor);
    }
    
    @Transient
    int clickX = 0, clickY = 0;
    
    @Override
    public void click(int x, int y, int mouseButton) {
        if(mouseButton == 0) {
            subComponentsShown = !subComponentsShown;
        }
        clickX = x;
        clickY = y;
    }
    
    public void move(int x, int y, int mouseButton) {
        if(mouseButton == 1) {
            location.setX(location.getX() + x - clickX);
            location.setY(location.getY() + y - clickY);
            loc = location;
        }
    }
}
