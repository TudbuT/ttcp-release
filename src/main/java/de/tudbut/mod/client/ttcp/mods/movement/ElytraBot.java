package de.tudbut.mod.client.ttcp.mods.movement;

import de.tudbut.type.Vector2d;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.GuiTTC;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.gui.lib.component.IntSlider;
import de.tudbut.mod.client.ttcp.gui.lib.component.ToggleButton;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.mods.rendering.Notifications;
import de.tudbut.mod.client.ttcp.utils.category.Movement;
import de.tudbut.mod.client.ttcp.utils.pathfinding.AStar;
import de.tudbut.mod.client.ttcp.utils.pathfinding.Node;
import de.tudbut.obj.Atomic;
import de.tudbut.obj.Save;
import de.tudbut.rendering.Maths2D;

import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.HashSet;

import static de.tudbut.mod.client.ttcp.utils.Tesselator.*;
import static de.tudbut.mod.client.ttcp.utils.Tesselator.put;

@Movement
public class ElytraBot extends Module {
    
    static ElytraBot bot;
    
    {
        bot = this;
    }
    
    public static ElytraBot getInstance() {
        return bot;
    }
    
    
    Atomic<Vec3d> dest = new Atomic<>();
    double orbitRotation = 0.1;
    private static final double PI_TIMES_TWO = Math.PI * 2;
    private static final Vector2d zeroZero = new Vector2d(0,0);
    private Vector2d original = zeroZero.clone();
    @Save
    public boolean pathFind = false, strict = false, highlight = true, ahighlight = false;
    @Save
    public int dist = 60, earlyExitLength = 30, length = 200, timeout = 5, earlyExitTimeout = 3, avoidWeight = 5, escapeStrength = 1500, panicEscapeTries = 20, maxEscapeTries = 50, restartEscapeTries = 100;
    public boolean newPath = false;
    Node[] nodes = null;
    AtomicReference<ArrayList<Node>> visitedNodes = null;
    AtomicReference<ArrayList<Node>> toBeVisitedNodes = null;
    int currentNode = 0;
    HashSet<BlockPos> avoid = new HashSet<>(), deny = new HashSet<>();
    BlockPos escapeModeBegin = null;
    boolean discard = false;
    boolean escaping = false;
    boolean wasEscaping = false;
    int escapeTry = 0;
    
    int task = -1;
    
    {updateBinds();}
    
    public void updateBinds() {
        subComponents.clear();
        if(task == -1 && !FlightBot.isActive()) {
            subComponents.add(new Button("Mode", text -> displayModeMenu()));
            subComponents.add(new ToggleButton("Pathfinding", this, "pathFind", this::updateBinds));
        }
        else
            subComponents.add(new Button("Stop", it -> {
                stop();
            }));
        if(pathFind) {
            subComponents.add(Setting.createInt(25, 525, "Search Distance", this, "dist"));
            subComponents.add(Setting.createInt(25, 525, "Search Length", this, "length"));
            subComponents.add(Setting.createInt(1, 50, "Search Timeout (seconds)", this, "timeout"));
            subComponents.add(Setting.createInt(1, 20, "Search FastMode Time (seconds)", this, "earlyExitTimeout"));
            subComponents.add(Setting.createBoolean("SearchHighlight", this, "highlight"));
            subComponents.add(Setting.createBoolean("AvoidHighlight", this, "ahighlight"));
            subComponents.add(Setting.createInt(0, 100, "EscapeMode weight", this, "avoidWeight"));
            subComponents.add(Setting.createInt(0, 10000, "EscapeStrength", this, "escapeStrength"));
            subComponents.add(Setting.createInt(0, 100, "EscapeTries until Panic", this, "panicEscapeTries"));
            subComponents.add(Setting.createInt(0, 100, "EscapeTries until ResetDeny", this, "maxEscapeTries"));
            subComponents.add(Setting.createInt(0, 100, "EscapeTries until Restart", this, "restartEscapeTries"));
            subComponents.add(new Button("EscapeTry: " + escapeTry, text -> {}));
        }
        subComponents.add(new ToggleButton("Strict Anticheat", this, "strict"));
        customKeyBinds.setIfNull("stop", new KeyBind(null, this + "::stop", false));
        subComponents.add(Setting.createKey("StopKeybind", customKeyBinds.get("stop")));
        customKeyBinds.setIfNull("discard", new KeyBind(null, this + "::discard", false));
        subComponents.add(Setting.createKey("DiscardKeybind", customKeyBinds.get("discard")));
    }
    
