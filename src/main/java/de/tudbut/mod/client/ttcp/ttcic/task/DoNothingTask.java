package de.tudbut.mod.client.ttcp.ttcic.task;

import de.tudbut.mod.client.ttcp.ttcic.Task;

public class DoNothingTask extends Task {

    @Override
    protected void onTick() {
    }

    @Override
    public void unpauseOrStart() {
        done();
    }

    @Override
    public void pauseOrStop() {
    }
}
