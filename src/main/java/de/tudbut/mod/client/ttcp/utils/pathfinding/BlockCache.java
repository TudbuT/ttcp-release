package de.tudbut.mod.client.ttcp.utils.pathfinding;

import net.minecraft.util.math.BlockPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.init.Blocks;
import de.tudbut.tools.Cache;

import java.util.HashMap;

public class BlockCache {
    private HashMap<World, Cache<BlockPos, IBlockState>> map = new HashMap<>();

    public IBlockState getBlock(World world, BlockPos pos) {
        Cache<BlockPos, IBlockState> cache = map.get(world);
        if(cache == null) {
            cache = new Cache<>();
            map.put(world, cache);
        }
        IBlockState state = cache.get(pos);
        if(state == null) {
            state = world.getBlockState(pos);
            cache.add(pos, state, 5000, new Cache.CacheRetriever<BlockPos, IBlockState>() { 
                @Override
                public IBlockState retrieveFromKey(BlockPos k) {
                    return world.getBlockState(k);
                }
            });
        }
        return state;
    }

    public boolean isAirBlock(World world, BlockPos pos) {
        return getBlock(world, pos).getBlock() == Blocks.AIR;
    }
}
