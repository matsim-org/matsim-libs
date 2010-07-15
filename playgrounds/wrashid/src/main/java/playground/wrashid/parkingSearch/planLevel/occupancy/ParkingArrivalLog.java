package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;

/**
 * The order of the parkings is important. First parking is the one after the home parking.
 * The last parking is the parking at home.
 * @author rashid_waraich
 *
 */
public class ParkingArrivalLog {

	ArrayList<ParkingArrivalInfo> list = new ArrayList<ParkingArrivalInfo>();

	public void addParkingArrivalInfo(Id facilityId, double arrivalTime) {
		list.add(new ParkingArrivalInfo(facilityId, arrivalTime));
	}

	public ArrayList<ParkingArrivalInfo> getParkingArrivalInfoList() {
		return list;
	}

}
