package de.tudbut.mod.client.ttcp.gui.lib.component;

import de.tudbut.mod.client.ttcp.utils.Module;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

public class EnumButton extends Component {
    String field;
    Module module;
    Class<? extends Enum<?>> enumType;
    Enum<?>[] enums;
    
    {green = true;}
    
    public EnumButton(Class<? extends Enum<?>> enumType, String s, Module module, String field) {
        this.enumType = enumType;
        enums = enumType.getEnumConstants();
        this.text = s;
        this.module = module;
        this.field = field;
        for (int i = 0 ; i < enums.length ; i++) {
            Button button;
            int finalI = i;
            subComponents.add(button = new Button(enums[i].toString(), it -> {
                field(module, field, finalI);
                for (Component component : subComponents) {
                    component.green = false;
                }
                it.green = true;
            }));
            button.green = field(module, field) == enums[i].ordinal();
        }
    }
    
    @Override
    public void click(int x, int y, int mouseButton) {
        super.click(x, y, mouseButton);
        green = true;
        
    }
    
    private int field(Module m, String s) {
        try {
            Field f = m.getClass().getDeclaredField(s);
            f.setAccessible(true);
            return ((Enum<?>) f.get(m)).ordinal();
        }
        catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void field(Module m, String s, int o) {
        try {
            Field f = m.getClass().getDeclaredField(s);
            f.setAccessible(true);
            f.set(m, enumType.getEnumConstants()[o]);
        }
        catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
