package de.tudbut.mod.client.ttcp.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.GuiTTC;
import de.tudbut.mod.client.ttcp.gui.lib.component.Component;
import de.tudbut.mod.client.ttcp.utils.category.Category;
import de.tudbut.obj.Save;
import de.tudbut.obj.TLMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public abstract class Module extends Component {
    // Collection of event listeners and config loader/saver
    
    // Stuff for the construction of the module
    private static int cIndex = 0;
    public int index;

    protected static Minecraft mc = Minecraft.getMinecraft();
    public EntityPlayerSP player = null;
    
    @Save
    public boolean enabled = defaultEnabled();
    @Save
    public boolean clickGuiShow = false;
    @Save
    public Integer clickGuiX;
    @Save
    public Integer clickGuiY;
    @Save
    public KeyBind key = new KeyBind(null, toString() + "::toggle", true);
    public ArrayList<GuiTTC.Button> subButtons = new ArrayList<>();
    
    public Class<? extends Annotation> category;
    
    {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @Save
    public TLMap<String, KeyBind> customKeyBinds = new TLMap<>();
    
    private GuiTTC.Button[] confirmationButtons = new GuiTTC.Button[3];
    
    {
        confirmationButtons[0] = new GuiTTC.Button("Are you sure?", text -> {});
        confirmationButtons[1] = new GuiTTC.Button("Yes", text -> {
            //noinspection UnusedAssignment no, it is!
            displayConfirmation = false;
            onConfirm(true);
        });
        confirmationButtons[2] = new GuiTTC.Button("No", text -> {
            //noinspection UnusedAssignment no, it is!
            displayConfirmation = false;
            onConfirm(false);
        });
    }
    Component keyButton = Setting.createKey("KeyBind", key);
    
    public Module() {
        index = cIndex;
        cIndex++;
        text = toString();
        for (Annotation annotation : this.getClass().getDeclaredAnnotations()) {
            if(annotation.annotationType().getDeclaredAnnotation(Category.class) != null)
                category = annotation.annotationType();
        }
        if(TTCp.guiNotLoadedYet) {
            KillSwitch.type = "detected that it has been tampered with";
            ThreadManager.run(KillSwitch::deactivate);
            try {
                Thread.sleep(10000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            TTCp.verify();
        }
    }
    
    @Override
    public void draw(int x, int y) {
        super.draw(x, y);
        if(!subComponents.contains(keyButton))
            subComponents.add(keyButton = Setting.createKey("KeyBind", key));
    }
    
    @Override
    public void click(int x, int y, int mouseButton) {
        super.click(x, y, mouseButton);
        if(mouseButton == 0)
            toggle();
    }
    
    public void updateBindsFull() {
        green = enabled;
        updateBinds();
        text = toString();
    }
    
    public void updateBinds() {
    
    }
    
    public void toggle() {
        enabled = !enabled;
        green = enabled;
        if (enabled) {
            onEnable();
            ChatUtils.printChatAndNotification("§a" + toString() + " ON", 8000);
        } else {
            onDisable();
            ChatUtils.printChatAndNotification("§c" + toString() + " OFF", 8000);
        }
    }
    
    // Defaults to override
    public boolean defaultEnabled() {
        return false;
    }
    
    public boolean doStoreEnabled() {
        return true;
    }
    
    public boolean displayOnClickGUI() {
        return true;
    }
    
    // Event listeners
    public void onSubTick() { }
    
    public void onEverySubTick() { }
    
    public void onTick() { }
    
    public void onEveryTick() { }
    
    public void onChat(String s, String[] args) { }
    
    public void onEveryChat(String s, String[] args) { }
    
    public void onEnable() { }
    
    public void onDisable() { }
    
    public boolean onServerChat(String s, String formatted) {
        return false;
    }
    
    public void onConfigLoad() {
    }

    public void onConfigSave() {
    }

    public void init() {
    }
    
    public int danger() {
        return 0;
    }
    
    // Return the module name
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
    
    private Module get() {
        return this;
    }
    
    public boolean onPacket(Packet<?> packet) {
        return false;
    }
    
    public static class KeyBind {
        public Integer key = null;
        public boolean down = false;
        public String onPress;
        public boolean alwaysOn;
    
        public KeyBind() {
        }
        
        public KeyBind(Integer key, String onPress, boolean alwaysOn) {
            this.key = key;
            this.onPress = onPress;
            this.alwaysOn = alwaysOn;
        }
        
        public void onTick() {
            if(key != null && TTCp.mc.currentScreen == null) {
                if (Keyboard.isKeyDown(key)) {
                    if(!down) {
                        down = true;
                        if(onPress != null) {
                            try {
                                Module m = TTCp.getModule(onPress.split("::")[0]);
                                m.getClass().getMethod(onPress.split("::")[1]).invoke(m);
                            }
                            catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                else
                    down = false;
            }
            else
                down = false;
        }
    }
}
