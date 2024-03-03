package de.tudbut.mod.client.ttcp.mods.misc;

import net.minecraft.util.math.BlockPos;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.utils.BlockUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Misc;
import de.tudbut.obj.Save;

@Misc
public class Flatten extends Module {
    
    @Save
    public boolean autoSelect = false;
    
    @Override
    public void updateBinds() {
        subComponents.clear();
        subComponents.add(Setting.createBoolean("AutoSelect", this, "autoSelect"));
    }
    
    @Override
    public void onTick() {
        BlockPos pos = BlockUtils.getRealPos(TTCp.player.getPositionVector());
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos block = pos.add(x, -1, z);
                /*if(autoSelect)
                    AutoCrystal.getInstance().selectObby();*/
                if(TTCp.world.isAirBlock(block))
                    BlockUtils.placeBlock(block, true);
            }
        }
    }
}
