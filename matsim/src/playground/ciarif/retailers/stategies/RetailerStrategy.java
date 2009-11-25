package playground.ciarif.retailers.stategies;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.ciarif.retailers.data.LinkRetailersImpl;

public interface RetailerStrategy {

	public Map<Id, ActivityFacility> moveFacilities(Map<Id, ActivityFacility> facilities, Map<Id,LinkRetailersImpl> links);
}
