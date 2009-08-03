package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.core.basic.v01.BasicLinkImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.ciarif.retailers.data.LinkRetailersImpl;

public class CustomersFeedbackStrategy implements RetailerStrategy {
	
	private final static Logger log = Logger.getLogger(MaxLinkRetailerStrategy.class);
	public static final String NAME = "customersFeedbackStrategy";
	private Controler controler;
	private Map<Id,ActivityFacility> movedFacilities = new TreeMap<Id,ActivityFacility>();
	private int incumbentCount = 0;
	private int newCount = 0;
	// TODO balmermi: do the same speed optimization here

	public CustomersFeedbackStrategy(Controler controler) {
		this.controler = controler;
	}
	
	public Map<Id, ActivityFacility> moveFacilities(Map<Id, ActivityFacility> facilities,  ArrayList<LinkRetailersImpl> allowedLinks) {
		
		
			return this.movedFacilities;
	}
}
