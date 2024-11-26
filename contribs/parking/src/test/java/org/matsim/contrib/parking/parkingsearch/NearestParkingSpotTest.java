package org.matsim.contrib.parking.parkingsearch;

public class NearestParkingSpotTest extends AbstractParkingTest {
	@Override
	ParkingSearchStrategy getParkingSearchStrategy() {
		return ParkingSearchStrategy.NearestParkingSpot;
	}
}
