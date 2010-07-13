package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

public class ParkingOccupancyMaintainer {

	HashMap<Id, Integer> currentParkingOccupancy = new HashMap<Id, Integer>();

	public void increaseParkingOccupancy(Id parkingFacilityId, double time) {
		if (!currentParkingOccupancy.containsKey(parkingFacilityId)) {
			currentParkingOccupancy.put(parkingFacilityId, 0);
		}

		int curParkingOccupancy = currentParkingOccupancy.get(parkingFacilityId);
		curParkingOccupancy++;
		currentParkingOccupancy.put(parkingFacilityId, curParkingOccupancy);
	}

	public void decreaseParkingOccupancy(Id parkingFacilityId, double time) {
		if (!currentParkingOccupancy.containsKey(parkingFacilityId)) {
			throw new Error("No car was ever added to the parking!");
		}

		int curParkingOccupancy = currentParkingOccupancy.get(parkingFacilityId);

		if (curParkingOccupancy == 0) {
			throw new Error("Parking occupancy cannot be negative!");
		}

		curParkingOccupancy--;
		currentParkingOccupancy.put(parkingFacilityId, curParkingOccupancy);
	}

	public int getParkingOccupancy(Id parkingFacilityId) {
		if (!currentParkingOccupancy.containsKey(parkingFacilityId)) {
			return 0;
		} else {
			return currentParkingOccupancy.get(parkingFacilityId);
		}
	}

}
