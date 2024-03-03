package de.tudbut.mod.client.ttcp.utils.pathfinding;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Comparator;
import java.util.Date;

public class AStar {

    public static class Result {
        private Result(Node[][] nodes, boolean didAvoid, boolean didEarlyExit) {
            this.nodes = nodes;
            this.didAvoid = didAvoid;
            this.didEarlyExit = didEarlyExit;
        }

        public Node[][] nodes;
        public boolean didAvoid;
        public boolean didEarlyExit;
    }
    
    public static Result calculate(
            BlockPos start, BlockPos end, World world, 
            int distance, int length, int timeout, int earlyExitTimeout, 
            AtomicReference<ArrayList<Node>> closedRef, AtomicReference<ArrayList<Node>> openRef, 
            HashSet<BlockPos> avoid, int avoidWeight, HashSet<BlockPos> deny, 
            boolean onlyScanWalls, int minimumClosedBlocks, boolean panic
    ) {
        BlockCache cache = new BlockCache();
        long sa = new Date().getTime();
        if(panic)
            minimumClosedBlocks = 0;

        try {
            boolean didAvoid = false;
            AtomicBoolean didAvoidA = new AtomicBoolean(false);
            ArrayList<Node> open = new ArrayList<>(), closed = new ArrayList<>();
            openRef.set(open);
            closedRef.set(closed);
            Node startNode = new Node(start), endNode = new Node(end);
    
            startNode.calcFGH(endNode, 0, panic);
            open.add(startNode);
            Node current = startNode;
    
            boolean earlyExit = false;
            w: while (!endNode.equals(current)) {
                if(System.currentTimeMillis() - sa > timeout * 1000L || closedRef.get() == null) {
                    current = null;
                    break;
                }
                if(open.size() == 0) {
                    ChatUtils.print("§a[TTC] §r[ElytraBot] §cUnable to find a path (open.length == 0). Stopping.");
                    current = null;
                    return new Result(new Node[2][0], false, false);
                }
                current = open.get(0);
                int currentID = 0;
                for (int i = 1; i < open.size(); i++) {
                    Node node = open.get(i);
                    if (node.f < current.f) {
                        current = node;
                        currentID = i;
                    }
                }
        
                open.remove(currentID);
                if(panic) {
                    for(Node n : open) {
                        n.f += 10 * 1000;
                    }
                }
                closed.add(current);
        
                ArrayList<BlockPos> next = getAdjacent3D(current);
        
                if(!earlyExit)
                    earlyExit = System.currentTimeMillis() - sa > earlyExitTimeout * 1000L;
                for (int i = 0; i < next.size(); i++) {
                    BlockPos bp = next.get(i);
    
                    int work = calcWork(cache, world, bp, current, onlyScanWalls || earlyExit, avoid, avoidWeight, didAvoid ? null : didAvoidA, deny);
                    if(work != -1) {
                        Node n = new Node(bp);
                        if(closed.indexOf(n) != -1)
                            continue;
                        n.parent = current;
                        if(end.getY() == -1) {
                            int tmp = bp.getY() - current.getY();
                            work += tmp * tmp * 10;
                        }
                        n.calcFGH(endNode, work, panic);
                        if (Math.abs(startNode.getX() - n.getX()) >= distance || Math.abs(startNode.getZ() - n.getZ()) >= distance || n.n > length) {
                            if(closed.size() < minimumClosedBlocks)
                                continue;
                            break w;
                        }
                        int b;
                        if((b = open.indexOf(n)) != -1 && open.get(b).g <= n.g)
                            continue;
                        if(b != -1)
                            open.remove(b);
                        open.add(n);
                    }
                }
                if(didAvoidA.get())
                    didAvoid = true;
            }
    
    
            ArrayList<Node> list = new ArrayList<>();
    
            Node n = current == null ? closed.stream().max((n1, n2) -> n1.n - n2.n).get() : current; // The end node
            while (n.getParent() != null) {
                list.add(n);
                n = n.getParent();
            }
    
            list.add(startNode);
    
            Node[] nodes = new Node[list.size()];
    
            // Reverse
            for (int i = 0; i < nodes.length; i++) {
                nodes[i] = list.get(nodes.length - 1 - i);
            }
    
            return new Result(new Node[][] {nodes, closed.toArray(new Node[0])}, didAvoid, earlyExit);
        } catch (IndexOutOfBoundsException e) {
            ChatUtils.print("§a[TTC] §r[ElytraBot] §cUnable to find a path. Stopping.");
            e.printStackTrace();
            return new Result(new Node[2][0], false, false);
        }
    }
    
