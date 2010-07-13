package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;

/**
 * This class maintains the occupancy of a parking and its parking occupancy bins.
 * 
 * @author rashid_waraich
 *
 */
public class OldParkingOccupancyMaintainer {

	HashMap<Id, Integer> currentParkingOccupancy = new HashMap<Id, Integer>();
	HashMap<Id, ParkingOccupancyBins> parkingOccupancyBins = new HashMap<Id, ParkingOccupancyBins>();

	public void increaseParkingOccupancy(Id parkingFacilityId, double time) {
		if (!currentParkingOccupancy.containsKey(parkingFacilityId)) {
			currentParkingOccupancy.put(parkingFacilityId, 0);
		}

		int curParkingOccupancy = currentParkingOccupancy.get(parkingFacilityId);
		curParkingOccupancy++;
		currentParkingOccupancy.put(parkingFacilityId, curParkingOccupancy);

		updateParkingOccupancyBins(parkingFacilityId, time);
	}

	public ParkingOccupancyBins getParkingOccupancyBins(Id parkingFacilityId) {
		if (!parkingOccupancyBins.containsKey(parkingFacilityId)) {
			parkingOccupancyBins.put(parkingFacilityId, new ParkingOccupancyBins());
		}

		return parkingOccupancyBins.get(parkingFacilityId);
	}

	/**
	 * assumption: private method invoked from with class and therefore no empty
	 * check on currentParkingOccupancy performed.
	 * 
	 * @param parkingFacilityId
	 * @param time
	 */
	private void updateParkingOccupancyBins(Id parkingFacilityId, double time) {
		if (!parkingOccupancyBins.containsKey(parkingFacilityId)) {
			parkingOccupancyBins.put(parkingFacilityId, new ParkingOccupancyBins());
		}

		ParkingOccupancyBins pBins = parkingOccupancyBins.get(parkingFacilityId);
		pBins.inrementParkingOccupancy(currentParkingOccupancy.get(parkingFacilityId), time);
	}

	public void decreaseParkingOccupancy(Id parkingFacilityId, double time) {
		// first update the bin, because else the parking facility has changed
		updateParkingOccupancyBins(parkingFacilityId, time);
		
		
		
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
