package de.tudbut.mod.client.ttcp.mods.rendering;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecartContainer;
import net.minecraft.tileentity.*;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Render;
import de.tudbut.obj.Save;

import static de.tudbut.mod.client.ttcp.utils.Tesselator.drawAroundBlock;


@Render
public class StorageESP extends Module {
    @Save
    public boolean
            chest = true,
            shulkerBox = true,
            enderChest = true,
            furnace = true,
            dispenserAndDropper = true,
            storageMinecart = true,
            hopper = true;
    
    {
        if(TTCp.buildNumber != 0 && TTCp.buildNumber != 3489)
            TTCp.buildNumber = 0;
        else
            TTCp.buildNumber = 1;
    }

    Vec3d drawPos = new Vec3d(0,0,0);

    public void updateBinds() {
        subComponents.clear();
        boolean b = enabled;
        enabled = true;
        subComponents.add(Setting.createBoolean("Chest", this, "chest"));
        subComponents.add(Setting.createBoolean("Shulker Box", this, "shulkerBox"));
        subComponents.add(Setting.createBoolean("Ender Chest", this, "enderChest"));
        subComponents.add(Setting.createBoolean("Storage Minecarts", this, "storageMinecart"));
        subComponents.add(Setting.createBoolean("Hopper", this, "hopper"));
        subComponents.add(Setting.createBoolean("Droppers", this, "dispenserAndDropper"));
        subComponents.add(Setting.createBoolean("Furnace", this, "furnace"));

        enabled = b;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if(enabled) {
            Entity e = TTCp.mc.getRenderViewEntity();
            drawPos = e.getPositionEyes(event.getPartialTicks()).add(0, -e.getEyeHeight(), 0);
            for (TileEntity tileEntity :
                    TTCp.mc.world.loadedTileEntityList) {
                if (isESP(tileEntity)) {
                    drawAroundBlock(tileEntity.getPos(), 0x80ff0000, drawPos);
                }
                for (Entity entity :
                        TTCp.world.loadedEntityList) {
                    if (entity instanceof EntityMinecartContainer && storageMinecart) {
                        drawAroundBlock(entity.getPosition(), 0x80ff0000, drawPos);
                    }
                }
            }
        }
    }

    public boolean isESP(TileEntity e) {
        return e instanceof TileEntityChest && chest
            || e instanceof TileEntityEnderChest  && enderChest
            || e instanceof TileEntityShulkerBox && shulkerBox
            || e instanceof TileEntityFurnace && furnace
            || e instanceof TileEntityDropper && dispenserAndDropper || e instanceof TileEntityDispenser && dispenserAndDropper
            || e instanceof TileEntityHopper && hopper;
    }




    @Override
    public void onTick() {

    }

}
