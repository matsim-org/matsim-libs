package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;

/**
 * Simple arrival-time calculator that applies the same additional delay to all vehicles.
 */
public class ConstantArrivalTime implements ArrivalTimeCalculator {
	private final double time;

	public ConstantArrivalTime(double time) {
		this.time = time;
	}

	@Override
	public double calculateArrivalTime(double now, QVehicle vehicle, Link link) {
		return time;
	}
}
