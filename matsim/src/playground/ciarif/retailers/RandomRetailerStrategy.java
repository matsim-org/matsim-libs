package playground.ciarif.retailers;

import java.util.Map;
import org.matsim.basic.v01.BasicLinkImpl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.network.NetworkLayer;

public class RandomRetailerStrategy implements RetailerStrategy {
	
	private final Object[] links;
	public static final String NAME = "randomRetailerStrategy";
	public RandomRetailerStrategy (NetworkLayer network, Object[] links) {
		this.links = links;
	}
	
	final public void moveFacilities(Map<Id, Facility> facilities) {
		for (Facility f : facilities.values()) {
			int rd = MatsimRandom.random.nextInt(links.length);
			BasicLinkImpl link =(BasicLinkImpl)links[rd];
			Utils.moveFacility(f,link);
		}
	}
}
