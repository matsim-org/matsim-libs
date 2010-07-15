package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.api.core.v01.Id;

/**
 * data for one person about when he arrived and at what parking.
 * 
 * @author rashid_waraich
 * 
 */
public class ParkingArrivalInfo {

	Id facilityId;
	double arrivalTime;

	public Id getFacilityId() {
		return facilityId;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public ParkingArrivalInfo(Id facilityId, double arrivalTime) {
		super();
		this.facilityId = facilityId;
		this.arrivalTime = arrivalTime;
	}

}
