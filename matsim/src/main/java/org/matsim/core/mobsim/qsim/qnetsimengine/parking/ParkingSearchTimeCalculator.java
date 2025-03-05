package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

/**
 * This interface calculates the parking search time for a vehicle on a given link.
 */
public interface ParkingSearchTimeCalculator {
	double calculateParkingSearchTime(double now, QVehicle vehicle, Link link);
}
