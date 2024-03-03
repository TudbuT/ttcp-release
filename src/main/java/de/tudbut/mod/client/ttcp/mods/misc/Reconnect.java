package de.tudbut.mod.client.ttcp.mods.misc;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.fml.client.FMLClientHandler;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Misc;

@Misc
public class Reconnect extends Module {
    public void onEnable() {
        toggle();
        if(!mc.isIntegratedServerRunning()) {
            ServerData data = mc.getCurrentServerData();
            mc.loadWorld(null);
            FMLClientHandler.instance().connectToServer(new GuiMultiplayer(new GuiMainMenu()), data);
        }
        else {
            mc.world.sendQuittingDisconnectingPacket();
            mc.displayGuiScreen(new GuiWorldSelection(new GuiMainMenu()));
        }
    }
}
