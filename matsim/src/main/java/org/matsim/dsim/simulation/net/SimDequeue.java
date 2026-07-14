package org.matsim.dsim.simulation.net;

import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;

/**
 * Have this interface here, because it is pretty tedious to implement an entire Deque<T> interface for
 * FIFO and Passing queue. This interface only requires a subset of available methods.
 */
interface SimDequeue {

	boolean isEmpty();

	DistributedMobsimVehicle peek();

	DistributedMobsimVehicle poll();

	void addFirst(DistributedMobsimVehicle vehicle);

	void addLast(DistributedMobsimVehicle vehicle);
}
