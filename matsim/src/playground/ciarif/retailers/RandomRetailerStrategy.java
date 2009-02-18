package playground.ciarif.retailers;

import java.util.Map;

import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.geometry.Coord;

public class RandomRetailerStrategy implements RetailerStrategy {
	private NetworkLayer network;
	
	public RandomRetailerStrategy (NetworkLayer network) {
		this.network = network;
	}
	// strategy: Random Mutation
	final public void moveFacilities(Map<Id, Facility> facilities) {
		for (Facility f : facilities.values()) {
			Object[] links = network.getLinks().values().toArray();
			int rd = MatsimRandom.random.nextInt(links.length);
			Link link = (Link)links[rd];
			Coord coord = link.getCenter();
			f.moveTo(coord);
		}
	}
}
