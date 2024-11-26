package org.matsim.contrib.parking.parkingsearch;

public class BenensonParkingTest extends AbstractParkingTest {
	@Override
	ParkingSearchStrategy getParkingSearchStrategy() {
		return ParkingSearchStrategy.Benenson;
	}
}
