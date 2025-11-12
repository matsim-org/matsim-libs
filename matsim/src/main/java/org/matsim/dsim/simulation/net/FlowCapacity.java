package org.matsim.dsim.simulation.net;

import org.matsim.api.core.v01.network.Link;
import org.matsim.dsim.DSimConfigGroup;

class FlowCapacity {

	private final double max;

	public double getMax() {
		return max;
	}

	private double lastUpdateTime;
	private double accumulatedCapacity;

	private FlowCapacity(double max) {
		this.max = max;
		this.accumulatedCapacity = max;
		this.lastUpdateTime = 0;
	}

	boolean isAvailable() {
		return accumulatedCapacity > 1e-10; // give some error margin here
	}

	void consume(double pce) {
		accumulatedCapacity -= pce;
	}

	void update(double now) {
		if (lastUpdateTime < now) {
			var timeSteps = now - lastUpdateTime;
			var accFlow = timeSteps * max + accumulatedCapacity;
			accumulatedCapacity = Math.min(accFlow, max);
			lastUpdateTime = now;
		}
	}

	@Override
	public String toString() {
		return "max=" + max + ", acc=" + accumulatedCapacity;
	}

	static FlowCapacity createOutflowCapacity(Link link) {
		return new FlowCapacity(link.getFlowCapacityPerSec());
	}

	static FlowCapacity createInflowCapacity(Link link, DSimConfigGroup config, double effectiveCellSize) {
		return switch (config.getTrafficDynamics()) {
			case queue -> new FlowCapacity(Double.POSITIVE_INFINITY);
			case kinematicWaves -> createInflowCapacityFromFlowDiagram(link, effectiveCellSize);
			case withHoles ->
				throw new RuntimeException("Config:qsim.trafficDynamics = 'withHoles' is not supported. options are 'queue' and 'kinematicWaves'");
		};
	}

	private static FlowCapacity createInflowCapacityFromFlowDiagram(Link link, double effectiveCellSize) {
		var maxInflowCapacity = (link.getNumberOfLanes() / effectiveCellSize) / (1. / KinematicWavesStorageCapacity.HOLE_SPEED + 1 / link.getFreespeed());
		return new FlowCapacity(maxInflowCapacity);
	}
}
