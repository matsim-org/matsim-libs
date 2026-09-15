package org.matsim.dsim.simulation.net;

import org.matsim.api.core.v01.network.Link;
import org.matsim.dsim.utils.CountedWarning;

class SimpleStorageCapacity implements StorageCapacity {

	static double calculateDefaultCapacity(Link link, double effectiveCellSize) {
		return link.getLength() * link.getNumberOfLanes() / effectiveCellSize;
	}

	static double calculateMinCapacityForSlowSpeed(Link link) {
		return link.getLength() / link.getFreespeed() * link.getFlowCapacityPerSec();
	}

	static double calculateSimpleStorageCapacity(Link link, double effectiveCellSize) {
		var defaultCapacity = calculateDefaultCapacity(link, effectiveCellSize);
		var minCapacityForOutFlow = link.getFlowCapacityPerSec();
		var minCapacityForSlowspeed = calculateMinCapacityForSlowSpeed(link);
		return Math.max(defaultCapacity, Math.max(minCapacityForOutFlow, minCapacityForSlowspeed));
	}

	static SimpleStorageCapacity create(Link link, double effectiveCellSize) {
		var defaultCapacity = calculateDefaultCapacity(link, effectiveCellSize);
		var max = calculateSimpleStorageCapacity(link, effectiveCellSize);

		if (defaultCapacity < max) {
			CountedWarning.warn("SimpleStorageCapacity::Init", 10,
				"Storage capacity for link {} is increased to serve the outflow of the link. This changes traffic dynamics", link.getId());
		}

		return new SimpleStorageCapacity(max);
	}

	private final double max;

	@Override
	public double getMax() {
		return max;
	}
	
	private double occupied;

	@Override
	public double getOccupied() {
		return occupied;
	}

	SimpleStorageCapacity(double maxCapacity) {
		this.max = maxCapacity;
		this.occupied = 0;
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
