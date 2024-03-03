package de.tudbut.mod.client.ttcp.mods.combat;

import de.tudbut.type.Vector2d;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.util.EnumHand;
import org.lwjgl.input.Keyboard;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.GuiPlayerSelect;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.mods.misc.AltControl;
import de.tudbut.mod.client.ttcp.mods.command.Friend;
import de.tudbut.mod.client.ttcp.mods.rendering.PlayerSelector;
import de.tudbut.mod.client.ttcp.mods.chat.Team;
import de.tudbut.mod.client.ttcp.utils.BlockUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Combat;
import de.tudbut.obj.Save;
import de.tudbut.tools.Queue;

import java.util.Date;

@Combat
public class SmoothAura extends Module {
    @Save
    int delay = 200;
    //@Save
    //int smoothness = 200;
    long last = 0;
    @Save
    int attack = 0;
    public Queue<Entity> toAttack = new Queue<>();
    public Queue<String> targets = new Queue<>();
    public String target = null;
    
    {
        customKeyBinds.set("select", new KeyBind(null, toString() + "::triggerSelect", false));
    }
    
    @Override
    public void init() {
        PlayerSelector.types.add(new PlayerSelector.Type(player -> {
            while (targets.hasNext()) {
                targets.next();
            }
            targets.add(player.getGameProfile().getName());
        }, "Set SmoothAura target"));
    }
    
    @SuppressWarnings("unused")
    public void triggerSelect() {
        while(targets.hasNext())
            targets.next();
        
        target = null;
            
        TTCp.mc.displayGuiScreen(
                new GuiPlayerSelect(
                        TTCp.world.playerEntities.stream().filter(
                                player -> !player.getName().equals(TTCp.player.getName())
                        ).toArray(EntityPlayer[]::new),
                        player -> {
                            if(!targets.toList().contains(player.getName()))
                                targets.add(player.getName());
                            
                            return false;
                        }
                )
        );
    }
    
    static SmoothAura instance;
    {
        instance = this;
    }
    public static SmoothAura getInstance() {
        return instance;
    }
    
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new Button("Delay: " + delay, it -> {
            if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                delay -= 25;
            else
                delay += 25;
            
            if(delay < 50)
                delay = 1000;
            if(delay > 1000)
                delay = 50;
            
            it.text = "Delay: " + delay;
        }));
    }
    
    @Override
    public void onTick() {
            a :
            {
                
                if (TTCp.world == null)
                    break a;
                
                boolean shouldNext = true;
                
                if(!toAttack.hasNext()) {
                    EntityPlayer[] players = TTCp.world.playerEntities.toArray(new EntityPlayer[0]);
                    for (int i = 0; i < players.length; i++) {
                        if(
                                players[i].getDistance(TTCp.player) < 8 &&
                                !Team.getInstance().names.contains(players[i].getGameProfile().getName()) &&
                                !Friend.getInstance().names.contains(players[i].getGameProfile().getName()) &&
                                !players[i].getGameProfile().getName().equals(TTCp.mc.getSession().getProfile().getName()) &&
                                !AltControl.getInstance().isAlt(players[i]) &&
                                players[i].getHealth() != 0
                        ) {
                            if(players[i].getName().equals(target)) {
                                toAttack.add(players[i]);
                                shouldNext = false;
                            }
                        }
                    }
                }
                if(shouldNext && targets.hasNext())
                    target = targets.next();
                
                if(toAttack.hasNext())
                    attackNext();
            }
    }
    
    public void attackNext() {
        Entity entity = toAttack.next();
        
        Vector2d rot = new Vector2d(TTCp.player.rotationYaw, TTCp.player.rotationPitch);
        BlockUtils.lookCloserTo(entity.getPositionVector().add(0, (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) / 2, 0), (float) (Math.random() * 20f));
    
        if (new Date().getTime() >= last + delay) {
            last = new Date().getTime();
            if(TTCp.mc.objectMouseOver != null && TTCp.mc.objectMouseOver.entityHit != null) {
                TTCp.mc.playerController.attackEntity(TTCp.player, TTCp.mc.objectMouseOver.entityHit);
                TTCp.player.setSprinting(false);
                TTCp.player.connection.sendPacket(new CPacketEntityAction(TTCp.player, CPacketEntityAction.Action.STOP_SPRINTING));
                TTCp.player.setSprinting(true);
                TTCp.player.connection.sendPacket(new CPacketEntityAction(TTCp.player, CPacketEntityAction.Action.START_SPRINTING));
            }
            TTCp.player.swingArm(EnumHand.MAIN_HAND);
            TTCp.player.resetCooldown();
        }
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
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
