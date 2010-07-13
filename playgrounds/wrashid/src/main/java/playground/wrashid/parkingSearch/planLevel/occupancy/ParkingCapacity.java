package playground.wrashid.parkingSearch.planLevel.occupancy;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;

public class ParkingCapacity {

	/*
	 * id: facilityId value: capacity of the facility
	 */
	private HashMap<Id, Integer> facilityCapacity = new HashMap<Id, Integer>();

	public ParkingCapacity(ActivityFacilitiesImpl facilities) {

		for (ActivityFacilityImpl facility : facilities.getFacilities().values()) {

			for (ActivityOptionImpl activityOption : facility.getActivityOptions().values()) {

				if (activityOption.getType().equalsIgnoreCase("parking")) {
					facilityCapacity.put(facility.getId(), (int) Math.round(Math.floor(activityOption.getCapacity())));
				}

			}

		}

	}

	public int getParkingCapacity(Id facilityId) {
		return facilityCapacity.get(facilityId);
	}

}
