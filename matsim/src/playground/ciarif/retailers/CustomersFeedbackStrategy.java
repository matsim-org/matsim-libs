package playground.ciarif.retailers;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.basic.v01.BasicLinkImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;

public class CustomersFeedbackStrategy implements RetailerStrategy {
	
	private final static Logger log = Logger.getLogger(MaxLinkRetailerStrategy.class);
	public static final String NAME = "customersFeedbackStrategy";
	private Controler controler;
	private Object[] links;
	// TODO balmermi: do the same speed optimization here

	public CustomersFeedbackStrategy(Controler controler, Object [] links) {
		this.controler = controler;
		this.links = links;
	}
	
	public void moveRetailersFacilities(Map<Id, FacilityRetailersImpl> facilities) {
		
		for (FacilityRetailersImpl f :facilities.values()) {
			int rd = MatsimRandom.getRandom().nextInt(links.length);
			BasicLinkImpl link =(BasicLinkImpl)links[rd];
			Utils.moveFacility(f,link, controler.getWorld());
		}		
	}

	public void moveFacilities(Map<Id, ActivityFacility> facilities) {
		// TODO Auto-generated method stub
		
	}

}
