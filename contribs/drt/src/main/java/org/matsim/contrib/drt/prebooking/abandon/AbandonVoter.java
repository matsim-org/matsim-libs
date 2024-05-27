package org.matsim.contrib.drt.prebooking.abandon;

import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public interface AbandonVoter {
	boolean abandonRequest(double time, DvrpVehicle vehicle, AcceptedDrtRequest request);
}
