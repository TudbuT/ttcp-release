package de.tudbut.mod.client.ttcp.mods.command;

import de.tudbut.api.RequestResult;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.WebServices2;
import de.tudbut.mod.client.ttcp.utils.category.Command;
import de.tudbut.mod.client.ttcp.utils.ThreadManager;

import java.util.UUID;

@Command
public class R extends Module {
    
    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public void onEveryChat(String s, String[] args) {
        ThreadManager.run(() -> {
            RequestResult<?> result = WebServices2.sendMessage(null, s);
            System.out.println(result);
            if(result.result == RequestResult.Type.SUCCESS) {
                ChatUtils.print("§a[TTC] §r[WebServices] §aSuccessfully sent message.");
            }
            else {
                ChatUtils.print("§a[TTC] §r[WebServices] §cFailed to send message.");
            }
        });
    }
}
