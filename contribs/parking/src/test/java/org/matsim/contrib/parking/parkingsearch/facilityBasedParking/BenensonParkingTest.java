package org.matsim.contrib.parking.parkingsearch.facilityBasedParking;

import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;

public class BenensonParkingTest extends AbstractParkingTest {
	@Override
	ParkingSearchStrategy getParkingSearchStrategy() {
		return ParkingSearchStrategy.Benenson;
	}
}
