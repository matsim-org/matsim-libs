package playground.wrashid.parkingSearch.planLevel.linkFacilityMapping;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkLayer;

public class LinkParkingFacilityAssociation extends LinkFacilityAssociation {

	public LinkParkingFacilityAssociation(ActivityFacilitiesImpl facilities, NetworkLayer network) {
		this.network=network;
		
		for (ActivityFacilityImpl facility: facilities.getFacilities().values()){
			addFacilityToHashMap(facility);
		}		
	}
	
	private void addFacilityToHashMap(ActivityFacilityImpl facility) {
		Id facilityLink=getClosestLink(facility);
		
		assureHashMapInitializedForLink(facilityLink);
		
		ArrayList<ActivityFacilityImpl> list=linkFacilityMapping.get(facilityLink);
		
		if (facility.getActivityOptions().containsKey("parking")){
			list.add(facility);
		}
	}

	
}
