package org.matsim.dsim.simulation.net;

/**
 * Have this interface here, because it is pretty tedious to implement an entire Deque<T> interface for
 * FIFO and Passing queue. This interface only requires a subset of available methods.
 */
interface SimDequeue {

	boolean isEmpty();

	SimVehicle peek();

	SimVehicle poll();

	void addFirst(SimVehicle vehicle);

	void addLast(SimVehicle vehicle);
}
