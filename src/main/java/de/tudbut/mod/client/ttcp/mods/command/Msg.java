package de.tudbut.mod.client.ttcp.mods.command;

import de.tudbut.api.RequestResult;
import de.tudbut.parsing.TCN;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.WebServices2;
import de.tudbut.mod.client.ttcp.utils.category.Chat;
import de.tudbut.mod.client.ttcp.utils.ThreadManager;

import java.io.IOException;

@Chat
public class Msg extends Module {
    
    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public void onEveryChat(String s, String[] args) {
        ThreadManager.run(() -> {
            if(args.length == 0) {
                ChatUtils.print("§aPlayers online: " + WebServices2.client.getOnline().success(TCN.class).apply(it -> it.getArray("names")).apply(it -> String.join(" ", it.toArray(new String[0]))).get());
                return;
            }
            String name = args[0];
            RequestResult<?> result = WebServices2.sendMessage(name, s.substring(name.length() + 1));
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
