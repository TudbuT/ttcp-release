package de.tudbut.mod.client.ttcp.ttcic;

import net.minecraft.client.Minecraft;
import de.tudbut.mod.client.ttcp.ttcic.task.DoNothingTask;
import de.tudbut.obj.Transient;
import de.tudbut.parsing.JSON;
import de.tudbut.parsing.TCN;
import de.tudbut.parsing.JSON.JSONFormatException;
import de.tudbut.tools.ConfigSaverTCN2;

public abstract class Task {

    @Transient
    private boolean done = false;

    protected static Minecraft mc = Minecraft.getMinecraft();

    public boolean isDone() {
        return done;
    }

    protected final void done() {
        done = true;
    }

    protected abstract void onTick();

    public abstract void unpauseOrStart();

    public abstract void pauseOrStop();

    public void start() {
        unpauseOrStart();
    }

    public void stop() {
        pauseOrStop();
    }

    public String toString() {
        return JSON.write((TCN) ConfigSaverTCN2.write(this, true, false));
    }

    public static Task fromString(String string) {
        Task t;
        try {
            t = (Task) ConfigSaverTCN2.read(JSON.read(string), null);
        } catch (ClassNotFoundException | JSONFormatException e) {
            e.printStackTrace();
            t = new DoNothingTask();
        }
        return t;
    }
}