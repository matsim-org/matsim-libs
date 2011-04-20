package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Id;

public class ActInfo {

	Id facilityId;
	String actType;
	
	public Id getFacilityId() {
		return facilityId;
	}

	public String getActType() {
		return actType;
	}

	public ActInfo(Id facilityId, String actType) {
		super();
		this.facilityId = facilityId;
		this.actType = actType;
	}
	
}
