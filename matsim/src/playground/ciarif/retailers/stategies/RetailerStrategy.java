package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.facilities.ActivityFacilityImpl;

import playground.ciarif.retailers.data.LinkRetailersImpl;

public interface RetailerStrategy {
	

	public Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> facilities, ArrayList<LinkRetailersImpl> links);
}
