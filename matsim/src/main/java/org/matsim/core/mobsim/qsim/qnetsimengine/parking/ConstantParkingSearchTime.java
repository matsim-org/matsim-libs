package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public class ConstantParkingSearchTime implements ParkingSearchTimeFunction {
	private final int time;

	public ConstantParkingSearchTime(int time) {
		this.time = time;
	}

	@Override
	public int calculateParkingSearchTime(QVehicle vehicle, Id<Link> linkId) {
		return time;
	}
}
