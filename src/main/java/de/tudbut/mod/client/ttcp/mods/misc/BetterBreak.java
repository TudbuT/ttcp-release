package de.tudbut.mod.client.ttcp.mods.misc;

import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Misc;

@Misc
public class BetterBreak extends Module {
    
    @SubscribeEvent
    public void onBreakSpeedGet(PlayerEvent.BreakSpeed event) {
        if(!enabled)
            return;

        float f = event.getOriginalSpeed();
    
        if (event.getEntityPlayer().isInsideOfMaterial(Material.WATER) && !EnchantmentHelper.getAquaAffinityModifier(event.getEntityPlayer()))
        {
            f *= 5.0F;
        }
    
        if (!event.getEntityPlayer().onGround)
        {
            f *= 5.0F;
        }
    
        event.setNewSpeed(f);
    }
    
    @Override
    public void onChat(String s, String[] args) {
    
    }
}
