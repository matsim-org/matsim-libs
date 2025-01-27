package org.matsim.contrib.parking.parkingsearch.facilityBasedParking;

import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;

public class RandomParkingTest extends AbstractParkingTest {
	@Override
	ParkingSearchStrategy getParkingSearchStrategy() {
		return ParkingSearchStrategy.Random;
	}
}
