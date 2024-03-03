package de.tudbut.mod.client.ttcp.gui.lib.component;

import de.tudbut.mod.client.ttcp.gui.lib.GUIManager;
import de.tudbut.mod.client.ttcp.utils.Module;
import net.minecraft.client.gui.Gui;

import java.lang.reflect.Field;
import java.util.function.Function;

public class Slider extends Component {
    
    public float f = 0;
    String field;
    Module module;
    Function<Float, String> sliderText;
    Function<Float, Boolean> updateMethod;
    float mapper;
    float adder;
    
    {green = true;}
    
    public Slider(String s, Module module, String field, Function<Float, String> text, float mapper, float adder, Function<Float, Boolean> updateMethod) {
        this.text = s;
        this.module = module;
        this.field = field;
        this.sliderText = text;
        this.mapper = mapper;
        this.adder = adder;
        this.updateMethod = updateMethod;
        update();
    }

    public Slider(String s, Module module, String field, Function<Float, String> text, float mapper, float adder) {
        this(s, module, field, text, mapper, adder, t -> true);
    }
    
    @Override
    public void draw(int x, int y) {
        Gui.drawRect(x, y + 13, x + 101, y + 14, GUIManager.sliderBackground);
        Gui.drawRect((int) Math.floor(x + f * 100), y + 11, (int) Math.floor(x + f * 100) + 1, y + 16, GUIManager.sliderColor);
        fontRenderer.drawString(sliderText.apply(f * mapper + adder), x + 100 + 4, y + 10, GUIManager.sliderColor);
    }
    
    @Override
    public synchronized void update() {
        f = (field(module, field) - adder) / mapper;
    }
    
    @Override
    public synchronized void click(int x, int y, int mouseButton) {
        if(mouseButton == 0)
            f = Math.max(Math.min(x, 100), 0) / 100f;
        
        field(module, field, f * mapper + adder);
        if(!updateMethod.apply(f * mapper + adder)) {
            System.out.println("Something went wrong handling the sliders!");
            throw new RuntimeException();
        }
        f = (field(module, field) - adder) / mapper;
    }
    
    @Override
    public void move(int x, int y, int mouseButton) {
        click(x, y, mouseButton);
    }
    
    @Override
    protected int size() {
        return 20;
    }
    
    private static Float field(Module m, String s) {
        try {
            Field f = m.getClass().getDeclaredField(s);
            f.setAccessible(true);
            return (Float) f.get(m);
        }
        catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static void field(Module m, String s, Object o) {
        try {
            Field f = m.getClass().getDeclaredField(s);
            f.setAccessible(true);
            f.set(m, o);
        }
        catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
