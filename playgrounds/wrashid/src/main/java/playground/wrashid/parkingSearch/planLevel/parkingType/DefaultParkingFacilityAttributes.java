package playground.wrashid.parkingSearch.planLevel.parkingType;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;

public class DefaultParkingFacilityAttributes implements ParkingFacilityAttributes {

	public LinkedList<ParkingAttribute> getParkingFacilityAttributes(Id facilityId) {
		return new LinkedList<ParkingAttribute>();
	}

}
