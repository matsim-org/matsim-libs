package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.BasicLinkImpl;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.world.World;

public class RandomRetailerStrategy implements RetailerStrategy {
	
	private final ArrayList<LinkRetailersImpl> links;
	private final World world;
	public static final String NAME = "randomRetailerStrategy";
	public RandomRetailerStrategy (NetworkLayer network, ArrayList<LinkRetailersImpl> arrayList, World world) {
		this.links = arrayList;
		this.world = world;
	}
	
	final public void moveFacilities(Map<Id, ActivityFacility> facilities) {
		for (ActivityFacility f : facilities.values()) {
			int rd = MatsimRandom.getRandom().nextInt(links.size());
			BasicLinkImpl link =(BasicLinkImpl)links.get(rd);
			Utils.moveFacility(f,link,this.world);
		}
	}

	public void moveRetailersFacilities(
			Map<Id, FacilityRetailersImpl> facilities) {
		// TODO Auto-generated method stub
		
	}
}
