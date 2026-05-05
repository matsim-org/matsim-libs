package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;

/**
 * This interface calculates the arrival duration for a vehicle on a given link.
 */
public interface ArrivalTimeCalculator {
	double calculateArrivalTime(double now, QVehicle vehicle, Link link);
}
