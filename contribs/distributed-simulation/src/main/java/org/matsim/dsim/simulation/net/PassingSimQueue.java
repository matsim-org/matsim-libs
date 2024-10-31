package org.matsim.dsim.simulation.net;

import java.util.Comparator;
import java.util.PriorityQueue;

public class PassingSimQueue implements SimQueue {

    private final PriorityQueue<SimVehicle> internalQ = new PriorityQueue<>(
            Comparator.comparingDouble(SimVehicle::getEarliestExitTime)
    );

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

        if (!isEmpty() && poll().getEarliestExitTime() < vehicle.getEarliestExitTime()) {
            throw new IllegalArgumentException("A vehicle's exit time must be set to the next time step if it should be added to the end of a link, when a passingQ is used.");
        }

        assert !internalQ.contains(vehicle) : "vehicle already in queue";

        // this will add the vehicle to the head of the queue
        // if the vehicle has the earliest exit time of all
        // vehicles.
        internalQ.add(vehicle);
    }

    @Override
    public void addLast(SimVehicle vehicle) {
        assert !internalQ.contains(vehicle) : "vehicle already in queue";

        internalQ.add(vehicle);
    }
}
