package org.matsim.contrib.parking.parkingsearch.facilityBasedParking;

import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;

public class DistanceMemoryParkingTest extends AbstractParkingTest {
	@Override
	ParkingSearchStrategy getParkingSearchStrategy() {
		return ParkingSearchStrategy.DistanceMemory;
	}
}
