package org.matsim.contrib.parking.parkingsearch;

public class RandomParkingTest extends AbstractParkingTest {
	@Override
	ParkingSearchStrategy getParkingSearchStrategy() {
		return ParkingSearchStrategy.Random;
	}
}
