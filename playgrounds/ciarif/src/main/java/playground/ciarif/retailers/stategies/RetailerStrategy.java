package playground.ciarif.retailers.stategies;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilityImpl;

import playground.ciarif.retailers.data.LinkRetailersImpl;

public interface RetailerStrategy {

	public Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> facilities, TreeMap<Id,LinkRetailersImpl> links);

	
}
