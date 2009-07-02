package playground.ciarif.retailers;

import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.BasicLinkImpl;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.world.World;

public class RandomRetailerStrategy implements RetailerStrategy {
	
	private final Object[] links;
	private final World world;
	public static final String NAME = "randomRetailerStrategy";
	public RandomRetailerStrategy (NetworkLayer network, Object[] links, World world) {
		this.links = links;
		this.world = world;
	}
	
	final public void moveFacilities(Map<Id, ActivityFacility> facilities) {
		for (ActivityFacility f : facilities.values()) {
			int rd = MatsimRandom.getRandom().nextInt(links.length);
			BasicLinkImpl link =(BasicLinkImpl)links[rd];
			Utils.moveFacility(f,link,this.world);
		}
	}

	public void moveRetailersFacilities(
			Map<Id, FacilityRetailersImpl> facilities) {
		// TODO Auto-generated method stub
		
	}
}
