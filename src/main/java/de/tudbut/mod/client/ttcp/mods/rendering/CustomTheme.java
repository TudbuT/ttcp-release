package de.tudbut.mod.client.ttcp.mods.rendering;

import de.tudbut.tools.Mouse;
import de.tudbut.tools.Tools;
import de.tudbut.ui.windowgui.RenderableWindow;
import net.minecraft.client.gui.GuiScreen;
import de.tudbut.mod.client.ttcp.gui.GuiTTC;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.gui.lib.component.ToggleButton;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Render;
import de.tudbut.obj.Save;
import de.tudbut.obj.Vector2i;
import de.tudbut.rendering.Maths2D;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

@Render
public class CustomTheme extends Module implements GuiTTC.ITheme {
    
    RenderableWindow window = new RenderableWindow(256 * 2, 256 * 2, "Color picker", 20, false);
    {
        new Thread(() -> {
            window.getWindow().setResizable(false);
            window.getWindow().setSize(256 * 2, 256 * 2);
            try {
                Thread.sleep(3000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            window.getWindow().setSize(256 * 2, 256 * 2);
            window.getWindow().setVisible(false);
            window.getWindow().addWindowListener(new WindowListener() {
                @Override
                public void windowOpened(WindowEvent windowEvent) {
            
                }
        
                @SuppressWarnings("UnusedAssignment") // No, its not
                @Override
                public void windowClosing(WindowEvent windowEvent) {
                    CustomTheme.this.show = false;
                    CustomTheme.this.selectedColor = null;
                    updateBinds();
                }
        
                @Override
                public void windowClosed(WindowEvent windowEvent) {
            
                }
        
                @Override
                public void windowIconified(WindowEvent windowEvent) {
            
                }
        
                @Override
                public void windowDeiconified(WindowEvent windowEvent) {
            
                }
        
                @Override
                public void windowActivated(WindowEvent windowEvent) {
            
                }
        
                @Override
                public void windowDeactivated(WindowEvent windowEvent) {
            
                }
            });
        }).start();
    }
    BufferedImage image0 = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    {
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                image0.setRGB(x, y, new Color(x, y, Math.max((x + y) - 256, 0)).getRGB());
            }
        }
    }
    BufferedImage image1 = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    {
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                image1.setRGB(x, y, new Color(x, Math.max((x+y) - 256, 0), y).getRGB());
            }
        }
    }
    BufferedImage image2 = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    {
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                image2.setRGB(x, y, new Color(Math.max((x+y) - 256, 0), x, y).getRGB());
            }
        }
    }
    BufferedImage image3 = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    {
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                image3.setRGB(x, y, new Color(Math.max((x+y) - 256, 0), Math.min(0xff, x + 0x80), Math.min(0xff, y + 0x80)).getRGB());
            }
        }
    }
    BufferedImage image4 = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    {
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                image4.setRGB(x, y, new Color(Math.min(0xff, x + 0x80), Math.max((x + y) - 256, 0), Math.min(0xff, y + 0x80)).getRGB());
            }
        }
    }
    BufferedImage image5 = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
    {
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                image5.setRGB(x, y, new Color(Math.min(0xff, x + 0x80), Math.min(0xff, y + 0x80), Math.max((x+y) - 256, 0)).getRGB());
            }
        }
    }
    
    BufferedImage[] images;
    
    {
        Vector2i size = window.getSizeOnScreen();
        images = new BufferedImage[] {
                Maths2D.distortImage(image0, 512, 512, 1),
                Maths2D.distortImage(image1, 512, 512, 1),
                Maths2D.distortImage(image2, 512, 512, 1),
                Maths2D.distortImage(image3, 512, 512, 1),
                Maths2D.distortImage(image4, 512, 512, 1),
                Maths2D.distortImage(image5, 512, 512, 1),
        };
    }
    
    int imageID = 0;
    BufferedImage image = images[0];
    
    boolean show = false;
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new Button("Copy theme to clipboard", it -> {
            GuiScreen.setClipboardString(themeString());
        }));
        subComponents.add(new Button("Use theme from clipboard", it -> {
            themeFromString(GuiScreen.getClipboardString());
        }));
        subComponents.add(new Button(show ? "Hide dialog" : "Show dialog", it -> {
            show = !show;
            window.getWindow().setVisible(show);
            if(!show) {
                selectedColor = null;
                updateBinds();
            }
            it.text = show ? "Hide dialog" : "Show dialog";
        }));
        //subComponents.add(new ToggleButton("FontShadow", this, "shadow"));
        if(selectedColor != null) {
            subComponents.add(new Button("Use selection as enabled color", it -> {
                greenColor = selectedColor;
            }));
            subComponents.add(new Button("Use selection as disabled color", it -> {
                redColor = selectedColor;
            }));
            subComponents.add(new Button("Use selection as frame color", it -> {
                frameColor = selectedColor;
            }));
            subComponents.add(new Button("Use selection as background color", it -> {
                backgroundColor = selectedColor;
            }));
        }
    }
    
    public Integer selectedColor = null;
    
    @Save
    public int frameColor = 0xffffffff;
    
    @Save
    public int greenColor = 0x8000ff00;
    
    @Save
    public int redColor = 0x4000ff00;
    
    @Save
    public int backgroundColor = 0xA0000000;
    
    @Save
    public boolean shadow = false;
    
    private String themeString() {
        Map<String, String> map = new HashMap<>();
        
        map.put("a", String.valueOf(greenColor));
        map.put("b", String.valueOf(redColor));
        map.put("c", String.valueOf(frameColor));
        map.put("d", String.valueOf(backgroundColor));
        map.put("e", String.valueOf(shadow));
        
        return Tools.mapToString(map);
    }
    
    private void themeFromString(String s) {
        Map<String, String> map = Tools.stringToMap(s);
        try {
            greenColor = Integer.parseInt(map.get("a"));
            redColor = Integer.parseInt(map.get("b"));
            frameColor = Integer.parseInt(map.get("c"));
            backgroundColor = Integer.parseInt(map.get("d"));
            shadow = Boolean.parseBoolean(map.get("e"));
        } catch (Exception ignored) { }
    }
    
    private boolean mouseWasDown = false;
    
    @Override
    public void onEnable() {
        ClickGUI.getInstance().customTheme = this;
        ClickGUI.getInstance().updateBinds();
    }
    
    @Override
    public void onDisable() {
        ClickGUI.getInstance().customTheme = null;
        ClickGUI.getInstance().updateBinds();
    }
    
    @Override
    public void onEveryTick() {
        if(show) {
            window.getWindow().setAutoRequestFocus(true);
            window.getWindow().setAlwaysOnTop(true);
            window.render((adaptedGraphics, graphics, bufferedImage) -> {
                adaptedGraphics.drawImage(0,0, image);
                if (Mouse.isKeyDown(3)) {
                    if (!mouseWasDown) {
                        mouseWasDown = true;
                        imageID++;
                        if (imageID >= images.length) {
                            imageID = 0;
                        }
            
                        image = images[imageID];
                    }
                }
                else {
                    mouseWasDown = false;
                }
                if (Mouse.isKeyDown(1)) {
                    updateBinds();
                    try {
                        Vector2i loc = window.getMousePos();
                        selectedColor = image.getRGB(loc.getX(), loc.getY());
                    } catch(IndexOutOfBoundsException ignore) { }
                }
            });
            window.prepareRender();
            window.doRender();
            window.swapBuffers();
        }
    }
    
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
}
