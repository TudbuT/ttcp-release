package de.tudbut.mod.client.ttcp.utils;

import de.tudbut.debug.DebugProfiler;

public class DebugProfilerAdapter extends DebugProfiler {
    public boolean fallthrough = false;

    public DebugProfilerAdapter(String name, String startingSection) {
        super(name, startingSection);
    }

    @Override
    public synchronized DebugProfiler next(String next) {
        if(fallthrough)
            return this;

        return super.next(next);
    }

    @Override
    public synchronized DebugProfiler endAll() {
        return super.endAll();
    }

    @Override
    public synchronized Results getResults() {
        return super.getResults();
    }

    @Override
    public synchronized Results getTempResults() {
        return super.getTempResults();
    }

    @Override
    public void optimize() {
        super.optimize();
    }

    @Override
    public boolean isLocked() {
        return super.isLocked();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public synchronized void delete() {
        super.delete();
    }

    @Override
    public void finalize() {
        super.finalize();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
