package de.tudbut.mod.client.ttcp.ttcic;

import de.tudbut.mod.client.ttcp.ttcic.task.DoNothingTask;
import de.tudbut.tools.Queue;

public class TaskQueue {
    public Queue<Task> nextTasks = new Queue<>();
    public Task currentTask = new DoNothingTask();

    public void enqueue(Task task) {
        nextTasks.add(task);
    }

    public void killCurrent() {
        currentTask.done();
        takeNewTask(null);
    }

    public void startNow(Task task) {
        if (!takeNewTask(task)) {
            currentTask.pauseOrStop();
            nextTasks.pushBottom(currentTask);
            currentTask = task;
            currentTask.start();
        }
    }

    public void onTick() {
        if (!nextTasks.hasNext() && currentTask instanceof DoNothingTask) // Optimization
            return;

        if (currentTask.isDone())
            takeNewTask(null);

        currentTask.onTick();
    }

    private boolean takeNewTask(Task fallback) {
        if (currentTask.isDone()) {
            currentTask.stop();
            if (nextTasks.hasNext()) {
                if (fallback != null)
                    currentTask = fallback;
                else
                    currentTask = nextTasks.next();
            } else {
                currentTask = new DoNothingTask();
            }
            currentTask.start();
            return true;
        }
        return false;
    }
}