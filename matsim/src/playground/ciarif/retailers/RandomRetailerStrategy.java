package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

public class RandomRetailerStrategy implements RetailerStrategy {
	private final ArrayList<Link> links;
	
	public RandomRetailerStrategy (NetworkLayer network) {
		links = new ArrayList<Link>(network.getLinks().values());
	}
	
	final public void moveFacilities(Map<Id, Facility> facilities) {
		for (Facility f : facilities.values()) {
			int rd = MatsimRandom.random.nextInt(links.size());
			Link link = (Link)links.get(rd);
			Utils.moveFacility(f,link);
		}
	}
}
