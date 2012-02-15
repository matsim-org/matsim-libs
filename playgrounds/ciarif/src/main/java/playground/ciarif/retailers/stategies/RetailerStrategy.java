package playground.ciarif.retailers.stategies;

import java.util.Map;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilityImpl;
import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.stategies.LocationStrategy;

public abstract interface RetailerStrategy
{
  public abstract Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> paramMap, TreeMap<Id, LinkRetailersImpl> paramTreeMap);
}
