package org.matsim.contrib.drt.stops;

import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * This class provides the stop duration of a request. Eventually, this could
 * also become a simple attribute of a DrtRequest to simplify the code
 * complexity.
 */
public interface PassengerStopDurationProvider {
	double calcPickupDuration(DvrpVehicle vehicle, DrtRequest request);

	double calcDropoffDuration(DvrpVehicle vehicle, DrtRequest request);
}
