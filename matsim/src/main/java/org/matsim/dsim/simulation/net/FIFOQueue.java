package org.matsim.dsim.simulation.net;

import org.matsim.core.mobsim.dsim.DistributedMobsimVehicle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;

class FIFOQueue implements SimDequeue {

	private final Deque<DistributedMobsimVehicle> internalQ = new ArrayDeque<>();

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

		internalQ.addFirst(vehicle);
	}

	@Override
	public void addLast(DistributedMobsimVehicle vehicle) {
		assert !internalQ.contains(vehicle) : "vehicle already in queue";

		internalQ.addLast(vehicle);
	}

	@Override
	public String toString() {
		return internalQ.stream().map(veh -> veh.getId().toString()).collect(Collectors.joining(", "));
	}
}
