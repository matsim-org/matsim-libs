package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

public interface ParkingCapacityInitializer {
	Map<Id<Link>, ParkingInitialCapacity> initialize();

	record ParkingInitialCapacity(int capacity, int occupancy) {
	}
}
