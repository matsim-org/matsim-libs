package org.matsim.contrib.parking.parkingsearch.facilityBasedParking;

import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;

public class NearestParkingSpotTest extends AbstractParkingTest {
	@Override
	ParkingSearchStrategy getParkingSearchStrategy() {
		return ParkingSearchStrategy.NearestParkingSpot;
	}

	//TODO something is wrong with this strategy. Even for 100 vehicles, all of them are parking in the same parking spot.
}