    private static int calcWork(BlockCache cache, World world, BlockPos bp, BlockPos prev, boolean requireSurround, HashSet<BlockPos> avoid, int avoidWeight, AtomicBoolean didAvoid, HashSet<BlockPos> deny) {
        BlockPos[] positions = getAntiDMG3D(bp).toArray(new BlockPos[0]);
        int work = 0, tmp;
        tmp = bp.getX() - prev.getX();
        work += tmp * tmp;
        tmp = bp.getY() - prev.getY();
        work += tmp * tmp;
        tmp = bp.getZ() - prev.getZ();
        work += tmp * tmp;
        boolean block = false;
        for (int i = 0; i < positions.length; i++) {
            BlockPos b = positions[i];
            if(!world.isBlockLoaded(b))
                return -1;
            if(!world.isAirBlock(b))
                return -1;
            if(deny.contains(b))
                return 1000 * 1000;
        }
        positions = getAdjacent3D(bp).toArray(new BlockPos[0]);
        for (int i = 0; i < positions.length; i++) {
            BlockPos b = positions[i];
            if(avoid.contains(b)) {
                work += avoidWeight * 1000;
                if(didAvoid != null)
                    didAvoid.set(true);
            }
        }
        if(requireSurround) {
            positions = getFastMode3D(bp).toArray(new BlockPos[0]);
            block = true;
            for(int i = 0; i < positions.length; i++) {
                bp = positions[i];
                if(!world.isAirBlock(bp))
                    block = false;
            }
        }
        if(block)
            return 10 * 1000 * 1000;
        return work;
    }
    
    public static ArrayList<BlockPos> getAdjacent3D(BlockPos pos) {
        ArrayList<BlockPos> r = new ArrayList<>();
        
        // Main
        r.add(pos.add(-1,  0, -1));
        r.add(pos.add(-1,  0,  0));
        r.add(pos.add(-1,  0, +1));
        r.add(pos.add( 0,  0, -1));
        // Don't add current!
        r.add(pos.add( 0,  0, +1));
        r.add(pos.add(+1,  0, -1));
        r.add(pos.add(+1,  0,  0));
        r.add(pos.add(+1,  0, +1));
        
        // Bottom
        r.add(pos.add(-1, -1, -1));
        r.add(pos.add(-1, -1,  0));
        r.add(pos.add(-1, -1, +1));
        r.add(pos.add( 0, -1, -1));
        r.add(pos.add( 0, -1,  0));
        r.add(pos.add( 0, -1, +1));
        r.add(pos.add(+1, -1, -1));
        r.add(pos.add(+1, -1,  0));
        r.add(pos.add(+1, -1, +1));
        
        // Top
        r.add(pos.add(-1, +1, -1));
        r.add(pos.add(-1, +1,  0));
        r.add(pos.add(-1, +1, +1));
        r.add(pos.add( 0, +1, -1));
        r.add(pos.add( 0, +1,  0));
        r.add(pos.add( 0, +1, +1));
        r.add(pos.add(+1, +1, -1));
        r.add(pos.add(+1, +1,  0));
        r.add(pos.add(+1, +1, +1));
        
        return r;
    }

    public static ArrayList<BlockPos> getAntiDMG3D(BlockPos pos) {
        ArrayList<BlockPos> r = new ArrayList<>();
        
        for(int x = -3; x <= 3; x++) {
            for(int y = -3; y <= 1; y++) {
                for(int z = -3; z <= 3; z++) {
                    if(!(x == 0 && y == 0 && z == 0)) {
                        r.add(pos.add(x,y,z));
                    }
                }
            }
        }
        
        return r;
    }

    public static ArrayList<BlockPos> getFastMode3D(BlockPos pos) {
        ArrayList<BlockPos> r = new ArrayList<>();
        
        for(int x = -4; x <= 4; x++) {
            for(int y = -4; y <= 2; y++) {
                for(int z = -4; z <= 4; z++) {
                    if(!(Math.abs(x) < 4 && (y > -4 && y < 2) && Math.abs(z) < 4)) {
                        r.add(pos.add(x,y,z));
                    }
                }
            }
        }
        
        return r;
    }
}
