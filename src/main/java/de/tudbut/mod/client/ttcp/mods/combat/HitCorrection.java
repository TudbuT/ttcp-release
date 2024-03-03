package de.tudbut.mod.client.ttcp.mods.combat;

import net.minecraft.entity.EntityLivingBase;
import de.tudbut.mod.client.ttcp.gui.lib.component.Slider;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Utils;
import de.tudbut.mod.client.ttcp.utils.category.Combat;
import de.tudbut.obj.Save;

import java.lang.reflect.Field;

@Combat
public class HitCorrection extends Module {

    @Save
    public static float amount;

    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(new Slider("Amount", this, "amount", f -> String.valueOf(Math.round(f * 100) / 100f), 5, 0));
    }

    @Override
    public void onTick() {
        try {
            Field ticksSinceLastSwing = EntityLivingBase.class.getDeclaredField(Utils.getFieldsForType(EntityLivingBase.class, int.class)[5]);
            ticksSinceLastSwing.setAccessible(true);
            ticksSinceLastSwing.setInt(mc.player, 100);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
