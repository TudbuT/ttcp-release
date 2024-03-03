package de.tudbut.mod.client.ttcp.utils.pathfinding;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class Node extends BlockPos {
    int f = 0; // "Cost"
    int g = 0; // Distance to start
    int h = 0; // Direct distance to end
    int n = 1;
    Node parent;
    
    public Node(int x, int y, int z) {
        super(x, y, z);
    }
    
    public Node(double x, double y, double z) {
        super(x, y, z);
    }
    
    public Node(Entity source) {
        super(source);
    }
    
    public Node(Vec3d vec) {
        super(vec);
    }
    
    public Node(Vec3i source) {
        super(source);
    }
    
    void calcFGH(Node end, int i, boolean invertH) {
        int dx, dy = 0, dz;
        
        if(parent != null) {
            g = parent.g;
            n += parent.n;
        }
        g += i;
        
        dx = end.getX() - getX();
        if(end.getY() != -1) {
            dy = end.getY() - getY();
        }
        dz = end.getZ() - getZ();
        h = dx * dx + dy * dy + dz * dz;
        if(invertH)
            h = -h;
        
        f = g+h;
    }
    
    
    public Node getParent() {
        return parent;
    }

    public boolean equals(Object o) {
        if(super.equals(o))
            return true;
        if(!(o instanceof BlockPos)) {
            return false;
        }
        BlockPos p = (BlockPos) o;
        return getY() == -1 && getX() == p.getX() && getZ() == p.getZ();
    }
}