    public void displayModeMenu() {
        subComponents.clear();
        subComponents.add(new Button("Back", it -> {
            updateBinds();
        }));
        subComponents.add(new Button("Orbit spawn", it -> {
            original = zeroZero.clone();
            startOrbitSpawn();
            updateBinds();
        }));
        subComponents.add(new Button("Orbit spawn from here", it -> {
            original = new Vector2d(Math.sqrt(TTCp.player.posX * TTCp.player.posX + TTCp.player.posZ * TTCp.player.posZ), 0);
            startOrbitSpawn();
            updateBinds();
        }));
    }
    
    public void startOrbitSpawn() {
        dest.set(new Vec3d(original.getX(), 257.1, original.getY()));
        FlightBot.deactivate();
        FlightBot.activate(dest);
        ChatUtils.chatPrinterDebug().println("Now flying to " + original);
        task = 0;
    }
    
    public void tickOrbitSpawn() {
        if(!FlightBot.isFlying() && !isRising) {
            Vector2d point = original.clone().add(orbitRotation * 5 * 16, 0);
            orbitRotation += (5 / (point.getX() * PI_TIMES_TWO));
            Maths2D.rotate(point, zeroZero, orbitRotation * PI_TIMES_TWO);
            Vec3d vec = dest.get();
            dest.set(new Vec3d(point.getX(), 257.1, point.getY()));
            ChatUtils.chatPrinterDebug().println("Distance traveled: " + (vec.distanceTo(dest.get())) +  "... Now flying to " + point);
        }
    }
    
    Atomic<Vec3d> theDest = new Atomic<>();
    int lastNoFly = 0;
    boolean isDoneFlying = false;
    public synchronized void tickGoTo() {
        lastNoFly++;
        if((!FlightBot.isFlying() || newPath) && pathFind && nodes != null) {
            lastNoFly = 0;
            newPath = false;
            if(currentNode >= nodes.length) {
                FlightBot.deactivate();
                if(isDoneFlying) {
                    stop();
                }
                return;
            }
            BlockPos bp = nodes[currentNode++];
            theDest.set(new Vec3d(bp.getX() + 0.5, bp.getY() + 0.2, bp.getZ() + 0.5));
        }
    }
    
    Vec3d pos;
    
