package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.matsim.api.core.v01.network.Link;

/**
 * Calculates the additional time a vehicle needs before it can finish arriving on a link.
 */
public interface ArrivalTimeCalculator {
	double calculateArrivalTime(double now, QVehicle vehicle, Link link);
}
