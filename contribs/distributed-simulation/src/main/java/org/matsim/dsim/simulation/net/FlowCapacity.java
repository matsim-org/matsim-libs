package org.matsim.dsim.simulation.net;

import lombok.Getter;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.groups.QSimConfigGroup;

class FlowCapacity {

	@Getter
	private final double max;

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

	static FlowCapacity createOutflowCapacity(Link link) {
		return new FlowCapacity(link.getFlowCapacityPerSec());
	}

	static FlowCapacity createInflowCapacity(Link link, QSimConfigGroup config, double effectiveCellSize) {
		return switch (config.getTrafficDynamics()) {
			case queue -> new FlowCapacity(Double.POSITIVE_INFINITY);
			case kinematicWaves -> createInflowCapacityFromFlowDiagram(link, config, effectiveCellSize);
			case withHoles ->
				throw new RuntimeException("Config:qsim.trafficDynamics = 'withHoles' is not supported. options are 'queue' and 'kinematicWaves'");
		};
	}

	private static FlowCapacity createInflowCapacityFromFlowDiagram(Link link, QSimConfigGroup config, double effectiveCellSize) {
		if (config.getInflowCapacitySetting() != QSimConfigGroup.InflowCapacitySetting.INFLOW_FROM_FDIAG) {
			throw new RuntimeException("Only INFLOW_FROM_FDiag is supported for Config:qsim.inflowCapacitySettings");
		}
		var maxInflowCapacity = (link.getNumberOfLanes() / effectiveCellSize) / (1. / KinematicWavesStorageCapacity.HOLE_SPEED + 1 / link.getFreespeed());
		return new FlowCapacity(maxInflowCapacity);
	}
}
