package org.matsim.core.mobsim.qsim.qnetsimengine.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

public interface ParkingSearchTimeFunction {
	double calculateParkingSearchTime(Map<Id<Link>, ParkingCount> parkingCount);
}
