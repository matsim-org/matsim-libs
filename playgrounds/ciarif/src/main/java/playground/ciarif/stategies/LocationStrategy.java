package playground.ciarif.stategies;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.facilities.ActivityFacilityImpl;

import playground.ciarif.data.FacilitiesPortfolio;
import playground.ciarif.flexibletransports.router.CarSharingStations;

/** This should become the mother class also for retailer strategies and for all
 * kind of planning strategies intended to optimize the locations of a portfolio
 * of facilities
 * 
 * @author ciarif
 *
 */

public interface LocationStrategy {
	
		public abstract void findOptimalLocations(FacilitiesPortfolio facilitiesPortfolio, TreeMap<Id, LinkImpl> links);
		
}

