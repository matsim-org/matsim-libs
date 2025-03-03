package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public class ConstantParkingSearchTime implements ParkingSearchTimeCalculator {
	private final double time;

	public ConstantParkingSearchTime(double time) {
		this.time = time;
	}

	@Override
	public double calculateParkingSearchTime(double now, QVehicle vehicle, Link link) {
		return time;
	}
}
