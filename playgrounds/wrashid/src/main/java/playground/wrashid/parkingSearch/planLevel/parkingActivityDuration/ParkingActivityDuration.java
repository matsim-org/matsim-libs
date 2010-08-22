package playground.wrashid.parkingSearch.planLevel.parkingActivityDuration;

import org.matsim.api.core.v01.Id;

public class ParkingActivityDuration {

	/**
	 * - e.g. different facilities might have different access times than others.
	 * - different persons might get different parking duration than others (e.g. a handi-caped person
	 * might get better service at one place than a different one).
	 * 
	 * @param parkingFacility
	 * @param personId
	 * @return
	 */
	public double getActivityDuration(Id parkingFacilityId, Id personId){
		return 60;
	}
	
}
