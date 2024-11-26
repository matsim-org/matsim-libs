package org.matsim.contrib.parking.parkingsearch;

public class NearestParkingSpotTest extends AbstractParkingTest {
	@Override
	ParkingSearchStrategy getParkingSearchStrategy() {
		return ParkingSearchStrategy.NearestParkingSpot;
	}

	//TODO something is wrong with this strategy. Even for 100 vehicles, all of them are parking in the same parking spot.
}
