package de.tudbut.mod.client.ttcp.mods.command;

import de.tudbut.mod.client.ttcp.utils.ChatUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.category.Command;
import de.tudbut.net.http.HTTPUtils;
import de.tudbut.parsing.ArgumentParser;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Command
public class Dev extends Module {
    @Override
    public boolean displayOnClickGUI() {
        return false;
    }
    
    /*
    @Override
    public void onEveryChat(String str, String[] args) {
        System.out.println(args.length);
        if(args.length == 0) {
            ChatUtils.print(",dev -rdesp -l <login> -n <name> [-x <pwd>]");
            ChatUtils.print("r: Remove premium");
            ChatUtils.print("d: Disable");
            ChatUtils.print("e: Enable");
            ChatUtils.print("s: Disable once");
            ChatUtils.print("p: Set premium password");
            ChatUtils.print("l: Admin password");
            ChatUtils.print("n: Name");
            ChatUtils.print("x: New password for -p");
            return;
        }
        ChatUtils.print(Arrays.toString(args));
        
        Map<String, String> arguments = ArgumentParser.parseDefault(args);
    
        boolean r = "true".equals(arguments.get("r"));
        boolean d = "true".equals(arguments.get("d"));
        boolean e = "true".equals(arguments.get("e"));
        boolean s = "true".equals(arguments.get("s"));
        boolean p = "true".equals(arguments.get("p"));
        String l = arguments.getOrDefault("l", " ");
        String password = arguments.getOrDefault("x", "-");
    
        ChatUtils.print("Asking mojang for the UUID of " + arguments.get("n") + " (with SSL)...");
        TudbuTAPI.getUUIDFromMojang(arguments.get("n")).then(uuid -> {
            ChatUtils.print(arguments.get("n") + " is " + uuid + ".");
    
            ChatUtils.print("Calling api.tudbut.de...");
            if(d)
                d(uuid, l);
            if(s)
                s(uuid, l);
            if(r)
                r(uuid, l);
            if(e)
                e(uuid, l);
            if(p)
                p(uuid, l, password);
            ChatUtils.print("Done.");
        }).ok();
    }
    
    private static void d(UUID uuid, String l) {
        TudbuTAPI
                .get("admin/deactivate", "uuid=" + uuid + "&key=" + HTTPUtils.encodeUTF8(l))
                .err(Throwable::printStackTrace)
                .ok()
                .await();
    }
    
    private static void s(UUID uuid, String l) {
        TudbuTAPI
                .get("admin/triggerDeactivate", "uuid=" + uuid + "&key=" + HTTPUtils.encodeUTF8(l))
                .err(Throwable::printStackTrace)
                .ok()
                .await();
    }
    
    private static void r(UUID uuid, String l) {
        TudbuTAPI
                .get("admin/remove", "uuid=" + uuid + "&key=" + HTTPUtils.encodeUTF8(l))
                .err(Throwable::printStackTrace)
                .ok()
                .await();
    }
    
    private static void p(UUID uuid, String l, String pwd) {
        TudbuTAPI
                .get("admin/setPassword", "uuid=" + uuid + "&key=" + HTTPUtils.encodeUTF8(l) + "&password=" + HTTPUtils.encodeUTF8(pwd))
                .err(Throwable::printStackTrace)
                .ok()
                .await();
    }
    
    private static void e(UUID uuid, String l) {
        TudbuTAPI
                .get("admin/enable", "uuid=" + uuid + "&key=" + HTTPUtils.encodeUTF8(l))
                .err(Throwable::printStackTrace)
                .ok()
                .await();
    }
    
     */
}
