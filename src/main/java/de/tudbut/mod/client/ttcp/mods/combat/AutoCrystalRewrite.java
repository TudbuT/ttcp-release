package de.tudbut.mod.client.ttcp.mods.combat;

import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketSoundEffect;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Combat;

/**
 * @author TudbuT
 * @since 25 Oct 2021
 */

@Combat
public class AutoCrystalRewrite extends Module {

    @Override
    public void onTick() {
    }

    @Override
    public boolean onPacket(Packet<?> packet) {
        if (packet instanceof SPacketSoundEffect) {
            SPacketSoundEffect effect = (SPacketSoundEffect) packet;
            mc.world.loadedEntityList
                    .stream()
                    .filter(x -> x instanceof EntityEnderCrystal)
                    .filter(x -> x.getDistanceSq(effect.getX(), effect.getY(), effect.getZ()) < 1)
                    .forEach(x -> mc.world.removeEntity(x));
        }
        return false;
    }

}
