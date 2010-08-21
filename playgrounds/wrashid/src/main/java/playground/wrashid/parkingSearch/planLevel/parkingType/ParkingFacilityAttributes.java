package playground.wrashid.parkingSearch.planLevel.parkingType;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;

public interface ParkingFacilityAttributes {

	public LinkedList<ParkingAttribute> getParkingFacilityAttributes(Id facilityId);

}
