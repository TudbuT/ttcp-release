package de.tudbut.mod.client.ttcp.ttcic;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import de.tudbut.tools.Tools;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import de.tudbut.io.FileBus;
import de.tudbut.io.TypedInputStream;
import de.tudbut.io.TypedOutputStream;

public class TTCIC extends Thread {

    public static TTCIC ttcic;
    {
        ttcic = this;
    }

    public enum Role {
        MAIN,
        ALT,
        FINDER,
    }

    private static Minecraft mc = Minecraft.getMinecraft();

    public Role role;
    public FileBus bus;
    public int id = -1;
    public boolean alsoRunAlt = false;
    public TaskQueue taskQueue = new TaskQueue();

    private int nextID = 0;
    private UserMap userData = new UserMap();

    public TTCIC(Role role, FileBus bus) {
        this.role = role;
        this.bus = bus;
        if (role == Role.MAIN) {
            id = 0;
        }
    }

    public TTCIC(Role role, FileBus bus, boolean alsoRunAlt) {
        this(role, bus);
        this.alsoRunAlt = alsoRunAlt;
    }

    @Override
    public void run() {
        TypedInputStream input = bus.getTypedReader();
        while (!this.isInterrupted()) {
            try {
                if (id == -1) {
                    write(
                            "Hello, my UUID is {}, and my username is {}",
                            mc.getSession().getProfile().getId(),
                            mc.getSession().getProfile().getName());
                }
                int sender = input.readInt();
                String s = input.readString();
                if (id == -1) {
                    id = Integer.parseInt(
                            Tools.readf(
                                    "Hello, this network is owned by {} (UUID {}), and your ID will be {}", s)[2]);
                }
                Thread.sleep(id);

                run(sender, s);
                switch (role) {
                    case MAIN:
                        if (alsoRunAlt) {
                            runAlt(sender, s);
                        }
                        runMain(sender, s);
                        break;
                    case ALT:
                        runAlt(sender, s);
                        break;
                    case FINDER:
                        // Already done regardless
                        break;
                }
            } catch (IOException e) {
                // ignore
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void runMain(int sender, String s) {
        @SuppressWarnings("unused")
        String data = null;
        String[] mdata = null;

        mdata = Tools.readf("Hello, my UUID is {}, and my username is {}", s);
        if (mdata != null) {
            Account account = new Account(++nextID, UUID.fromString(mdata[0]), mdata[1]);
            write("Hello {} (UUID {}), this network is owned by {} (UUID {}), and your ID will be {}",
                    mdata[1], mdata[0],
                    mc.getSession().getProfile().getName(), mc.getSession().getProfile().getId(),
                    nextID);
            userData.put(account);
            writeAccounts();
            write("Please provide a list of players in your RD.");
        }
    }

    private void runAlt(int sender, String s) {
        String data = null;
        String[] mdata = null;

        mdata = Tools.readf("{}, please start the task {}.", s);
        if (mdata != null) {
            if (id != Integer.parseInt(mdata[0]))
                return;
            Task task = Task.fromString(mdata[1]);
            taskQueue.enqueue(task);
        }

        data = Tools.readf1("Please everyone start the task {}", s);
        if (data != null) {
            Task task = Task.fromString(data);
            taskQueue.enqueue(task);
        }

        data = Tools.readf1("{}, please stop your tasks.", s);
        if (data != null) {
            if (id != Integer.parseInt(data))
                return;
            taskQueue.killCurrent();
        }

        data = Tools.readf1("Please stop your tasks.", s);
        if (data != null) {
            taskQueue.killCurrent();
        }
    }

    private void run(int sender, String s) {
        String data = null;
        String[] mdata = null;

        data = Tools.readf1("Please provide a list of players in your RD.", s);
        if (data != null) {
            writeAccounts();
        }

        mdata = Tools.readf("{} (UUID {}) is at BlockPos({}, {}, {}).", s);
        if (mdata != null) {
            Account account = userData.getOrMake(null, UUID.fromString(mdata[1]), mdata[0]);
            account.location = Optional.of(new Vec3d(
                    Double.parseDouble(mdata[2]),
                    Double.parseDouble(mdata[3]),
                    Double.parseDouble(mdata[4])));
        }

        mdata = Tools.readf("Hello {} (UUID {}), this network is owned by {} (UUID {}), and your ID will be {}", s);
        if (mdata != null) {
            userData.getOrMake(0, UUID.fromString(mdata[3]), mdata[2]);
            userData.getOrMake(Integer.parseInt(mdata[4]), UUID.fromString(mdata[1]), mdata[0]);
        }

        data = Tools.readf1("Quitting.", s);
        if (data != null) {
            userData.removeID(sender);
        }
    }

    public void writeAccounts() {
        for (EntityPlayer player : mc.world.playerEntities) {
            write("{} (UUID {}) is at BlockPos({}, {}, {}).",
                    player.getGameProfile().getName(),
                    player.getGameProfile().getId(),
                    player.posX, player.posY, player.posZ);
            userData.getOrMake(
                    null,
                    player.getGameProfile().getId(),
                    player.getGameProfile().getName()).location = Optional.of(
                            player.getPositionVector());
        }
    }

    private void internalWrite(String s, Object... format) {
        for (Object f : format) {
            s = s.replaceFirst("{}", f.toString());
        }
        TypedOutputStream stream = bus.getTypedWriter();
        try {
            stream.writeInt(id);
            stream.writeString(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(String s, Object... format) {
        try {
            bus.startWrite();
            internalWrite(s, (Object[]) format);
            bus.stopWrite();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Account localize(Account toAttack) {
        return userData.getOrMake(toAttack.id.isPresent() ? toAttack.id.getAsInt() : null,
                toAttack.uuid,
                toAttack.name);
    }

}
