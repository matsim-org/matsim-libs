package org.matsim.contrib.parking.parkingsearch;

public class DistanceMemoryParkingTest extends AbstractParkingTest {
	@Override
	ParkingSearchStrategy getParkingSearchStrategy() {
		return ParkingSearchStrategy.DistanceMemory;
	}
}
