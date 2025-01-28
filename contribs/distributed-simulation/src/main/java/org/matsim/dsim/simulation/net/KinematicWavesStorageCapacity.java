package org.matsim.dsim.simulation.net;

import lombok.Getter;
import org.matsim.api.core.v01.network.Link;
import org.matsim.utils.CountedWarning;

import java.util.ArrayDeque;
import java.util.Queue;

class KinematicWavesStorageCapacity implements StorageCapacity {

	final static double HOLE_SPEED = 15.0 / 3.6;

	static KinematicWavesStorageCapacity create(Link link, double effectiveCellSize) {

		var minCapacityForHoles = calculateMinCapacityForHoles(link);
		var simpleStorageCapacity = SimpleStorageCapacity.calculateSimpleStorageCapacity(link, effectiveCellSize);
		var assignedCapacity = Math.max(simpleStorageCapacity, minCapacityForHoles);
		var holeTravelTime = link.getLength() / HOLE_SPEED;

		if (simpleStorageCapacity < assignedCapacity) {
			CountedWarning.warn("KinematicWavesStorageCapacity::init", 10,
				"Storage capacity of link {} increased for backwards travelling holes. This changes the traffic dynamics", link.getId()
			);
		}

		return new KinematicWavesStorageCapacity(assignedCapacity, holeTravelTime);
	}

	static double calculateMinCapacityForHoles(Link link) {
		return link.getLength() * link.getFlowCapacityPerSec() * (link.getFreespeed() + HOLE_SPEED) / link.getFreespeed() / HOLE_SPEED;
	}

	@Getter
	private final double max;
	private final double holeTravelTime;
	private final Queue<Hole> holes = new ArrayDeque<>();

	private double lastUpdateTime = 0;
	private double occupiedByVehicles;
	private double occupiedByHoles;

	KinematicWavesStorageCapacity(double capacity, double holeTravelTime) {
		this.max = capacity;
		this.holeTravelTime = holeTravelTime;
	}

	/*KinematicWavesStorageCapacity(Link link, double effectiveCellSize) {

		// In addition to the minimal storage capacity requirements to serve slow speeds and the outflow of the link,
		// we also need sufficient storage capacity to simulate holes and kinematic waves.
		var simpleStorageCapacity = new SimpleStorageCapacity(link, effectiveCellSize);
		var minStorageForHoles = link.getLength() * link.getFlowCapacityPerSec() * (link.getFreespeed() + HOLE_SPEED) / link.getFreespeed() / HOLE_SPEED;
		max = Math.max(simpleStorageCapacity.getMax(), minStorageForHoles);
		holeTravelTime = link.getLength() / HOLE_SPEED;
	}

	 */


	@Override
	public double getOccupied() {
		return occupiedByHoles + occupiedByVehicles;
	}

	@Override
	public void consume(double pce) {
		occupiedByVehicles += pce;
	}

	@Override
	public void release(double pce, double now) {
		occupiedByVehicles = Math.max(0, occupiedByVehicles - pce);
		var earliestExitTime = now + holeTravelTime;
		holes.add(new Hole(earliestExitTime, pce));
		occupiedByHoles += pce;
	}

	@Override
	public void update(double now) {

		if (lastUpdateTime >= now) return;

		lastUpdateTime = now;
		while (!holes.isEmpty() && holes.peek().exitTime() <= now) {
			lastUpdateTime = now;
			var hole = holes.poll();
			occupiedByHoles -= hole.pce();
		}
	}

	@Override
	public boolean isAvailable() {
		return occupiedByVehicles + occupiedByHoles < max;
	}

	private record Hole(double exitTime, double pce) {
	}

	@Override
	public String toString() {
		return "max=" + max + ", pceVeh=" + occupiedByVehicles + ", pceHoles=" + occupiedByHoles;
	}
}
