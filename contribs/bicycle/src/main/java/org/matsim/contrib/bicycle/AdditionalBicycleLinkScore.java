package org.matsim.contrib.bicycle;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public interface AdditionalBicycleLinkScore{
	double computeLinkBasedScore(Link link, Id<Vehicle> vehicleId, String bicycleMode );
}
