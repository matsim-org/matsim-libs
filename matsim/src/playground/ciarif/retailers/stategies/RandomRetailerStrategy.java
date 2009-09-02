package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.network.BasicLinkImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkLayer;
import org.matsim.world.World;

import playground.ciarif.retailers.data.FacilityRetailersImpl;
import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.utils.Utils;

public class RandomRetailerStrategy implements RetailerStrategy {
	
	private final World world;
	public static final String NAME = "randomRetailerStrategy";
	private Map<Id,ActivityFacilityImpl> movedFacilities = new TreeMap<Id,ActivityFacilityImpl>();
	
	public RandomRetailerStrategy (NetworkLayer network, World world) {
		this.world = world;
	}
	
	final public Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> facilities, ArrayList<LinkRetailersImpl> allowedLinks) {
		for (ActivityFacilityImpl f : facilities.values()) {
			int rd = MatsimRandom.getRandom().nextInt(allowedLinks.size());
			BasicLinkImpl link =(BasicLinkImpl)allowedLinks.get(rd);
			Utils.moveFacility(f,link,this.world);
			this.movedFacilities.put(f.getId(),f);
		}
		return this.movedFacilities;
	}

	public void moveRetailersFacilities(
			Map<Id, FacilityRetailersImpl> facilities) {
		// TODO Auto-generated method stub
		
	}

	public ArrayList<LinkRetailersImpl> findAvailableLinks() {
		// TODO Auto-generated method stub
		return null;
	}
}
