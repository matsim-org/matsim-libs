package org.matsim.dsim.simulation.net;

import java.util.ArrayDeque;
import java.util.Deque;

class FIFOQueue implements SimDequeue {

    private final Deque<SimVehicle> internalQ = new ArrayDeque<>();

    @Override
    public boolean isEmpty() {
        return internalQ.isEmpty();
    }

    @Override
    public SimVehicle peek() {
        return internalQ.peek();
    }

    @Override
    public SimVehicle poll() {
        return internalQ.poll();
    }

    @Override
    public void addFirst(SimVehicle vehicle) {
        assert !internalQ.contains(vehicle) : "vehicle already in queue";

        internalQ.addFirst(vehicle);
    }

    @Override
    public void addLast(SimVehicle vehicle) {
        assert !internalQ.contains(vehicle) : "vehicle already in queue";

        internalQ.addLast(vehicle);
    }
}