    @SubscribeEvent
    public void onRenderWorld(Event event) {
    
        if (event instanceof RenderWorldLastEvent) {
            Node[] nodes = this.nodes;
            if (this.enabled && TTCp.isIngame() && task == 1) {
                Entity e = TTCp.mc.getRenderViewEntity();
                pos = e.getPositionEyes(
                        ((RenderWorldLastEvent) event).getPartialTicks()
                ).add(0, -e.getEyeHeight(), 0);

                if(nodes != null) {
                    int color = 0x80ffffff;
                    ready();
                    translate(-this.pos.x, -this.pos.y, -this.pos.z);
                    color(color);
                    depth(false);
                    begin(GL11.GL_QUADS);

                    for (int i = 0; i < nodes.length; i++) {
                        try {
                            Vec3d pos = new Vec3d(nodes[i]).add(0.5, 0, 0.5);

                            put(pos.x - 0.5, pos.y - 0.01, pos.z + 0.5);
                            put(pos.x + 0.5, pos.y - 0.01, pos.z + 0.5);
                            put(pos.x + 0.5, pos.y - 0.01, pos.z - 0.5);
                            put(pos.x - 0.5, pos.y - 0.01, pos.z - 0.5);
                            next();
                        } catch(NullPointerException er) {
                        }
                    }
                    end();
                }
                if(highlight && visitedNodes != null && visitedNodes.get() != null) {
                    nodes = visitedNodes.get().toArray(new Node[0]);

                    int color = 0x20ffff00;
                    ready();
                    translate(-this.pos.x, -this.pos.y, -this.pos.z);
                    
                    color(color);
                    depth(false);
                    begin(GL11.GL_QUADS);
                    for (int i = 0; i < nodes.length; i++) {
                        try {
                            Vec3d pos = new Vec3d(nodes[i]).add(0.5, 0, 0.5);

                            put(pos.x - 0.5, pos.y - 0.01, pos.z + 0.5);
                            put(pos.x + 0.5, pos.y - 0.01, pos.z + 0.5);
                            put(pos.x + 0.5, pos.y - 0.01, pos.z - 0.5);
                            put(pos.x - 0.5, pos.y - 0.01, pos.z - 0.5);
                            next();
                        } catch(NullPointerException er) {
                        }
                    }
                    end();
                }
                if(highlight && toBeVisitedNodes != null && toBeVisitedNodes.get() != null) {
                    nodes = toBeVisitedNodes.get().toArray(new Node[0]);

                    int color = 0x200000ff;
                    ready();
                    translate(-this.pos.x, -this.pos.y, -this.pos.z);
                    
                    color(color);
                    depth(false);
                    begin(GL11.GL_QUADS);
                    for (int i = 0; i < nodes.length; i++) {
                        try {
                            Vec3d pos = new Vec3d(nodes[i]).add(0.5, 0, 0.5);

                            put(pos.x - 0.5, pos.y - 0.01, pos.z + 0.5);
                            put(pos.x + 0.5, pos.y - 0.01, pos.z + 0.5);
                            put(pos.x + 0.5, pos.y - 0.01, pos.z - 0.5);
                            put(pos.x - 0.5, pos.y - 0.01, pos.z - 0.5);
                            next();
                        } catch(NullPointerException er) {
                        }
                    }
                    end();
                }
                if(ahighlight) {
                    synchronized(avoid) {
                        nodes = avoid.toArray(new Node[0]);
                    }

                    int color = 0x20ff0000;
                    ready();
                    translate(-this.pos.x, -this.pos.y, -this.pos.z);
                    
                    color(color);
                    depth(false);
                    begin(GL11.GL_QUADS);
                    for (int i = 0; i < nodes.length; i++) {
                        try {
                            Vec3d pos = new Vec3d(nodes[i]).add(0.5, 0, 0.5);

                            put(pos.x - 0.5, pos.y - 0.01, pos.z + 0.5);
                            put(pos.x + 0.5, pos.y - 0.01, pos.z + 0.5);
                            put(pos.x + 0.5, pos.y - 0.01, pos.z - 0.5);
                            put(pos.x - 0.5, pos.y - 0.01, pos.z - 0.5);
                            next();
                        } catch(NullPointerException er) {
                        }
                    }
                    end();
                }

            }
        }
    }
    
    @Override
    public void onTick() {
        if(TTCp.mc.world == null) {
            return;
        }
        EntityPlayerSP player = TTCp.player;
    
        
        if(player.posY >= 257 && !(pathFind && task == 1)) {
            switch (task) {
                case -1:
                    break;
                case 0:
                    tickOrbitSpawn();
                    break;
                case 1:
                    FlightBot.updateDestination(dest);
                    if (!FlightBot.isFlying() && !isRising) {
                        stop();
                    }
                    break;
            }
        }
        if(!(pathFind && task == 1)) {
            if (task != -1 && FlightBot.isActive()) {
                rise(257);
            }
        }
        else {
            tickGoTo();
        }
    }

