package de.tudbut.mod.client.ttcp.mods.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.player.EntityPlayer;
import de.tudbut.mod.client.ttcp.gui.lib.component.Slider;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Utils;
import de.tudbut.mod.client.ttcp.utils.category.Combat;
import de.tudbut.obj.Save;

@Combat
public class Reach extends Module {
    
    @Save
    public static float reach = 3;
    @Save
    public static float breach = 5;
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new Slider("Entities", this, "reach", f -> String.valueOf(Math.round(f * 100) / 100f), 6, 3));
        subComponents.add(new Slider("Blocks", this, "breach", f -> String.valueOf(Math.round(f * 100) / 100f), 5, 5));
    }
    
    public static final IAttribute REACH_DISTANCE = (IAttribute) Utils.getPrivateField(EntityPlayer.class, null, Utils.getFieldsForType(EntityPlayer.class, IAttribute.class)[0]);
    
    @Override
    public void onDisable() {
        Minecraft.getMinecraft().player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).setBaseValue(5);
    }
    
    @Override
    public void onTick() {
        Minecraft.getMinecraft().player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).setBaseValue(breach);
    }
}
