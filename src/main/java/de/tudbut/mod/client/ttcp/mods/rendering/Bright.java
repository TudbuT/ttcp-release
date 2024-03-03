package de.tudbut.mod.client.ttcp.mods.rendering;

import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Render;

@Render
public class Bright extends Module
{
    @Override
    public void onChat(String s, String[] args) {
    
    }
    
    public void onEveryTick() {
        if (enabled) {
            PotionEffect p;
            TTCp.player.addPotionEffect(p = new PotionEffect(
                    MobEffects.NIGHT_VISION,
                    1000,
                    127,
                    true,
                    false
            ));
            p.setPotionDurationMax(true);
        } else
           TTCp.player.removeActivePotionEffect(MobEffects.NIGHT_VISION);
    
    }
}