    public void onDisable() {
        FlightBot.deactivate();
        if(visitedNodes != null)
            visitedNodes.set(null);
        visitedNodes = null;
    }
    public void onEnable() {
        nodes = null;
        lastNoFly = 20;
    }
    
    boolean isRising = false;
    
    public void flyTo(BlockPos pos, boolean pathFind) {
        task = 1;
        nodes = null;
        currentNode = 0;
        FlightBot.setForce(true);
        FlightBot.deactivate();
        if(pathFind) {
            isDoneFlying = false;
            theDest.set(TTCp.player.getPositionVector());
            dest.set(new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
            ThreadManager.run(() -> {
                try {
                    boolean stop = false;
                    BlockPos endPosition;

                    escapeModeBegin = BlockUtils.getRealPos(TTCp.player.getPositionVector());
                    endPosition = flyAStar();
                    if(endPosition == null)
                        return;

                    while (task == 1 && !stop) {
                        Node[] nodes = this.nodes;
                        if(nodes.length > 0 && nodes[nodes.length - 1].equals(endPosition))
                            stop = true;
                        try {
                            for(int i = 0 ; (((i < 100 || currentNode < nodes.length - 5 || (stop && currentNode < nodes.length)) && lastNoFly < 20) || !enabled) && !discard ; i++) {
                                Thread.sleep(10);
                                if (task != 1 || isDoneFlying)
                                    return;
                            }
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if(stop)
                            break;
                        isDoneFlying = false;
        
                        discard = false;
                        endPosition = flyAStar();
                        if(endPosition == null)
                            return;
                    }
                } finally {
                    stop();
                }
            });
        }
        else {
            FlightBot.deactivate();
            dest.set(new Vec3d(pos.getX(), 257, pos.getZ()));
            FlightBot.activate(dest);
            FlightBot.setForce(false);
        }
        updateBinds();
    }

    public BlockPos flyAStar() {
        updateBinds();

        Vec3d d = dest.get();
        
        if(d.distanceTo(TTCp.player.getPositionVector()) > Math.max(dist, length) * 2) {
            d = new Vec3d(d.x, -1, d.z);
        }

        BlockPos endPosition = new BlockPos((int) d.x, (int) d.y, (int) d.z);

        AStar.Result result = AStar.calculate(
                BlockUtils.getRealPos(TTCp.mc.player.getPositionVector()),
                endPosition,
                TTCp.world,
                dist,
                length,
                timeout,
                earlyExitTimeout,
                visitedNodes = new AtomicReference<>(),
                toBeVisitedNodes = new AtomicReference<>(),
                avoid,
                avoidWeight,
                deny,
                escaping,
                escaping ? escapeStrength : 0,
                escapeTry > panicEscapeTries
        );

        ChatUtils.print("§a[TTC] §r[ElytraBot] §aFound path of length " + result.nodes[0].length);
        wasEscaping = escaping;
        escaping = result.didAvoid && (result.didEarlyExit || escaping);
        if(escaping && !wasEscaping) {
            Notifications.add(new Notifications.Notification("[ElytraBot] Engaged EscapeMode"));
        }
        if(!escaping && wasEscaping) {
            Notifications.add(new Notifications.Notification("[ElytraBot] Disengaged EscapeMode"));
        }
        if(escaping)
            escapeTry++;
        else
            escapeTry = (int) (escapeTry * 0.75f);
        Node[][] nodes = result.nodes;
        this.nodes = nodes[0];
        if (nodes[0].length == 1)
            return null;
        Node last;
        if (nodes[0].length != 0) {
            last = nodes[0][nodes[0].length - 1];
            deny.add(last);
        } {
            last = nodes[1][nodes[1].length - 1];
        }

        if(escapeTry > maxEscapeTries) {
            deny.clear();
        }
        if(escapeTry > restartEscapeTries) {
            escapeTry = 0;
            deny.clear();
            avoid.clear();
        }

        synchronized(avoid) {
            if(enabled || discard) {
                for (int i = 0 ; i < nodes[1].length; i++) {
                    if(nodes[1][i].distanceSq(last) > 10*10) {
                        if(avoid.contains(nodes[1][i]) && escapeTry > 5)
                            deny.add(nodes[1][i]);
                        else
                            avoid.add(nodes[1][i]);
                    }
                }
                if(new Vec3d(escapeModeBegin).distanceTo(new Vec3d(last)) > 4 * Math.max(dist, length)) {
                    if(escapeTry == 0) {
                        avoid.clear();
                    }
                    escapeModeBegin = last;
                }
            }
        }
        if(discard) {
            discard = false;
            return endPosition;
        }
        result = AStar.calculate(
                BlockUtils.getRealPos(TTCp.mc.player.getPositionVector()),
                last,
                TTCp.world,
                length,
                length,
                earlyExitTimeout,
                earlyExitTimeout,
                new AtomicReference<>(),
                new AtomicReference<>(),
                new HashSet<>(),
                avoidWeight,
                new HashSet<>(),
                false,
                0,
                false
        );
        if(result.nodes[0][result.nodes[0].length - 1].equals(last)) {
            nodes = result.nodes;
        }
        if (nodes[0].length == 1)
            return null;
        else
            this.nodes = nodes[0];
        if(task == 1 && enabled) {
            currentNode = 0;
            float sd = Float.MAX_VALUE;
            for (int i = 0 ; i < nodes[0].length ; i++) {
                float f = (float) nodes[0][i].distanceSq(TTCp.player.getPosition());
                if (f < sd) {
                    sd = f;
                    currentNode = i;
                }
            }
            if(task != 1)
                return null;
            newPath = true;
            FlightBot.setSpeed(1);
            FlightBot.setForce(true);
            updateBinds();
            tickGoTo();
            FlightBot.activate(theDest);
        }
        return endPosition;
    }

    public synchronized void discard() {
        discard = true;
        onDisable();
        onEnable();
    }
    
    public synchronized void stop() {
        if(task != -1)
            ChatUtils.print("§a[TTC] §r[ElytraBot] §aStopped.");
        if(visitedNodes != null)
            visitedNodes.set(null);
        isDoneFlying = true;
        visitedNodes = null;
        escaping = false;
        FlightBot.deactivate();
        escapeTry = 0;
        task = -1;
        orbitRotation = 0.1;
        FlightBot.setForce(false);
        avoid.clear();
        deny.clear();
        updateBinds();
    }
    
    int strictCounter = 0;
    
    public void rise(double pos) {
        EntityPlayerSP player = TTCp.player;
        
        if(!FlightBot.isActive()) {
            isRising = false;
            return;
        }
        
        if(player.posY < pos) {
            if(strict) {
                if(strictCounter++ % 60 == 0)
                    TTCp.getModule(ElytraFlight.class).up();
            }
            else {
                FlightBot.updateDestination(new Atomic<>(new Vec3d(player.posX, pos, player.posZ)));
                isRising = true;
            }
        } else if(isRising) {
            FlightBot.updateDestination(dest);
            isRising = false;
        }
    }
    
    @Override
    public void onEveryChat(String s, String[] args) {
        if(TTCp.mc.world == null) {
            return;
        }

        if(args.length == 0) {
            stop();
            return;
        }
        
        if(task != -1 || FlightBot.isActive()) {
            ChatUtils.print("You have to stop your current task first.");
            return;
        }
    
        switch (args.length) {
            case 2:
                flyTo(new BlockPos(Integer.parseInt(args[0]), pathFind ? -1 : 257, Integer.parseInt(args[1])), pathFind);
                ChatUtils.print("Flying...");
                break;
            case 3:
                flyTo(new BlockPos(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2])), pathFind);
                ChatUtils.print("Flying...");
                break;
                
        }
        updateBinds();
    }
    
    @Override
    public int danger() {
        return 2;
    }
}
