package playground.balac.retailers.strategies;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.balac.retailers.data.LinkRetailersImpl;

import java.util.Map;
import java.util.TreeMap;


public class RetailerStrategyImpl
  implements RetailerStrategy
{
	  private static final Logger log = Logger.getLogger(RetailerStrategyImpl.class);
	  protected Map<Id<ActivityFacility>, ActivityFacilityImpl> retailerFacilities;
	  protected Controler controler;
	
	  public RetailerStrategyImpl(Controler controler)
	  {
	    this.controler = controler;
	    log.info("Controler" + this.controler);
	  }
	
	  @Override
		public Map<Id<ActivityFacility>, ActivityFacilityImpl> moveFacilities(Map<Id<ActivityFacility>, ActivityFacilityImpl> facilities, TreeMap<Id<Link>, LinkRetailersImpl> links)
	  {
	    return null;
	  }
	
	  protected TreeMap<Integer, String> createInitialLocationsForGA(TreeMap<Id<Link>, LinkRetailersImpl> availableLinks) {
	    TreeMap<Integer, String> locations = new TreeMap<Integer, String>();
	    int intCount = 0;
	    for (ActivityFacilityImpl af : this.retailerFacilities.values())
	    {
            locations.put(Integer.valueOf(intCount), NetworkUtils.getNearestLink(((NetworkImpl) this.controler.getScenario().getNetwork()), af.getCoord()).getId().toString());
	      ++intCount;
	      log.info("The facility with Id: " + af.getId() + " has been added, this is located on the link: " + af.getLinkId());
	    }
	    for (LinkRetailersImpl l : availableLinks.values()) {
	      if (locations.containsValue(l.getId().toString())) {
	        log.info("The Link: " + l.getId() + " is already on the list");
	      }
	      else {
	        locations.put(Integer.valueOf(intCount), l.getId().toString());
	        ++intCount;
	        log.info("The Link: " + l.getId() + " has been added");
	      }
	    }
	
	    log.info("Initial Locations (with Free Links) = " + locations);
	    return locations;
	  }
	
	  protected TreeMap<Id<Link>, LinkRetailersImpl> mergeLinks(TreeMap<Id<Link>, LinkRetailersImpl> freeLinks, Map<Id<ActivityFacility>, ActivityFacilityImpl> retailerFacilities)
	  {
	    this.retailerFacilities = retailerFacilities;
	    TreeMap<Id<Link>,LinkRetailersImpl> availableLinks = new TreeMap<>();
	    for (ActivityFacilityImpl af : this.retailerFacilities.values()) {
	    	Id<Link> id = af.getLinkId();
            LinkRetailersImpl link = new LinkRetailersImpl(this.controler.getScenario().getNetwork().getLinks().get(id), this.controler.getScenario().getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
	    	availableLinks.put(id, link);
	    }
	    availableLinks.putAll(freeLinks);
	    return availableLinks;
	  }
	


}
