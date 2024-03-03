package de.tudbut.mod.client.ttcp.utils;

import org.lwjgl.input.Keyboard;
import de.tudbut.mod.client.ttcp.gui.lib.component.*;

import java.lang.reflect.Field;

public class Setting {
    
    public static Component createInt(int min, int max, String string, Module module, String field, Runnable onClick) {
        return new IntSlider(
                string,
                module,
                field,
                String::valueOf,
                max - min,
                min,
                it -> {
                    onClick.run();
                    return true;
                }
        );
    }
    
    public static Component createEnum(Class<? extends Enum<?>> theEnum, String string, Module module, String field, Runnable onClick) {
        return new EnumButton(theEnum, string, module, field);
    }
    
    public static Component createInt(int min, int max, String string, Module module, String field) {
        return Setting.createInt(min, max, string, module, field, () -> {});
    }
    
    public static Component createEnum(Class<? extends Enum<?>> theEnum, String string, Module module, String field) {
        return Setting.createEnum(theEnum, string, module, field, () -> {});
    }
    
    public static Component createFloat(float min, float max, String string, Module module, String field) {
        return new Slider(
                string,
                module,
                field,
                it -> String.valueOf(Math.round(it * 100f) / 100f),
                max - min,
                min,
                it -> true
        );
    }
    
    public static Component createBoolean(String string, Module module, String field) {
        return new ToggleButton(
                string,
                module,
                field
        );
    }
    
    public static Component createKey(String string, Module.KeyBind keyBind) {
        return new Button(
                string + ": " + (keyBind.key == null ? "NONE" : Keyboard.getKeyName(keyBind.key)),
                it -> {
                    int i;
                    if ((i = getKeyPress()) != -1) {
                        keyBind.key = i;
                        it.text = string + ": " + (Keyboard.getKeyName(keyBind.key));
                    }
                    else {
                        keyBind.key = null;
                        it.text = string + ": " + ("NONE (Hold)");
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            it.text = string + ": " + ("NONE");
                        }).start();
                    }
                }
        );
    }
    
    private static int getKeyPress() {
        for (int i = 0 ; i < 256 ; i++) {
            if(Keyboard.isKeyDown(i))
                return i;
        }
        return -1;
    }
    
    private static Object field(Module m, String s) {
        try {
            Field f = m.getClass().getDeclaredField(s);
            f.setAccessible(true);
            return f.get(m);
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
