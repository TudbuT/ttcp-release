package de.tudbut.mod.client.ttcp.mixin;

import net.minecraftforge.fml.common.network.NetworkRegistry;

import io.netty.channel.ChannelHandler;

import java.util.EnumMap;

import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.network.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;

import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(NetworkRegistry.class)
public class MixinFMLNetworkRegistry {
    @Shadow(remap = false)
    private EnumMap<Side,Map<String,FMLEmbeddedChannel>> channels = Maps.newEnumMap(Side.class);

    @Overwrite(remap = false)
    public EnumMap<Side,FMLEmbeddedChannel> newChannel(ModContainer container, String name, ChannelHandler... handlers)
    {
        if (channels.get(Side.CLIENT).containsKey(name) || channels.get(Side.SERVER).containsKey(name) || name.startsWith("MC|") || name.startsWith("\u0001") || (name.startsWith("FML") && !("FML".equals(container.getModId()))))
        {
            throw new RuntimeException("That channel is already registered");
        }
        EnumMap<Side,FMLEmbeddedChannel> result = Maps.newEnumMap(Side.class);

        for (Side side : Side.values())
        {
            try {
                FMLEmbeddedChannel channel = new FMLEmbeddedChannel(container, name, side, handlers);
                channels.get(side).put(name,channel);
                result.put(side, channel);
            } catch (Exception e) {
                System.err.println("error initializing side " + side + ", probably due to forge literally deleting all version control history of mergetool 1.0.12, fuck you forge. youre just trying to get people to use a newer forgegradle but im not gonna let that happen, fuck you and please restore mergetool 1.0.12!");
            }
        }
        return result;
    }
}
