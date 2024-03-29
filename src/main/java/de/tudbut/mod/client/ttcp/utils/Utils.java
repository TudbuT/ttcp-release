package de.tudbut.mod.client.ttcp.utils;

import com.google.common.base.Predicates;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import de.tudbut.tools.Hasher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.INetHandlerStatusClient;
import net.minecraft.network.status.client.CPacketPing;
import net.minecraft.network.status.client.CPacketServerQuery;
import net.minecraft.network.status.server.SPacketPong;
import net.minecraft.network.status.server.SPacketServerInfo;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import de.tudbut.mod.client.ttcp.TTCp;
import de.tudbut.mod.client.ttcp.events.EventHandler;
import de.tudbut.mod.client.ttcp.mods.misc.AltControl;
import de.tudbut.mod.client.ttcp.mods.command.Friend;
import de.tudbut.mod.client.ttcp.mods.chat.Team;
import de.tudbut.net.http.*;
import de.tudbut.parsing.TCN;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public class Utils { // A bunch of utils that don't deserve their own class, self-explanatory

    private static float rotationX, rotationY;
    private static boolean rotationUpdated = false;

    public static void markRotationSent() {
        rotationUpdated = false;
    }

    public static Vec2f getRotation() {
        if(!rotationUpdated)
            return null;
        return new Vec2f(rotationX, rotationY);
    }

    public static void setRotation(float x, float y) {
        rotationX = x;
        rotationY = y;
        rotationUpdated = true;
    }

    public static void setRotation(Vec2f vec) {
        setRotation(vec.x, vec.y);
    }

    public static float tpsMultiplier() {
        return EventHandler.tps / 20f;
    }
    
    public static long[] getPingToServer(ServerData server) {
        
        server = new ServerData(server.serverName, server.serverIP, server.isOnLAN());
        
        try {
            long sa = new Date().getTime();
            
            AtomicLong pingSentAt = new AtomicLong();
            AtomicBoolean done = new AtomicBoolean(false);
            
            ServerAddress serveraddress = ServerAddress.fromString(server.serverIP);
            final NetworkManager networkmanager;
            networkmanager = NetworkManager.createNetworkManagerAndConnect(InetAddress.getByName(serveraddress.getIP()), serveraddress.getPort(), false);
            
            server.pingToServer = -1L;
            final long[] players = { 1, 1 };
            ServerData finalServer = server;
            networkmanager.setNetHandler(new INetHandlerStatusClient() {
                @Override
                public void onDisconnect(@Nullable ITextComponent reason) {
                    done.set(true);
                }
                
                @Override
                public void handleServerInfo(@Nullable SPacketServerInfo packetIn) {
                    pingSentAt.set(System.currentTimeMillis());
                    networkmanager.sendPacket(new CPacketPing(pingSentAt.get()));
                    try {
                        assert packetIn != null;
                        players[0] = packetIn.getResponse().getPlayers().getOnlinePlayerCount();
                        players[1] = packetIn.getResponse().getPlayers().getMaxPlayers();
                    } catch (Exception ignored) { }
                }
                
                public void handlePong(@Nullable SPacketPong packetIn) {
                    long j = System.currentTimeMillis();
                    finalServer.pingToServer = j - pingSentAt.get();
                    networkmanager.closeChannel(new TextComponentString("Finished"));
                    done.set(true);
                }
            });
            
            networkmanager.sendPacket(new C00Handshake(serveraddress.getIP(), serveraddress.getPort(), EnumConnectionState.STATUS, false));
            networkmanager.sendPacket(new CPacketServerQuery());
            
            while (!done.get() && (System.currentTimeMillis() - sa) < 7500) Thread.sleep(50);
            
            return new long[] { server.pingToServer, players[0], players[1] };
        }
        catch (Throwable ignored) {
            return new long[] { -1, 1, 1 };
        }
    }
    
    public static String getPasswordFor(UUID uuid) throws IOException {
        HTTPRequest request = new HTTPRequest(HTTPRequestType.GET, "api.tudbut.de", 82, "/api/getHashedPassword?uuid=" + uuid.toString());
        HTTPResponse req = request.send();
        return req.parse().getBody();
    }
    
    public static boolean setPassword(String currentPassword, String newPassword) {
        GameProfile profile = TTCp.mc.getSession().getProfile();
        try {
            return new HTTPRequest(HTTPRequestType.GET, "api.tudbut.de", 82, "/api/setPassword?uuid=" + profile.getId().toString() + "&key=" + URLEncoder.encode(currentPassword, "UTF8") + "&password=" + URLEncoder.encode(Hasher.sha512hex(Hasher.sha256hex(newPassword)), "UTF8")).send().parse().getBody().startsWith("Set!");
        }
        catch (IOException e) {
            return false;
        }
    }
    
    public static float roundTo(float f, int p) {
        p = (int) Math.pow(10, p);
        return (float) Math.round(f * p) / p;
    }
    
    public static boolean isCallingFrom(Class<?> clazz) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        for (int i = 0; i < trace.length; i++) {
            if(trace[i].getClassName().equals(clazz.getName())) {
                return true;
            }
        }
        return false;
    }
    
    public static Object getPrivateField(Class<?> clazz, Object instance, String field) {
        try {
            Object t;
            Field f = clazz.getDeclaredField(field);
            boolean b = f.isAccessible();
            f.setAccessible(true);
            t = f.get(instance);
            f.setAccessible(b);
            return t;
        } catch (Exception e) {
            return null;
        }
    }
    public static void setPrivateField(Class<?> clazz, Object instance, String field, Object content) {
        try {
            Field f = clazz.getDeclaredField(field);
            boolean b = f.isAccessible();
            f.setAccessible(true);
            f.set(instance, content);
            f.setAccessible(b);
        } catch (Exception ignored) {
        }
    }
    public static String[] getFieldsForType(Class<?> clazz, Class<?> type) {
        try {
            Field[] all = clazz.getDeclaredFields();
            ArrayList<String> names = new ArrayList<>();
            for (int i = 0; i < all.length; i++) {
                if(all[i].getType() == type) {
                    names.add(all[i].getName());
                }
            }
            return names.toArray(new String[0]);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
    
    public static <T> T[] getEntities(Class<? extends T> entityType, Predicate<? super T> filter) {
        List<T> list = Lists.newArrayList();
        
        List<Entity> loadedEntityList = TTCp.world.loadedEntityList;
        for (int i = 0; i < loadedEntityList.size(); i++) {
            Entity entity4 = loadedEntityList.get(i);
            if (entityType.isAssignableFrom(entity4.getClass()) && filter.test((T) entity4)) {
                list.add((T) entity4);
            }
        }
        
        return list.toArray((T[]) Array.newInstance(entityType, 0));
    }
    
    public static String removeNewlines(String s) {
        if (s == null)
            return null;
        return s.replaceAll("\n", "").replaceAll("\r", "");
    }
    
    public static TCN getData() {
        try {
            URL updateCheckURL = new URL("https://raw.githubusercontent.com/TudbuT/ttcp-data/master/data_main");
            InputStream stream = updateCheckURL.openConnection().getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            
            String s;
            StringBuilder builder = new StringBuilder();
            while ((s = reader.readLine()) != null) {
                builder.append(s).append("\n");
            }
            
            return TCN.read(builder.toString());
        }
        catch (Exception ignore) { }
        return null; // No internet access
    }
    
    public static String getLatestVersion() {
        try {
            //return Objects.requireNonNull(getData()).getString("version");
        }
        catch (Exception ignore) { }
        return TTCp.VERSION; // No internet access
    }
    
    // Transforms Integer[] to int[]
    public static int[] objectArrayToNativeArray(Integer[] oa) {
        // Create the int array tp copy to
        int[] na = new int[oa.length];
        
        // Convert the integers one by one
        for (int i = 0; i < na.length; i++) {
            na[i] = oa[i];
        }
        
        return na;
    }
    
    public static int[] range(int min, int max) {
        int[] r = new int[max - min];
        for (int i = min, j = 0; i < max; i++, j++) {
            r[j] = i;
        }
        return r;
    }
    
    public static int[] add(int[] array0, int[] array1) {
        int[] r = new int[array0.length + array1.length];
        System.arraycopy(array0, 0, r, 0, array0.length);
        System.arraycopy(array1, 0, r, 0 - array0.length, array1.length);
        return r;
    }
    
    public static Map<String, String> stringToMap(String mapStringParsable) {
        HashMap<String, String> map = new HashMap<>();
        
        String[] splitTiles = mapStringParsable.split(";");
        for (int i = 0; i < splitTiles.length; i++) {
            String tile = splitTiles[i];
            String[] splitTile = tile.split(":");
            if (tile.contains(":")) {
                if (splitTile.length == 2)
                    map.put(
                            splitTile[0].replaceAll("%I", ":").replaceAll("%B", ";").replaceAll("%P", "%"),
                            splitTile[1].equals("%N") ? null : splitTile[1].replaceAll("%I", ":").replaceAll("%B", ";").replaceAll("%P", "%")
                    );
                else
                    map.put(splitTile[0].replaceAll("%I", ":").replaceAll("%B", ";").replaceAll("%P", "%"), "");
            }
        }
        
        return map;
    }
    
    public static String mapToString(Map<String, String> map) {
        StringBuilder r = new StringBuilder();
        
        for (String key : map.keySet().toArray(new String[0])) {
            
            r
                    .append(key.replaceAll("%", "%P").replaceAll(";", "%B").replaceAll(":", "%I"))
                    .append(":")
                    .append(map.get(key) == null ? "%N" : map.get(key).replaceAll("%", "%P").replaceAll(";", "%B").replaceAll(":", "%I"))
                    .append(";")
            ;
        }
        
        return r.toString();
    }
    
    public static NetworkPlayerInfo[] getPlayerList() {
        return Minecraft.getMinecraft().getConnection().getPlayerInfoMap().toArray(new NetworkPlayerInfo[0]);
    }
    
    public static NetworkPlayerInfo getPlayerListPlayer(String name) {
        for (NetworkPlayerInfo p : getPlayerList()) {
            if(p.getGameProfile().getName().equals(name)) {
                return p;
            }
        }
        return null;
    }
    
    public static NetworkPlayerInfo getPlayerListPlayerIgnoreCase(String name) {
        for (NetworkPlayerInfo p : getPlayerList()) {
            if(p.getGameProfile().getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }
    
    public static NetworkPlayerInfo getPlayerListPlayer(UUID uuid) {
        for (NetworkPlayerInfo p : getPlayerList()) {
            if(p.getGameProfile().getId().equals(uuid)) {
                return p;
            }
        }
        return null;
    }
    
    public static Method[] getMethods(Class<?> clazz, Class<?>... args) {
        ArrayList<Method> methods = new ArrayList<>();
        
        Method[] declaredMethods = clazz.getDeclaredMethods();
        for (int i = 0 ; i < declaredMethods.length ; i++) {
            Method m = declaredMethods[i];
            if(Arrays.equals(m.getParameterTypes(), args)) {
                methods.add(m);
            }
        }
        
        return methods.toArray(new Method[0]);
    }
    
    public static EntityPlayer[] getAllies() {
        EntityPlayer[] players = TTCp.world.playerEntities.toArray(new EntityPlayer[0]);
        ArrayList<EntityPlayer> allies = new ArrayList<>();
        for (int i = 0; i < players.length; i++) {
            if(
                    players[i].getUniqueID().equals(TTCp.mc.getSession().getProfile().getId()) ||
                    (
                            Team.getInstance().names.contains(players[i].getGameProfile().getName()) ||
                            Friend.getInstance().names.contains(players[i].getGameProfile().getName()) ||
                            AltControl.getInstance().isAlt(players[i])
                    )
            ) {
                allies.add(players[i]);
            }
        }
        return allies.toArray(new EntityPlayer[0]);
    }

    public static int trunc(double d) {
        return (int) (d < 0 ? Math.ceil(d) : Math.floor(d));
    }
    public static Entity getPointingEntity(float reach, float expand) {
        double d2 = reach, d0 = reach, d1 = reach;
        Entity entity = TTCp.mc.getRenderViewEntity();
        Vec3d vec3d = entity.getPositionEyes(1);
        Vec3d vec3d1 = entity.getLook(1.0F);
        Vec3d vec3d2 = vec3d.add(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0);
        Vec3d vec3d3 = null;
        List<Entity> list = TTCp.mc.world.getEntitiesInAABBexcluding(entity, entity.getEntityBoundingBox().expand(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0).grow(1.0D, 1.0D, 1.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, new com.google.common.base.Predicate<Entity>() {
            public boolean apply(@Nullable Entity p_apply_1_)
            {
                return p_apply_1_ != null && p_apply_1_.canBeCollidedWith();
            }
        }));
        Entity pointedEntity = null;

        for (int j = 0; j < list.size(); ++j)
        {
            Entity entity1 = list.get(j);
            AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow((double)entity1.getCollisionBorderSize()).grow(expand);
            RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);

            if (axisalignedbb.contains(vec3d))
            {
                if (d2 >= 0.0D)
                {
                    pointedEntity = entity1;
                    vec3d3 = raytraceresult == null ? vec3d : raytraceresult.hitVec;
                    d2 = 0.0D;
                }
            }
            else if (raytraceresult != null)
            {
                double d3 = vec3d.distanceTo(raytraceresult.hitVec);

                if (d3 < d2 || d2 == 0.0D)
                {
                    if (entity1.getLowestRidingEntity() == entity.getLowestRidingEntity() && !entity1.canRiderInteract())
                    {
                        if (d2 == 0.0D)
                        {
                            pointedEntity = entity1;
                            vec3d3 = raytraceresult.hitVec;
                        }
                    }
                    else
                    {
                        pointedEntity = entity1;
                        vec3d3 = raytraceresult.hitVec;
                        d2 = d3;
                    }
                }
            }
        }
        return pointedEntity;
    }
}
