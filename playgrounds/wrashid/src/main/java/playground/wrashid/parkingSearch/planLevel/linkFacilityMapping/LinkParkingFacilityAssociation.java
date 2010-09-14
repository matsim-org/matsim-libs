package playground.wrashid.parkingSearch.planLevel.linkFacilityMapping;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.tools.kml.Color;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;

public class LinkParkingFacilityAssociation extends LinkFacilityAssociation {

	public LinkParkingFacilityAssociation(ActivityFacilitiesImpl facilities, NetworkImpl network) {
		this.network=network;
		
		for (ActivityFacility facility: facilities.getFacilities().values()){
			addFacilityToHashMap((ActivityFacilityImpl) facility);
		}		
	}
	
	private void addFacilityToHashMap(ActivityFacilityImpl facility) {
		Id facilityLink=getClosestLink(facility);
		
		assureHashMapInitializedForLink(facilityLink);
		
		ArrayList<ActivityFacilityImpl> list=linkFacilityMapping.get(facilityLink);
		
		if (facility.getActivityOptions().containsKey("parking")){
			if (list.contains(facility)){
				throw new Error("!!!!!!!!!!!!!!");
			}
			
			list.add(facility);
			
			
			
			
			// DEBUG INFO: display parking facilities
			if (GeneralLib.getDistance(new CoordImpl(4528426.090831845,5822407.437950259), network.getLinks().get(facilityLink).getCoord())<5000){
				ParkingRoot.getMapDebugTrace().addPointCoordinate(network.getLinks().get(facilityLink).getCoord(), facilityLink.toString(), Color.GREEN);
			}
		}
	}

	
}
