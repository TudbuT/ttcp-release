package de.tudbut.mod.client.ttcp.mods.misc;

import de.tudbut.mod.client.ttcp.gui.GuiTTC;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Misc;
//import de.tudbut.mod.client.ttcp.utils.ttcic.ControlCenter;
//import de.tudbut.mod.client.ttcp.utils.ttcic.packet.ds.PacketPlayer;

@Misc
public class AltControlRewrite extends Module {
    
    @Override
    public void updateBinds() {
        // subComponents.clear();
        // if(!ControlCenter.isRunning()) {
        //     subComponents.add(new Button("Start Server", it -> {
        //         subComponents.clear();
        //         subComponents.add(new Button("Starting...", a -> {}));
        //         ControlCenter.server();
        //         updateBinds();
        //     }));
        //     subComponents.add(new Button("Start Client", it -> {
        //         subComponents.clear();
        //         subComponents.add(new Button("Starting...", a -> {}));
        //         ControlCenter.client();
        //         updateBinds();
        //     }));
        // }
        // else {
        //     subComponents.add(new Button("Stop", it -> {
        //         subComponents.clear();
        //         ControlCenter.stop = true;
        //         new Thread(() -> {
        //             subComponents.add(new Button("Stopped.", a -> {}));
        //             while (ControlCenter.isRunning());
        //             updateBinds();
        //         }).start();
        //     }));
        //     subComponents.add(new Button("Group", it -> {
        //         for (PacketPlayer packetPlayer : ControlCenter.getGroup()) {
        //             ChatUtils.print(packetPlayer.name);
        //         }
        //     }));
        // }
    }
    
    @Override
    public void onTick() {
        //ControlCenter.onTick();
    }
}
