package playground.wrashid.parkingSearch.planLevel.parkingType;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

public interface ParkingFacilityAttributes {

	public LinkedList<ParkingAttribute> getParkingFacilityAttributes(Id<ActivityFacility> facilityId);

}
