package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;

import playground.wrashid.parkingSearch.planLevel.scoring.ParkingTimeInfo;

/**
 * TODO: class is similar to ParkingCapacityFullLogger => refactor!!
 * 
 * The order of the parkings is important. First parking is the one after the
 * home parking. The last parking is the parking at home.
 * 
 * @author rashid_waraich
 * 
 */
public class ParkingArrivalDepartureLog {

	ArrayList<ParkingTimeInfo> list;

	public ParkingArrivalDepartureLog() {
		list = new ArrayList<ParkingTimeInfo>();
	}

	public void logParkingArrivalDepartureTime(Id facilityId, double arrivalTime, double departureTime) {
		list.add(new ParkingTimeInfo(arrivalTime, departureTime, facilityId));
	}

	public ArrayList<ParkingTimeInfo> getParkingArrivalDepartureList() {
		return list;
	}

}
