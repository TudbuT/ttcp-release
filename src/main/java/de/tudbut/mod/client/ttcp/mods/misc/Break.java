package de.tudbut.mod.client.ttcp.mods.misc;

import io.netty.buffer.Unpooled;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import de.tudbut.mod.client.ttcp.gui.lib.component.Button;
import de.tudbut.mod.client.ttcp.mods.rendering.Notifications;
import de.tudbut.mod.client.ttcp.utils.BlockUtils;
import de.tudbut.mod.client.ttcp.utils.Module;
import de.tudbut.mod.client.ttcp.utils.Setting;
import de.tudbut.mod.client.ttcp.utils.category.Misc;
import de.tudbut.obj.Save;

import java.io.IOException;

@Misc
public class Break extends Module {

    BlockPos start = null;
    BlockPos end = null;

    public boolean doBreak = true;
    @Save
    public boolean rotate = false;

    float altRotX = 0, altRotY = 0;

    long lastPacket = 0;
    
    @Save
    int iterations = 1;
    
    public boolean done = false;

    @Override
    public boolean onPacket(Packet<?> packet) {
        if(rotate) {
            if (packet instanceof CPacketPlayer.Rotation) {
                if (System.currentTimeMillis() - lastPacket < 90)
                    return true;
                lastPacket = System.currentTimeMillis();
                PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
                buffer.writeFloat(altRotX);
                buffer.writeFloat(altRotY);
                buffer.writeByte(((CPacketPlayer.Rotation) packet).isOnGround() ? 1 : 0);
                try {
                    packet.readPacketData(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (packet instanceof CPacketPlayer.PositionRotation) {
                lastPacket = System.currentTimeMillis();
                PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
                buffer.writeDouble(((CPacketPlayer.PositionRotation) packet).getX(0));
                buffer.writeDouble(((CPacketPlayer.PositionRotation) packet).getY(0));
                buffer.writeDouble(((CPacketPlayer.PositionRotation) packet).getZ(0));
                buffer.writeFloat(altRotX);
                buffer.writeFloat(altRotY);
                buffer.writeByte(((CPacketPlayer.PositionRotation) packet).isOnGround() ? 1 : 0);
                try {
                    packet.readPacketData(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return super.onPacket(packet);
    }
    @Override
    public void updateBinds() {
        customKeyBinds.setIfNull("reset", new KeyBind(null, toString() + "::onEnable", false));
        customKeyBinds.setIfNull("pause", new KeyBind(null, toString() + "::togglePause", false));
        subComponents.clear();
        subComponents.add(Setting.createBoolean("Break", this, "doBreak"));
        subComponents.add(Setting.createBoolean("Rotate", this, "rotate"));
        subComponents.add(new Button("Reset", it -> {
            onEnable();
            updateBinds();
        }));
        subComponents.add(Setting.createInt(1, 5, "Iterations", this, "iterations"));
        subComponents.add(Setting.createKey("ResetKey", customKeyBinds.get("reset")));
        subComponents.add(Setting.createKey("PauseKey", customKeyBinds.get("pause")));
    }

    public void togglePause() {
        doBreak = !doBreak;
        if(doBreak)
            Notifications.add(new Notifications.Notification("Unpaused breaking."));
        if(!doBreak)
            Notifications.add(new Notifications.Notification("Paused breaking."));
    }

    @Override
    public void onDisable() {
        if(MidClick.bindBlock != null)
            if(MidClick.bindBlock.getName().startsWith("Break"))
                MidClick.bindBlock = null;
        MidClick.reload();
    }

    @Override
    public void onEnable() {
        start = end = null;
        Notifications.add(new Notifications.Notification("Please select the starting position with MIDCLICK!", 20000));
        MidClick.set(new MidClick.Bind() {
            @Override
            public Type getType() {
                return Type.BLOCK;
            }

            @Override
            public String getName() {
                return "Break START";
            }

            @Override
            public void call(Data data) {
                posCallback(data);
            }
        });
    }

    @Override
    public void onTick() {
        if(doBreak) {
            if(end != null) {
                for (int i = 0 ; i < iterations ; i++) {
                    run();
                }
            } else {
                done = true;
            }
        }
    }
    
    private void run() {
        int px = (int)player.posX, py = (int)player.getPositionEyes(1).y, pz = (int)player.posZ;
        for (int iy = 0; iy <= 10; iy++) {
            for (int iz = 0; iz <= 10; iz++) {
                for (int ix = 0; ix <= 10; ix++) {
                    int x = px + ix - 5, y = py + iy - 5, z = pz + iz - 5;

                    if(x >= start.getX() && y >= start.getY() && z >= start.getZ()) {
                        if(x <= end.getX() && y <= end.getY() && z <= end.getZ()) {
                            if(breakBlockIfPossible(x,y,z)) {
                                done = false;
                                return;
                            }
                        }
                    }
                }
            }
        }
        done = true;
    }

    private boolean breakBlockIfPossible(int x, int y, int z) {
        BlockPos pos = new BlockPos(x,y,z);

        if(player.getPositionEyes(1).distanceTo(new Vec3d(x + 0.5, y, z + 0.5)) > mc.playerController.getBlockReachDistance() - 0.25)
            return false;
        if(!mc.world.getBlockState(pos).getBlock().isReplaceable(mc.world, pos)) {
            float[] floats = BlockUtils.getLegitRotations(new Vec3d(pos).add(0.5, 0.5, 0.5));
            if(altRotX != floats[0] || altRotY != floats[1]) {
                altRotX = floats[0];
                altRotY = floats[1];
                if (rotate && System.currentTimeMillis() - lastPacket < 100)
                    mc.player.connection.sendPacket(new CPacketPlayer.Rotation());
            }
            if(mc.playerController.onPlayerDamageBlock(pos, EnumFacing.DOWN)) {
                mc.effectRenderer.addBlockHitEffects(pos, EnumFacing.DOWN);
                player.swingArm(EnumHand.MAIN_HAND);
            }
            return true;
        }
        return false;
    }

    private void posCallback(MidClick.Bind.Data data) {
        if(start == null) {
            start = data.block();
            MidClick.set(new MidClick.Bind() {
                @Override
                public Type getType() {
                    return Type.BLOCK;
                }

                @Override
                public String getName() {
                    return "Break END";
                }

                @Override
                public void call(Data data) {
                    posCallback(data);
                }
            });
            Notifications.add(new Notifications.Notification("Please select the ending position with MIDCLICK!", 20000));
            return;
        }
        if(end == null) {
            BlockPos endSel = data.block();
            BlockPos startSel = start;
            start = new BlockPos(Math.min(startSel.getX(), endSel.getX()), Math.min(startSel.getY(), endSel.getY()), Math.min(startSel.getZ(), endSel.getZ()));
            end = new BlockPos(Math.max(startSel.getX(), endSel.getX()), Math.max(startSel.getY(), endSel.getY()), Math.max(startSel.getZ(), endSel.getZ()));
            MidClick.bindBlock = null;
            MidClick.reload();
            Notifications.add(new Notifications.Notification("Breaking!", 20000));
        }
    }
}
