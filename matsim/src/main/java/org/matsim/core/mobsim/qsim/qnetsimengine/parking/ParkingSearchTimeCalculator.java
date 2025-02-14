package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public interface ParkingSearchTimeCalculator {
	double calculateParkingSearchTime(QVehicle vehicle, Link link);
}
