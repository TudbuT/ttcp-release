package de.tudbut.mod.client.ttcp.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.Point;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.lib.GUIManager;
import de.tudbut.mod.client.ttcp.gui.lib.component.Category;
import de.tudbut.mod.client.ttcp.gui.lib.component.Component;
import de.tudbut.mod.client.ttcp.mods.rendering.ClickGUI;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.obj.TLMap;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;

public class GuiRewrite extends GuiScreen {
    
    // The mouse X and Y
    private int cx;
    private int cy;
    private Category[] categories = new Category[0];
    
    public GuiRewrite() {
        this.mc = TTCp.mc;
        ClickGUI clickGUI = TTCp.getModule(ClickGUI.class);
        if(!clickGUI.enabled)
            clickGUI.toggle();
        createComponents();
    }
    
    // Minecraft wants this
    @Override
    public boolean doesGuiPauseGame() {
        return mc.player.timeInPortal != 0;
    }
    
    // The initiator, this can, for some reason, not be in the constructor
    public void initGui() {
        
        // Minecraft wants this
        super.buttonList.clear();
        super.buttonList.add(new GuiButton(0, -500, -500, ""));
        super.initGui();
    }
    
    private void createComponents() {
        ArrayList<Category> categories = new ArrayList<>();
        int y = 10;
        TLMap<Class<? extends Annotation>, Category> map = new TLMap<>();
        for (int i = 0 ; i < TTCp.modules.length ; i++) {
            Module module = TTCp.modules[i];
            if(!module.displayOnClickGUI()) {
                continue;
            }
            Category category;
            if((category = map.get(module.category)) == null) {
                if(category == null) {
                    map.set(module.category, category = new Category() {{
                        text = module.category.getSimpleName();
                    }});
                }
                if(category.location == null) {
                    category.location = new Point(10, y);
                    y += 20;
                }
                categories.add(category);
                category.subComponents.clear();
            }
            category.subComponents.add(module);
        }
        for (int i = 0 ; i < categories.size() ; i++) {
            Category category = categories.get(i);
            Point p = TTCp.categories.get(category.text);
            Boolean b = TTCp.categoryShow.get(category.text);
            if(p == null) {
                TTCp.categories.set(category.text, category.location);
            }
            else {
                category.location = p;
            }
            if(b == null) {
                //noinspection UnnecessaryBoxing,BooleanConstructorCall
                TTCp.categoryShow.set(category.text, new Boolean(category.subComponentsShown));
            }
            else {
                category.subComponentsShown = b;
            }
        }
        this.categories = categories.toArray(new Category[0]);
    }
    
    // When ESC is pressed
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        ClickGUI.getInstance().enabled = false;
        for (Category category : categories) {
            //noinspection UnnecessaryBoxing,BooleanConstructorCall
            TTCp.categoryShow.set(category.text, new Boolean(category.subComponentsShown));
        }
    }
    
    @Override
    public void updateScreen() {
        for (Component value : GUIManager.renderedComponents.values()) {
            value.update();
        }
        GUIManager.update();
    }
    
    
    // Called when the user presses a mouse button
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Update cx and cy
        cx = mouseX;
        cy = mouseY;
        
        GUIManager.click(mouseX, mouseY, mouseButton);
    }
    
    
    
    // Update cx and cy
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        cx = mouseX;
        cy = mouseY;
    
        GUIManager.move(mouseX, mouseY, mouseButton);
    }
    
    // Called when the user releases a mouse button
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        
        // Update cx and cy
        cx = mouseX;
        cy = mouseY;
    }
    
    // Render the screen
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    
        // Update cx and cy
        cx = mouseX;
        cy = mouseY;
        
        GUIManager.renderedComponents = new TLMap<>();
        for (int i = 0 ; i < categories.length ; i++) {
            if(categories[i].location.getY() < -10000) {
                categories[i].location.setY(categories[i].location.getY() + 10000);
            }
            if(categories[i].location.getY() > 10000) {
                categories[i].location.setY(categories[i].location.getY() - 10000);
            }
            categories[i].render();
        }
        
        // TMP fix for a strange bug that causes the mouse to be hidden
        if (ClickGUI.getInstance().mouseFix) {
            drawRect(mouseX - 2, mouseY - 2, mouseX + 2, mouseY + 2, 0xffffffff);
        }
        int m = Mouse.getDWheel();
        if(m != 0) {
            for (int i = 0 ; i < categories.length ; i++) {
                categories[i].location.setY(categories[i].location.getY() + m);
            }
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
