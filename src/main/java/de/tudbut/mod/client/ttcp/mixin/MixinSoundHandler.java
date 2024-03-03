package de.tudbut.mod.client.ttcp.mixin;

import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ConcurrentModificationException;

@Mixin(SoundHandler.class)
public class MixinSoundHandler {
    
    @Shadow
    SoundManager sndManager;

    /**
     * @author
     */
    @Overwrite
    public void update() {
        try {
            this.sndManager.updateAllSounds();
        }
        catch (ConcurrentModificationException ignore) {
            this.update();
        }
    }
}
