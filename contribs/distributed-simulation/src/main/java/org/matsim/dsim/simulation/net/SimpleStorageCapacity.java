package org.matsim.dsim.simulation.net;

import lombok.Getter;
import org.matsim.api.core.v01.network.Link;
import org.matsim.dsim.utils.CountedWarning;

@Getter
class SimpleStorageCapacity implements StorageCapacity {

	private final double max;
	private double occupied;

	SimpleStorageCapacity(Link link, double effectiveCellSize) {
		var defaultStorageCapacity = link.getLength() * link.getNumberOfLanes() / effectiveCellSize;
		// We have two lower bounds for storage capacities:
		// 1. We need as much storage capacity as we have flow capacity per time step
		// 2. We need sufficient storage capacity to serve the flow if the link has a large travel time (slow freesepeed)
		// NOTE: I don't understand 2. Why is that capacity not dependent on the cell size?
		var minStorageCapacityForOutFlow = link.getFlowCapacityPerSec();
		var minStorageCapacityForSlowSpeed = link.getLength() / link.getFreespeed() * link.getFlowCapacityPerSec();
		max = Math.max(defaultStorageCapacity, Math.max(minStorageCapacityForOutFlow, minStorageCapacityForSlowSpeed));

		if (defaultStorageCapacity < max) {
			CountedWarning.warn("SimpleStorageCapacity::Init", 10,
				"Storage capacity for link {} is increased to serve the outflow of the link. This changes traffic dynamics", link.getId());
		}
	}

	@Override
	public void consume(double pce) {
		occupied += pce;
	}

	@Override
	public void release(double pce, double now) {
		occupied -= pce;
	}

	@Override
	public void update(double now) {
		// don't do anything
	}

	@Override
	public boolean isAvailable() {
		return occupied < max;
	}

	@Override
	public String toString() {
		return "max=" + max + " occupied=" + occupied;
	}
}
