package playground.ciarif.retailers.stategies;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacility;

import playground.ciarif.retailers.data.LinkRetailersImpl;

public abstract interface RetailerStrategy
{
  public abstract Map<Id<ActivityFacility>, ActivityFacility> moveFacilities(Map<Id<ActivityFacility>, ActivityFacility> facilities, Map<Id<Link>, LinkRetailersImpl> link);
}
