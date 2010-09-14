package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;

public class ParkingCapacity {

	/*
	 * id: facilityId value: capacity of the facility
	 */
	private HashMap<Id, Integer> facilityCapacity = new HashMap<Id, Integer>();
	private ArrayList<ActivityFacilityImpl> parkingFacilities=new ArrayList<ActivityFacilityImpl>();

	public ArrayList<ActivityFacilityImpl> getParkingFacilities() {
		return parkingFacilities;
	}

	public ParkingCapacity(ActivityFacilitiesImpl facilities) {

		for (ActivityFacility facility : facilities.getFacilities().values()) {

			for (ActivityOption activityOption : facility.getActivityOptions().values()) {

				if (activityOption.getType().equalsIgnoreCase("parking")) {
					facilityCapacity.put(facility.getId(), (int) Math.round(Math.floor(activityOption.getCapacity())));
					parkingFacilities.add((ActivityFacilityImpl) facility);
				}

			}

		}

	}

	public int getParkingCapacity(Id facilityId) {
		return facilityCapacity.get(facilityId);
	}
	
	public int getNumberOfParkings(){
		return facilityCapacity.size();
	}
	
	 

}
