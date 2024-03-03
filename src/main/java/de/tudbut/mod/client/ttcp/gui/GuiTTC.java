package de.tudbut.mod.client.ttcp.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.mods.rendering.ClickGUI;
import de.tudbut.mod.client.ttcp.utils.Module;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class GuiTTC extends GuiScreen {
    
    // The buttons to be rendered (sub buttons are in the button object)
    // One button per module
    private Button[] buttons;
    
    public static void loadClass() throws NoSuchFieldException, IllegalAccessException {
        "This does nothing except load the class".toString();
        TTCp.class.getDeclaredField("guiLoaded").set(null, true);
    }
    
    // Theme
    public interface ITheme {
        int getGreenColor();
        int getRedColor();
        int getFrameColor();
        int getBackgroundColor();
    }
    public enum Theme implements ITheme {
        TTC(0xff80ff00, 0xff008800),
        ETERNAL_BLUE(0xff4040ff, 0xffff0000, 0xffffffff, 0xff000030),
        DARK(0xff008000, 0xff800000, 0xff808080, 0xff000000),
        LIGHT(0xffcccccc, 0xff999999),
        BLOOD(0xffaa0000, 0xff880000, 0xff00ffff, 0xaaaaaaaa),
        SKY(0xff00cccc, 0xff009999, 0x000000),
        KAMI_BLUE(0xbb353642, 0xbb353642, 0xffbbbbbb, 0xaaaaaaaa),
        SCHLONGHAX(0xbb553662, 0xbb553662, 0xffbbbbbb, 0xaaaaaaaa),
        ORANGE(0xffcc8000, 0xff996000, 0xff404040),
        XV11(0xff3f718e, 0xff2d2d2d, 0xff67915f, 0xff000000),
        TTC_OLD(0xff00ff00, 0xffff0000),
        SOBERSHULKER(0xffff88ff, 0xffaa40aa, 0xffff88ff, 0xff000000),
        VIRUS(0xffc0ddff, 0xffffffff, 0x00000000, 0xaa202040),
        
        ;
    
        @Override
        public int getGreenColor() {
            return greenColor;
        }
    
        @Override
        public int getRedColor() {
            return redColor;
        }
    
        @Override
        public int getFrameColor() {
            return frameColor;
        }
        
        @Override
        public int getBackgroundColor() {
            return backgroundColor;
        }
    
        public final int greenColor;
        public final int redColor;
        public final int frameColor;
        public final int backgroundColor;
    
        Theme(int greenColor, int redColor) {
            this.greenColor = greenColor;
            this.redColor = redColor;
            this.frameColor = 0xffffffff;
            this.backgroundColor = 0xA0000000;
        }
        Theme(int greenColor, int redColor, int frameColor) {
            this.greenColor = greenColor;
            this.redColor = redColor;
            this.frameColor = frameColor;
            this.backgroundColor = 0xA0000000;
        }
        Theme(int greenColor, int redColor, int frameColor, int backgroundColor) {
            this.greenColor = greenColor;
            this.redColor = redColor;
            this.frameColor = frameColor;
            this.backgroundColor = backgroundColor;
        }
    }
    
    // The mouse X and Y
    private int cx;
    private int cy;
    private int lastScrollPos = Mouse.getEventDWheel();
    
    public GuiTTC() {
        this.mc = TTCp.mc;
    }
    
    // Minecraft wants this
    @Override
    public boolean doesGuiPauseGame() {
        return mc.player.timeInPortal != 0;
    }
    
    // The initiator, this can, for some reason, not be in the constructor
    public void initGui() {
        // Creates buttons
        buttons = new Button[256];
        resetButtons();
        
        // Minecraft wants this
        super.buttonList.clear();
        super.buttonList.add(new GuiButton(0, -500, -500, ""));
        super.initGui();
        lastScrollPos = Mouse.getEventDWheel();
    }
    
    // When ESC is pressed
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        ClickGUI.getInstance().enabled = false;
    }
    
    // Called every tick, idk why its called update tho
    @Override
    public void updateScreen() {
        // Minecraft is stupid and sometimes forgets to call initScreen, so this is needed
        while (buttons == null) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (buttons == null)
                resetButtons();
        }
        // Call onTick on every button
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null)
                buttons[i].onTick(this);
        }
    }
    
    // Reset the buttons array
    public void resetButtons() {
        Button[] buttons = new Button[TTCp.modules.length];
        for (int i = 0, j = 0; i < TTCp.modules.length; i++) {
            int x = j / 15;
            int y = j - x * 15;
            
            // Don't add the button if it isn't requested
            if (!TTCp.modules[i].displayOnClickGUI())
                continue;
            
            // Create the button
            int r = i;
            Button b = new Button(
                    10 + (155 * x), 10 + (y * 25), TTCp.modules[r].toString() + ": " + TTCp.modules[r].enabled,
                    (text) -> {
                        if (TTCp.modules[r].enabled = !TTCp.modules[r].enabled)
                            TTCp.modules[r].onEnable();
                        else
                            TTCp.modules[r].onDisable();
                        
                    }, TTCp.modules[i]
            );
            buttons[i] = b;
            
            j++;
        }
        this.buttons = buttons;
    }
    
    // Reset text on the buttons
    private void updateButtons() {
        while (buttons == null) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (buttons == null)
                resetButtons();
        }
        for (int i = 0; i < TTCp.modules.length; i++) {
            if (buttons[i] != null)
                buttons[i].text.set(TTCp.modules[i].toString() + ": " + TTCp.modules[i].enabled);
        }
    }
    
    // Called when the user presses a mouse button
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        
        // Update cx and cy
        cx = mouseX;
        cy = mouseY;
        
        // Notify buttons
        for (int i = 0 ; i < buttons.length ; i++) {
            Button button = buttons[i];
            if (button != null)
                if (button.mouseClicked(mouseX, mouseY, mouseButton))
                    return;
        }
    }
    
    
    
    // Update cx and cy
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        cx = mouseX;
        cy = mouseY;
    }
    
    // Called when the user releases a mouse button
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        
        // Update cx and cy
        cx = mouseX;
        cy = mouseY;
        
        // Notify buttons
        for (int i = 0 ; i < buttons.length ; i++) {
            Button button = buttons[i];
            if (button != null)
                button.mouseReleased();
        }
    }
    
    // Render the screen
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateButtons();
        
        this.drawDefaultBackground();
        
        cx = mouseX;
        cy = mouseY;
        
        // Ask the buttons to render themselves
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null)
                buttons[i].draw(this);
        }
        
        // TMP fix for a strange bug that causes the mouse to be hidden
        if (ClickGUI.getInstance().mouseFix) {
            drawRect(mouseX - 2, mouseY - 2, mouseX + 2, mouseY + 2, 0xffffffff);
        }
        int m = -Mouse.getDWheel();
        if(m != 0) {
            for (int i = 0; i < buttons.length; i++) {
                if(buttons[i] != null) {
                    int d = (lastScrollPos - m) / 3;
                    switch (ClickGUI.getInstance().sd) {
                        case Vertical:
                            buttons[i].y += d;
                            break;
                        case Horizontal:
                            buttons[i].x += d;
                            break;
                    }
                }
            }
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    
    
    public static class Button {
        public int x, y;
        public AtomicReference<String> text;
        // Color for rendering
        public int color = 0x8000ff00;
        // The associated module, can be null if it is a sub button
        public Module module;
        // Called when the button is clicked
        ButtonClickEvent event;
        // If any mouse button is pressed
        private boolean mouseDown = false;
        // The mouse button that is pressed
        private int mouseDownButton = 0;
        // The sub buttons of the button, null if no module is associated to provide them
        private Button[] subButtons;
        
        private boolean display = true;
        
        // Constructor used for sub buttons
        public Button(String text, ButtonClickEvent event) {
            this(0, 0, text, event, null);
        }
        
        // Constructor used by GuiTTC to construct a button with an associated module
        // and main constructor
        public Button(int x, int y, String text, ButtonClickEvent event, Module module) {
            if (module != null) {
                if (module.clickGuiX != null && module.clickGuiY != null) {
                    x = module.clickGuiX;
                    y = module.clickGuiY;
                }
                subButtons = module.subButtons.toArray(new Button[0]);
                display = module.displayOnClickGUI();
            }
            this.x = x;
            this.y = y;
            this.text = new AtomicReference<>(text);
            this.event = event;
            this.module = module;
            if(ClickGUI.getInstance() != null)
                this.color = ClickGUI.getInstance().getTheme().getGreenColor();
        }
        
        // Render the button
        public void draw(GuiTTC gui) {
            if (!display)
                return;
    
            int color = this.color;
    
            if (gui.cx >= x && gui.cy >= y && gui.cx <= x + 150 && gui.cy <= y + ySize()) {
                Color c = new Color(color, true);
                int r, g, b, a;
                r = c.getRed();
                g = c.getGreen();
                b = c.getBlue();
                a = c.getAlpha();
                r += 0x20;
                g += 0x20;
                b += 0x20;
                a += 0x20;
                color = new Color(Math.min(r, 0xff),Math.min(g, 0xff),Math.min(b, 0xff),Math.min(a, 0xff)).getRGB();
            }
            
            drawRect(x, y, x + 150, y + ySize(), color);
            //gui.fontRenderer.drawString(text.get(), x + 6, y + ySize() / 2f - 8 / 2f, ClickGUI.getInstance().getTheme().getFrameColor(), ClickGUI.getInstance().getTheme().hasShadow());
            
            // Draw sub buttons
            if (module != null && (module.enabled ^ module.clickGuiShow)) {
                //subButtons = module.getSubButtons();
                
                for (int i = 0; i < subButtons.length; i++) {
                    Button b = subButtons[i];
                    if(b != null) {
                        b.x = x;
                        b.y = y + ( ( i + 1 ) * 15 + ( 20 - 15 ) );
                        b.color = ClickGUI.getInstance().getTheme().getRedColor();
                        b.draw(gui);
                    }
                }
            }
        }
        
        public int ySize() {
            return module == null ? 15 : 20;
        }
        
        public boolean mouseClicked(int clickX, int clickY, int button) {
            if (clickX >= x && clickY >= y) {
                if (clickX < x + 150 && clickY < y + ySize()) {
                    mouseDown = true;
                    if(ClickGUI.getInstance().flipButtons) {
                        button = (button == 0 ? 1 : (button == 1 ? 0 : button));
                    }
                    mouseDownButton = button;
                    click(button);
                    return true;
                }
            }
            if (module != null && (module.enabled ^ module.clickGuiShow)) {
                //subButtons = module.getSubButtons();
                
                for (int i = 0; i < subButtons.length; i++) {
                    Button b = subButtons[i];
                    if(b != null) {
                        b.x = x;
                        b.y = y + ( ( i + 1 ) * 15 + ( 20 - 15 ) );
                        b.color = ClickGUI.getInstance().getTheme().getRedColor();
                        if (b.mouseClicked(clickX, clickY, button))
                            return true;
                    }
                }
            }
            return false;
        }
        
        public void mouseReleased() {
            mouseDown = false;
            if (module != null && (module.enabled ^ module.clickGuiShow)) {
                subButtons = module.subButtons.toArray(new Button[0]);
                
                for (int i = 0; i < subButtons.length; i++) {
                    subButtons[i].mouseReleased();
                }
            }
            
        }
        
        // More simple onCLick, only called when the mouse is clicked while on the button
        protected void click(int button) {
            if (button == 0)
                event.run(text);
            if (button == 2 && module != null)
                module.clickGuiShow = !module.clickGuiShow;
        }
        
        protected void onTick(GuiTTC gui) {
            this.color = ClickGUI.getInstance().getTheme().getGreenColor();
            if (module != null) {
                if (mouseDown && mouseDownButton == 1) {
                    x = gui.cx - 150 / 2;
                    y = gui.cy - 10;
                    x = (x / 5) * 5;
                    y = (y / 5) * 5;
                }
                module.clickGuiX = x;
                module.clickGuiY = y;
            }
        }
        
    }
    
    public interface ButtonClickEvent {
        void run(AtomicReference<String> text);
    }
}
