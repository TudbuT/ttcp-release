package de.tudbut.mod.client.ttcp.mods.combat;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketAnimation;
import net.minecraft.util.EnumHand;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.GuiPlayerSelect;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.gui.lib.component.ToggleButton;
import de.tudbut.mod.client.ttcp.mods.misc.AltControl;
import de.tudbut.mod.client.ttcp.mods.command.Friend;
import de.tudbut.mod.client.ttcp.mods.rendering.PlayerSelector;
import de.tudbut.mod.client.ttcp.mods.chat.Team;
import de.tudbut.mod.client.ttcp.utils.*;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Combat;
import de.tudbut.obj.Save;
import de.tudbut.tools.Lock;
import de.tudbut.tools.Queue;

import java.util.*;

@Combat
public class KillAura extends Module {
    @Save
    int delay = 300;
    @Save
    int randomDelay = 0;
    @Save
    public int attack = 0;
    @Save
    boolean threadMode = false;
    @Save
    boolean superAttack = false;
    @Save
    boolean batch = false;
    boolean cBatch = false;
    @Save
    boolean switchItem = false;
    boolean noSwitch = false;
    boolean cSwitch = true;
    @Save
    boolean tpsSync = false;
    @Save
    SwitchType switchType = SwitchType.HOTBAR;
    @Save
    int switchDelay = 0;
    @Save
    int iterations = 1;
    @Save
    int iterationDelay = 0, iterationRandomDelay = 0;
    Lock switchTimer = new Lock();
    @Save
    boolean rotate = false;
    @Save
    boolean swing = true, offhandSwing = false, iSwing = true, iOffhandSwing = false;
    @Save
    int misses = 10;
    @Save
    int iterMisses = 25;
    @Save
    float autoDest = 1f;
    @Save
    boolean auto = false;
    @Save
    boolean oldAuto = false;
    @Save
    boolean hyperMode = false;
    @Save
    boolean iterationSync = false;
    @Save
    int subIterations = 1;
    @Save
    int switches = 1;
    int switchesDone = 0;
    
    @Save
    boolean switchIterations = false, switchSubIterations = false;
    
    Map<EntityLivingBase, Float> lastHealth = new HashMap<>();
    Map<EntityLivingBase, Integer> lastTotems = new HashMap<>();
    
    public enum SwitchType {
        HOTBAR,
        SWAP,
        ;
    }
    
    Queue<EntityLivingBase> toAttack = new Queue<>();
    public ArrayList<String> targets = new ArrayList<>();
    Lock threadLock = new Lock(true);
    Lock timer = new Lock();
    Thread thread = new Thread(() -> {
        while (true) {
            try {
                threadLock.waitHere(100);
                if(threadLock.isLocked())
                    continue;
                timer.waitHere((int) (delay / 6 * Utils.tpsMultiplier()));
                if (enabled)
                    onTick();
                else
                    timer.lock((int) (delay / 2 * Utils.tpsMultiplier()));
            } catch (Exception ignore) { }
        }
    }, "KillAura"); { thread.start(); }
    
    {
        customKeyBinds.set("select", new KeyBind(null, toString() + "::triggerSelect", false));
    }
    
    @Override
    public void init() {
        PlayerSelector.types.add(new PlayerSelector.Type(player -> {
            targets.clear();
            targets.add(player.getGameProfile().getName());
        }, "Set KillAura target"));
    }
    
    @SuppressWarnings("unused")
    public void triggerSelect() {
        targets.clear();
        TTCp.mc.displayGuiScreen(
                new GuiPlayerSelect(
                        TTCp.world.playerEntities.stream().filter(
                                player -> !player.getName().equals(TTCp.player.getName())
                        ).toArray(EntityPlayer[]::new),
                        player -> {
                            targets.remove(player.getName());
                            targets.add(player.getName());
                        
                            return false;
                        }
                )
        );
    }
    
