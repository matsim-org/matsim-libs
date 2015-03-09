package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.facilities.ActivityFacility;

import playground.ciarif.retailers.data.LinkRetailersImpl;

public class CustomersFeedbackStrategy implements RetailerStrategy {
	
	private final static Logger log = Logger.getLogger(MaxLinkRetailerStrategy.class);
	public static final String NAME = "customersFeedbackStrategy";
	private Controler controler;
	private Map<Id<ActivityFacility>,ActivityFacility> movedFacilities = new TreeMap<>();
	private int incumbentCount = 0;
	private int newCount = 0;
	// TODO balmermi: do the same speed optimization here

	public CustomersFeedbackStrategy(Controler controler) {
		this.controler = controler;
	}
	
	public Map<Id<ActivityFacility>, ActivityFacility> moveFacilities(Map<Id<ActivityFacility>, ActivityFacility> facilities,  ArrayList<LinkRetailersImpl> allowedLinks) {
		
		
			return this.movedFacilities;
	}
	public ArrayList<LinkRetailersImpl> findAvailableLinks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Id<ActivityFacility>, ActivityFacility> moveFacilities(
			Map<Id<ActivityFacility>, ActivityFacility> facilities,
			Map<Id<Link>, LinkRetailersImpl> link) {
		// TODO Auto-generated method stub
		return null;
	}

}
