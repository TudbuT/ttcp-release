package de.tudbut.mod.client.ttcp.mods.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import org.lwjgl.input.Mouse;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.mods.rendering.Notifications;
import de.tudbut.mod.client.ttcp.mods.combat.KillAura;
import de.tudbut.mod.client.ttcp.mods.combat.SmoothAura;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Misc;
import de.tudbut.obj.Save;

import java.util.ArrayList;

@Misc
public class MidClick extends Module {

    public enum CustomBindsBlock implements PartialBind {
        NONE((data) -> {}),

        ;

        public final Bind toDo;

        CustomBindsBlock(PartialBind toDo) {
            this.toDo = toDo;
        }

        @Override
        public void call(Bind.Data data) {
            toDo.call(data);
        }
    }
    public enum CustomBindsPlayer implements PartialBind {
        NONE((data) -> {}),
        Friend((data) -> {
            ArrayList<String> names = de.tudbut.mod.client.ttcp.mods.command.Friend.getInstance().names;
            if(names.contains(data.entity().getName())) {
                names.remove(data.entity().getName());
                Notifications.add(new Notifications.Notification(data.entity().getName() + " removed from your friends."));
            } else {
                names.add(data.entity().getName());
                Notifications.add(new Notifications.Notification(data.entity().getName() + " added to your friends."));
            }
        }),
        Target((data) -> {
            KillAura.getInstance().targets.clear();
            while (SmoothAura.getInstance().targets.hasNext()) {
                SmoothAura.getInstance().targets.next();
            }
            KillAura.getInstance().targets.add(data.entity().getName());
            SmoothAura.getInstance().targets.add(data.entity().getName());
        }),
        Message((data) -> {
            Minecraft.getMinecraft().displayGuiScreen(new GuiChat(TTCp.prefix + "msg " + data.entity().getName() + " "));
        }),

        ;

        public final Bind toDo;

        CustomBindsPlayer(PartialBind toDo) {
            this.toDo = toDo;
        }

        @Override
        public void call(Bind.Data data) {
            toDo.call(data);
        }
    }
    public enum CustomBindsEntity implements PartialBind {
        NONE((data) -> {}),

        ;

        public final Bind toDo;

        CustomBindsEntity(PartialBind toDo) {
            this.toDo = toDo;
        }

        @Override
        public void call(Data data) {
            toDo.call(data);
        }
    }

    public static Bind bindBlock = null;
    public static Bind bindPlayer = null;
    public static Bind bindEntity = null;
    @Save
    private static Bind cbb = CustomBindsBlock.NONE, cbp = CustomBindsPlayer.NONE, cbe = CustomBindsPlayer.NONE;

    @Override
    public void updateBinds() {
        subComponents.clear();


        if(bindBlock != null) {
            subComponents.add(new Button("ModuleBindBlock " + bindBlock.getName(),it -> {
                bindBlock = null;
                reload();
            }));
        }
        else {
            subComponents.add(new Button("ModuleBindBlock NONE", it -> {}));
        }
        if(bindPlayer != null) {
            subComponents.add(new Button("ModuleBindPlayer: " + bindPlayer.getName(),it -> {
                bindPlayer = null;
                reload();
            }));
        }
        else {
            subComponents.add(new Button("ModuleBindPlayer: NONE", it -> {}));
        }
        if(bindEntity != null) {
            subComponents.add(new Button("ModuleBindEntity: " + bindEntity.getName(),it -> {
                bindEntity = null;
                reload();
            }));
        }
        else {
            subComponents.add(new Button("ModuleBindEntity: NONE", it -> {}));
        }

        subComponents.add(Setting.createEnum(CustomBindsBlock.class, "CustomBindBlock", this, "cbb"));
        subComponents.add(Setting.createEnum(CustomBindsPlayer.class, "CustomBindPlayer", this, "cbp"));
        subComponents.add(Setting.createEnum(CustomBindsEntity.class, "CustomBindEntity", this, "cbe"));
    }

    boolean down = false;

    @Override
    public void onSubTick() {
        if(Mouse.isButtonDown(2) && mc.currentScreen == null) {
            if(!down) {
                run();
            }
            down = true;
        }
        else
            down = false;
    }

    private void run() {
        RayTraceResult hover = mc.objectMouseOver;
        if(hover.entityHit != null) {
            if(bindPlayer != null && hover.entityHit instanceof EntityPlayer) {
                bindPlayer.call(createData(hover));
                return;
            }
            if(bindEntity != null) {
                bindEntity.call(createData(hover));
                return;
            }
            if(runCustomEntityBinds(hover))
                return;
        }
        if(hover.getBlockPos() != null) {
            if(bindBlock != null) {
                bindBlock.call(createData(hover));
                return;
            }
            cbb.call(createData(hover));
        }
    }

    private boolean runCustomEntityBinds(RayTraceResult hover) {
        if(hover.entityHit instanceof EntityPlayer) {
            cbp.call(createData(hover));
        }
        else
            cbe.call(createData(hover));

        return cbe != CustomBindsEntity.NONE.toDo && cbp != CustomBindsPlayer.NONE.toDo;
    }

    private Bind.Data createData(RayTraceResult hover) {
        return new Bind.Data() {
            @Override
            public BlockPos block() {
                return hover.getBlockPos();
            }

            @Override
            public Entity entity() {
                return hover.entityHit;
            }
        };
    }

    public static void set(Bind bind) {
        switch (bind.getType()) {
            case BLOCK:
                bindBlock = bind;
                break;
            case PLAYER:
                bindPlayer = bind;
                break;
            case ENTITY:
                bindEntity = bind;
                break;
        }
        MidClick.reload();
    }

    public static void reload() {
        TTCp.getModule(MidClick.class).updateBinds();
    }

    public interface Bind {
        enum Type {
            BLOCK,
            ENTITY,
            PLAYER,

            ;
        }
        interface Data {
            BlockPos block();
            Entity entity();
        }

        Type getType();
        String getName();
        void call(Data data);
    }

    private interface PartialBind extends Bind {
        @Override
        default String getName() {
            return "";
        }

        @Override
        default Type getType() {
            return null;
        }

        void call(Data data);
    }
}