    static KillAura instance;
    {
        instance = this;
    }
    public static KillAura getInstance() {
        return instance;
    }
    
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new Button("Attack " + (attack == 0 ? "all" : (attack == 1 ? "players" : "targets")), it -> {
            attack++;
            if(attack > 2)
                attack = 0;
        
            it.text = "Attack " + (attack == 0 ? "all" : (attack == 1 ? "players" : "targets"));
        }));
        subComponents.add(new ToggleButton("HyperMode", this, "hyperMode") {
            @Override
            public synchronized void click(int x, int y, int mouseButton) {
                super.click(x, y, mouseButton);
                updateBinds();
            }
        });
        if(!hyperMode) {
            subComponents.add(Setting.createInt(10, 1000, "Delay", this, "delay"));
            subComponents.add(Setting.createInt(0, 500, "RandomDelay", this, "randomDelay"));
        }
        subComponents.add(Setting.createBoolean("Thread mode", this, "threadMode"));
        if(!hyperMode) {
            subComponents.add(Setting.createBoolean("SuperAttack", this, "superAttack"));
            subComponents.add(Setting.createBoolean("Batches", this, "batch"));
        }
        subComponents.add(Setting.createBoolean("Switch", this, "switchItem"));
        subComponents.add(Setting.createBoolean("Rotate", this, "rotate"));
        subComponents.add(Setting.createBoolean("TPSSync", this, "tpsSync"));
        subComponents.add(Setting.createEnum(SwitchType.class, "SwitchType", this, "switchType"));
        subComponents.add(Setting.createInt(0, 400, "SwitchDelay", this, "switchDelay"));
        subComponents.add(Setting.createInt(1, 5, "SwitchCount", this, "switches"));
        subComponents.add(Setting.createInt(1, 10, "Iterations (i/a)", this, "iterations"));
        subComponents.add(Setting.createInt(1, 10, "SubIterations (si/i)", this, "subIterations"));
        subComponents.add(Setting.createInt(0, 100, "IterDelay", this, "iterationDelay"));
        subComponents.add(Setting.createInt(0, 100, "IterRandomDelay", this, "iterationRandomDelay"));
        subComponents.add(Setting.createBoolean("Swing", this, "swing"));
        subComponents.add(Setting.createBoolean("SwitchIterations", this, "switchIterations"));
        subComponents.add(Setting.createBoolean("SwitchSubIterations", this, "switchSubIterations"));
        subComponents.add(Setting.createBoolean("IterationSync", this, "iterationSync"));
        subComponents.add(Setting.createBoolean("IterSwing", this, "iSwing"));
        subComponents.add(Setting.createBoolean("OffhandSwing", this, "offhandSwing"));
        subComponents.add(Setting.createBoolean("IterOffhandSwing", this, "iOffhandSwing"));
        subComponents.add(Setting.createInt(0, 100, "MissChance", this, "misses"));
        subComponents.add(Setting.createInt(0, 100, "IterMissChance", this, "iterMisses"));
        if(!hyperMode)
            subComponents.add(Setting.createBoolean("AutoSetup", this, "auto"));
        subComponents.add(Setting.createKey("Select", customKeyBinds.get("select")));
    }
    
    EntityLivingBase entity;
    Lock iTimer = new Lock(true);
    int iterationsToDo = 0;
    
    @Override
    public void onTick() {
        if(threadMode)
            threadLock.unlock();
        else
            threadLock.lock();
    
        if (threadMode && !(Thread.currentThread() == thread)) {
            return;
        }

        {
            if (!toAttack.hasNext()) {
                EntityPlayer[] players = TTCp.world.playerEntities.toArray(new EntityPlayer[0]);
                for (int i = 0 ; i < players.length ; i++) {
                    if (
                            players[i].getDistance(TTCp.player) < 6 &&
                            !Team.getInstance().names.contains(players[i].getGameProfile().getName()) &&
                            !Friend.getInstance().names.contains(players[i].getGameProfile().getName()) &&
                            !players[i].getGameProfile().getName().equals(TTCp.mc.getSession().getProfile().getName()) &&
                            !AltControl.getInstance().isAlt(players[i])
                    ) {
                        if (!targets.isEmpty() || attack == 2) {
                            if (targets.contains(players[i].getGameProfile().getName())) {
                                toAttack.add(players[i]);
                            }
                        }
                        else
                            toAttack.add(players[i]);
                    }
                }
            }
            if (!toAttack.hasNext() && attack == 0) {
                EntityLivingBase[] entities = Utils.getEntities(EntityLivingBase.class, EntityLivingBase::isEntityAlive);
                for (int i = 0 ; i < entities.length ; i++) {
                    if (
                            entities[i].getDistance(TTCp.player) < 6 &&
                            !(entities[i] instanceof EntityPlayer)
                    ) {
                        toAttack.add(entities[i]);
                    }
                }
            }
        }
        
        if(!switchTimer.isLocked() && !switchIterations) {
            switchTimer.lock(switchDelay);
    
            if(switchItem && toAttack.hasNext() && !noSwitch) {
                switchItem();
            }
        }
        if (hyperMode) {
            if (toAttack.hasNext()) {
                hyper();
            }
        } else {
            if (!timer.isLocked()) {
                int e = extraDelay();
                if(switchDelay == 0)
                    switchTimer.lock(delay(e) / 3);
                timer.lock(delay(e));
                if ((cBatch = !cBatch) && batch) {
                    timer.lock(delay(e) * 2);
                    if(switchDelay == 0)
                        switchTimer.lock(delay(e) * 2 / 3);
                }
    
                if (auto) {
                    try {
                        int i = lastTotems.get(toAttack.peek()) - countTotems(toAttack.peek());
                        if (lastHealth.get(toAttack.peek()) > toAttack.peek().getHealth() || i != 0) {
                            if (i < 0)
                                i = 1;
                            autoAttack(i);
                        }
                        else {
                            autoAttack(0);
                        }
                    }
                    catch (Exception a) {
                    }
                }
    
                if (toAttack.hasNext())
                    attackNext();
            }
        }
        
        if(iterationSync) {
            if(!iTimer.isLocked()) {
                iTimer.lock();
                if(iterationsToDo > 0) {
                    for (int i = 0 ; i < subIterations ; i++) {
                        if (Math.random() > iterMisses / 100f)
                            TTCp.mc.playerController.attackEntity(TTCp.player, entity);
                        if (swing)
                            TTCp.player.swingArm(EnumHand.MAIN_HAND);
                        if(switchSubIterations)
                            switchItem();
                    }
                    if (switchIterations && !switchSubIterations)
                        switchItem();
                    if(iterationsToDo-- > 1) {
                        iTimer.lock(iterationDelay);
                    }
                }
            }
        }
    }
    
    private void switchItem() {
        if(noSwitch)
            return;
        switch (switchType) {
            case HOTBAR:
                int i = InventoryUtils.getCurrentSlot();
                if (i == 0)
                    cSwitch = true;
                if (i == 8)
                    cSwitch = false;
                InventoryUtils.setCurrentSlot(i + ((cSwitch = !cSwitch) ? -1 : 1));
                break;
            case SWAP:
                InventoryUtils.swap(37 + switchesDone++, 0);
                switchesDone = switchesDone % switches;
                break;
        }
    }
    
    int lastHRT = 0;
    boolean allowHRT = true;
    
    private void hyper() {
        EntityLivingBase ta = toAttack.peek();
        if(ta.hurtResistantTime > lastHRT)
            allowHRT = true;
        if(ta.hurtResistantTime < ta.maxHurtResistantTime * 0.75f && allowHRT) {
            allowHRT = false;
            switchTimer.unlock();
            attackNext();
        }
        lastHRT = ta.hurtResistantTime;
    }
    
    boolean lastAutoAttack = false;
    float autoAttackAvrg = -1;
    float autoAttackBiggerAvrg = -1;
    float delayAvrg = delay;
    int lastAdd = -10;
    
    private void autoAttack(float x) {
        if(autoAttackAvrg == -1) {
            autoAttackAvrg = 0;
            autoAttackBiggerAvrg = 0;
        }
        autoAttackAvrg = (autoAttackAvrg * 4 + x) / 5;
        autoAttackBiggerAvrg = (autoAttackBiggerAvrg * 24 + x) / 25;
        delayAvrg = (delayAvrg * 24 + delay) / 25;
        if(autoAttackAvrg / Math.max(delay, 1) < autoAttackBiggerAvrg / Math.max(delayAvrg, 1))
            delay += lastAdd = -lastAdd;
        else {
            delay += lastAdd;
        }
        delay = Math.max(delay, 0);
    }
    
    private int countTotems(EntityLivingBase entity) {
        int t = 0;
        ItemStack itemStack = entity.getHeldItem(EnumHand.MAIN_HAND);
        if (itemStack.getItem() == Items.TOTEM_OF_UNDYING) {
            t = itemStack.getCount();
        } else {
            itemStack = entity.getHeldItem(EnumHand.OFF_HAND);
            if (itemStack.getItem() == Items.TOTEM_OF_UNDYING) {
                t = itemStack.getCount();
            }
        }
        return t;
    }
    
    private int delay(int e) {
        return (int) (delay + e * (tpsSync ? Utils.tpsMultiplier() : 1));
    }
    
    private int extraDelay() {
        return (int) (randomDelay * Math.random());
    }
    
    public void attackNext() {
        EntityLivingBase entity = toAttack.next();
        
        if(Math.random() <= misses / 100f) {
            if (swing)
                TTCp.player.swingArm(EnumHand.MAIN_HAND);
            if (offhandSwing)
                TTCp.player.swingArm(EnumHand.OFF_HAND);
            return;
        }

        if(!superAttack || entity.hurtTime <= 0) {
            if(iterationSync) {
                iterationsToDo = iterations;
                iTimer.unlock();
                this.entity = entity;
            }
            else {
                lastHealth.put(entity, entity.getHealth());
                lastTotems.put(entity, countTotems(entity));
                for (int i = 0 ; i < iterations ; i++) {
                    if (rotate)
                        BlockUtils.lookAt(entity.getPositionVector().add(Math.random() * 0.3 - 0.15, (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) / 1.5 + Math.random() * 0.3 - 0.15, Math.random() * 0.3 - 0.15));
                    for (int j = 0 ; j < subIterations ; j++) {
                        if (Math.random() > iterMisses / 100f)
                            TTCp.mc.playerController.attackEntity(TTCp.player, entity);
    
                        if (i != iterations - 1 && iSwing)
                            TTCp.player.swingArm(EnumHand.MAIN_HAND);
                        if (i != iterations - 1 && iOffhandSwing)
                            TTCp.player.swingArm(EnumHand.OFF_HAND);
                    }
        
                    try {
                        Thread.sleep((long) (iterationDelay + iterationRandomDelay * Math.random()));
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        if (swing)
            TTCp.player.swingArm(EnumHand.MAIN_HAND);
        if (offhandSwing)
            TTCp.player.swingArm(EnumHand.OFF_HAND);
    }
    
    @Override
    public boolean onPacket(Packet<?> packet) {
        if(packet instanceof SPacketAnimation) {
            if(((SPacketAnimation) packet).getAnimationType() == 0) {
                if(swingNotifiers.containsKey(((SPacketAnimation) packet).getEntityID())) {
                    ChatUtils.print("§8Swing by " + mc.world.playerEntities.stream().filter(entityPlayer -> entityPlayer.getEntityId() == ((SPacketAnimation) packet).getEntityID()).findFirst());
                    swingNotifiers.get(((SPacketAnimation) packet).getEntityID()).unlock();
                }
            }
        }
        
        return false;
    }
    
    Map<Integer, Lock> swingNotifiers = new HashMap<>();
    
    @Override
    public void onEveryChat(String s, String[] args) {
        if(args.length == 1) {
            new Thread(() -> {
                String playerName = args[0];
                Optional<EntityPlayer> player = mc.world.playerEntities.stream().filter(entityPlayer -> entityPlayer.getName().equals(playerName)).findFirst();
                if(player.isPresent()) {
                    ChatUtils.print("Watching...");
                    EntityPlayer detect = player.get();
                    
                    ArrayList<Long> timings = new ArrayList<>();
                    Lock lock = new Lock();
                    swingNotifiers.put(detect.getEntityId(), lock);
                    for (int i = 0 ; i < 10 ; i++) {
                        int spi;
                        lock.waitHere();
                        lock.lock();
                        timings.add(System.currentTimeMillis());
                    }
                    swingNotifiers.put(detect.getEntityId(), null);
                    long last = timings.get(0);
                    long l = 0;
                    for (int i = 1 ; i < timings.size() ; i++) {
                        l += timings.get(i) - last;
                        last = timings.get(i);
                    }
                    ChatUtils.print("§a[TTC] KA Speed of " + detect.getName() + ": " + l / (timings.size() - 1f));
                }
                else {
                    ChatUtils.print("That player is not in the visual range!");
                }
            }).start();
        }
    }
    
    @Override
    public void onConfigLoad() {
        updateBinds();
    }
    
    @Override
    public int danger() {
        return 3;
    }
}
