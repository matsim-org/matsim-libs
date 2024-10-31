package org.matsim.dsim.simulation.net;

public interface SimQueue {

    boolean isEmpty();

    SimVehicle peek();

    SimVehicle poll();

    void addFirst(SimVehicle vehicle);

    void addLast(SimVehicle vehicle);
}
