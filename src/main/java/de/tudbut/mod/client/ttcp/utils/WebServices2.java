package de.tudbut.mod.client.ttcp.utils;

import de.tudbut.api.RequestResult;
import de.tudbut.api.TudbuTAPIClient;
import de.tudbut.tools.Nullable;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.parsing.TCN;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @author TudbuT
 * @since 29 Jul 2022
 */

public class WebServices2 {
    public static TudbuTAPIClient client = new TudbuTAPIClient("ttc", TTCp.mc.getSession().getProfile().getId(), "api.tudbut.de", 99);
    static UUID lastMessaged = null;
    static String lastMessagedName = null;
    
    
    public static boolean handshake() {
        RequestResult<?> result = client.login(TTCp.MODID + " " + TTCp.NAME.replace(' ', '_') + " " + TTCp.BRAND + "@" + TTCp.VERSION);
        return result.result == RequestResult.Type.SUCCESS;
    }
    
    public static boolean play() {
        RequestResult<?> result = client.use();
        if(result.result == RequestResult.Type.SUCCESS) {
            if (client.hasNewMessages()) {
                return new Nullable<ArrayList<TCN>>(client.getMessages().success(ArrayList.class).get()).apply(msgs -> {
                    for (int i = 0 ; i < msgs.size() ; i++) {
                        TCN msg = msgs.get(i);
                        if(msg.getBoolean("global")) {
                            ChatUtils.print("§a[TTC] §r[WebServices] <" + msg.getSub("from").getString("name") + "> " + msg.getString("content"));
                        }
                        else {
                            ChatUtils.print("§a[TTC] §r[WebServices] §c[DIRECT] §r" + msg.getSub("from").getString("name") + ": " + msg.getString("content"));
                            lastMessaged = UUID.fromString(msg.getString("fromUUID"));
                            lastMessagedName = null;
                        }
                    }
                    return msgs;
                }).get() != null;
            }
            if(client.hasNewDataMessages()) {
                client.getDataMessages().success(ArrayList.class).apply(l -> (ArrayList<TCN>) l).consume(l -> {
                    for (int i = 0 ; i < l.size() ; i++) {
                        TCN msg = l.get(i);
                        if(msg.getString("type").equals("announcement")) {
                            ChatUtils.print(msg.getString("toPrint"));
                        }
                        if(msg.getString("type").equals("disable")) {
                            KillSwitch.deactivate();
                        }
                    }
                });
            }
            if(Boolean.TRUE.equals(client.serviceData().getBoolean("disable"))) {
                KillSwitch.deactivate();
            }
            return true;
        }
        return false;
    }
    
    public static RequestResult<?> sendMessage(String user, String message) {
        if(user == null) {
            if(lastMessagedName != null) {
                return client.sendMessage(lastMessagedName, message);
            }
            if(lastMessaged != null) {
                return client.sendMessage(lastMessaged, message);
            }
            return RequestResult.FAIL("Unable to find last messaged user");
        } else {
            RequestResult<?> result = client.sendMessage(user, message);
            lastMessagedName = user;
            return result;
        }
    }
}
