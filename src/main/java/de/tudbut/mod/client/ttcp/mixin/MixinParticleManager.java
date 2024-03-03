package de.tudbut.mod.client.ttcp.mixin;

import akka.dispatch.AbstractNodeQueue;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEmitter;
import net.minecraft.client.particle.ParticleManager;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(ParticleManager.class)
public class MixinParticleManager {
    
    @Shadow
    private Queue<ParticleEmitter> particleEmitters;
    @Shadow
    private Queue<Particle> queue;
    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    @Shadow
    private ArrayDeque<Particle>[][] fxLayers;
    
    /**
     * @author
     */
    @Overwrite
    public void updateEffects() {
        try {
            for (int i = 0 ; i < 4 ; ++i) {
                this.updateEffectLayer(i);
            }
    
            if (!this.particleEmitters.isEmpty()) {
                List<ParticleEmitter> list = Lists.<ParticleEmitter>newArrayList();
        
                ParticleEmitter[] particleEmitters = this.particleEmitters.toArray(new ParticleEmitter[0]);
                for (ParticleEmitter particleemitter : particleEmitters) {
                    particleemitter.onUpdate();
            
                    if (!particleemitter.isAlive()) {
                        list.add(particleemitter);
                    }
                }
        
                this.particleEmitters.removeAll(list);
            }
    
            if (!this.queue.isEmpty()) {
                for (Particle particle = this.queue.poll() ; particle != null ; particle = this.queue.poll()) {
                    int j = particle.getFXLayer();
                    int k = particle.shouldDisableDepth() ? 0 : 1;
            
                    if (this.fxLayers[j][k].size() >= 16384) {
                        this.fxLayers[j][k].removeFirst();
                    }
            
                    this.fxLayers[j][k].add(particle);
                }
            }
        } catch (Exception ignored) { }
    }
    
    @Shadow
    private void updateEffectLayer(int i) {
    
    }
}
