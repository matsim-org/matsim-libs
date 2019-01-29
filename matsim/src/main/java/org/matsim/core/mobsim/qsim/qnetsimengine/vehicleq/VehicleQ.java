package org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq;

import java.util.Queue;



public interface VehicleQ<E> extends Queue<E> {

	// For transit, which inserts its vehicles "in front of" the queue.
	void addFirst(E previous);

}
