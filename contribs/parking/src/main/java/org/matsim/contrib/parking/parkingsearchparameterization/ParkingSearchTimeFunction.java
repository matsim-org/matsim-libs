package org.matsim.contrib.parking.parkingsearchparameterization;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

public interface ParkingSearchTimeFunction {
	double calculateParkingSearchTime(Map<Id<Link>, ParkingCount> parkingCount);
}
