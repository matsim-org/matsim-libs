package org.matsim.dsim.simulation.net;

import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;

import java.util.Comparator;
import java.util.PriorityQueue;

class PassingQueue implements SimDequeue {

	private final PriorityQueue<DistributedMobsimVehicle> internalQ = new PriorityQueue<>(
		Comparator.comparingDouble(DistributedMobsimVehicle::getEarliestLinkExitTime)
	);

	@Override
	public boolean isEmpty() {
		return internalQ.isEmpty();
	}

	@Override
	public DistributedMobsimVehicle peek() {
		return internalQ.peek();
	}

	@Override
	public DistributedMobsimVehicle poll() {
		return internalQ.poll();
	}

	@Override
	public void addFirst(DistributedMobsimVehicle vehicle) {

		assert !internalQ.contains(vehicle) : "vehicle already in queue";

		// this will add the vehicle to the head of the queue
		// if the vehicle has the earliest exit time of all
		// vehicles.
		internalQ.add(vehicle);
	}

	@Override
	public void addLast(DistributedMobsimVehicle vehicle) {
		assert !internalQ.contains(vehicle) : "vehicle already in queue";

		internalQ.add(vehicle);
	}
}
