package playground.balac.retailers.strategies;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.balac.retailers.data.LinkRetailersImpl;


public abstract interface RetailerStrategy
{
  public abstract Map<Id<ActivityFacility>, ActivityFacilityImpl> moveFacilities(Map<Id<ActivityFacility>, ActivityFacilityImpl> paramMap, TreeMap<Id<Link>, LinkRetailersImpl> paramTreeMap);
}
