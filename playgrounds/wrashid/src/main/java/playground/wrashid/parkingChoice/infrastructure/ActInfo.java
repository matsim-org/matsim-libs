package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

public class ActInfo {

	Id<ActivityFacility> facilityId;
	String actType;
	
	public Id<ActivityFacility> getFacilityId() {
		return facilityId;
	}

	public String getActType() {
		return actType;
	}

	public ActInfo(Id<ActivityFacility> facilityId, String actType) {
		super();
		this.facilityId = facilityId;
		this.actType = actType;
	}
	
}