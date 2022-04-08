package org.matsim.contrib.shared_mobility.routing;

import java.util.Optional;

import org.matsim.facilities.Facility;

public interface InteractionFinder {
	Optional<InteractionPoint> findPickup(Facility originFacility);

	Optional<InteractionPoint> findDropoff(Facility destinationFacility);
}
