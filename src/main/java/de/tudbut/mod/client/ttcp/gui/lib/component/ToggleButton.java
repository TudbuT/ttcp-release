package de.tudbut.mod.client.ttcp.gui.lib.component;

import de.tudbut.mod.client.ttcp.utils.Module;

import java.lang.reflect.Field;

public class ToggleButton extends Component {
    
    String field;
    Module module;
    private Runnable lambda;
    
    public ToggleButton(String s, Module module, String field) {
        this.text = s;
        this.module = module;
        this.field = field;
        update();
    }
    public ToggleButton(String s, Module module, String field, Runnable lambda) {
        this.lambda = lambda;
        this.text = s;
        this.module = module;
        this.field = field;
        update();
    }
    
    @Override
    public synchronized void update() {
        green = field(module, field);
    }
    
    @Override
    public synchronized void click(int x, int y, int mouseButton) {
        super.click(x, y, mouseButton);
        field(module, field, green);
        if(lambda != null)
            lambda.run();
    }
    
    public interface ClickEvent {
        void click(Button it);
    }
    
    private static Boolean field(Module m, String s) {
        try {
            Field f = m.getClass().getDeclaredField(s);
            f.setAccessible(true);
            return (Boolean) f.get(m);
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
