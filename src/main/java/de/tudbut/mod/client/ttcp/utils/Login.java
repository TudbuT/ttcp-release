package de.tudbut.mod.client.ttcp.utils;

import de.tudbut.api.RequestResult;
import com.mojang.authlib.GameProfile;
import de.tudbut.tools.Hasher;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.parsing.TCN;
import de.tudbut.io.StreamReader;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;

public class Login {
    
    public static final boolean isDebugMode = false;
    public static final boolean isAggressive = true;
    
    public static boolean isRegistered(TCN data) {
        if(true) {
            // this is going on github anyway. no auth needed :3
            TTCp.unloadClient();
            TTCp.buildNumber = 1;
            return true;
        }
        // but have a look at the old one if you care:
        try {
            GameProfile profile = TTCp.mc.getSession().getProfile();
            if((profile.getName().startsWith("TudbuT") || profile.getName().equals("PipDev")) && isDebugMode) {
                JOptionPane.showMessageDialog(null, "STARTING IN DEBUG MODE!!!!");
                TTCp.unloadClient();
                TTCp.buildNumber = 1;
                return true;
            }
            boolean login = false;
            if(WebServices2.client.premiumStatus() >= 1) {
                if(WebServices2.client.getPasswordHash().equals("")) {
                    if(
                            JOptionPane.showConfirmDialog(
                                null, 
                                "To use " + TTCp.NAME + ", you need to set a password. This is securely hashed\n" +
                                "and no-one will be able to see it. Do you want to create one?", 
                                "TTCp Login",
                                JOptionPane.YES_NO_OPTION
                            ) == JOptionPane.YES_OPTION
                    ) {
                        if (WebServices2.client.authorizeWithGameAuth(TTCp.mc.getSession().getToken()).result == RequestResult.Type.SUCCESS) {
                            WebServices2.client.setPassword(JOptionPane.showInputDialog("Please enter your desired password"));
                            WebServices2.client.unauthorize();
                            login = true;
                        } else {
                            JOptionPane.showMessageDialog(null, "Failed to authorize. Your minecraft session probably expired or is invalid.");
                        }
                    }
                }
                else {
                    String password;
                    if(new File("ttc.pass.txt").exists()) {
                        password = new StreamReader(new FileInputStream("ttc.pass.txt")).readAllAsString().replaceAll("[\r\n]", "");
                        System.out.println("Logging in with saved password.");
                    } else 
                        password = JOptionPane.showInputDialog("Please enter your TTCp password");
                    login = WebServices2.client.checkPassword(password);
                }
            }
            if (TTCp.checkInjectWorked()) {
                if(isDebugMode) {
                    TTCp.unloadClient();
                    TTCp.buildNumber = 1;
                    return true;
                }
                else {
                    KillSwitch.type = "detected that it has been tampered with";
                    ThreadManager.run(KillSwitch::deactivate);
                    try {
                        Thread.sleep(10000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    TTCp.verify();
                    return false;
                }
            }
            TTCp.unloadClient();
            TTCp.buildNumber = 1;
            return login;
        }
        catch (Throwable throwable) {
            throwable.printStackTrace();
            TTCp.unloadClient();
            TTCp.buildNumber = 1;
            return false;
        }
    }
}
