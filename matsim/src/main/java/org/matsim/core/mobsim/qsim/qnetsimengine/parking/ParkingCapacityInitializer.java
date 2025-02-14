package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

public interface ParkingCapacityInitializer {
	String LINK_ON_STREET_SPOTS = "onstreet_spots";
	String LINK_OFF_STREET_SPOTS = "offstreet_spots";

	Map<Id<Link>, ParkingInitialCapacity> initialize();

	record ParkingInitialCapacity(int capacity, int initial) {
	}
}
