package de.tudbut.mod.client.ttcp.mods.command;

import de.tudbut.api.RequestResult;
import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.ThreadManager;
import de.tudbut.mod.client.ttcp.utils.WebServices2;
import de.tudbut.mod.client.ttcp.utils.category.Command;

@Command
public class Password extends Module {
    
    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    @Override
    public boolean doStoreEnabled() {
        return false;
    }
    
    @Override
    public boolean defaultEnabled() {
        return true;
    }
    
    @Override
    public void onChat(String s, String[] args) {
        ThreadManager.run(() -> {
            if (args.length == 2) {
                WebServices2.client.setPassword(args[0], args[1]);
            }
            if (args.length == 1) {
                ChatUtils.print("Authorizing for password reset with GameAuth...");
                if (WebServices2.client.authorizeWithGameAuth(mc.getSession().getToken()).result == RequestResult.Type.SUCCESS) {
                    ChatUtils.print("Setting password...");
                    WebServices2.client.setPassword(args[0]);
                    ChatUtils.print("Done. Thank you!");
                    WebServices2.client.unauthorize();
                }
                else {
                    ChatUtils.print("Failed to authorize. Your minecraft session probably expired or is invalid.");
                }
            }
        });
    }
}
