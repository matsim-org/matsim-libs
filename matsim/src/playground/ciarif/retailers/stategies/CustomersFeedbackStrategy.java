package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.network.BasicLinkImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
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
	private Map<Id,ActivityFacilityImpl> movedFacilities = new TreeMap<Id,ActivityFacilityImpl>();
	private int incumbentCount = 0;
	private int newCount = 0;
	// TODO balmermi: do the same speed optimization here

	public CustomersFeedbackStrategy(Controler controler) {
		this.controler = controler;
	}
	
	public Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> facilities,  ArrayList<LinkRetailersImpl> allowedLinks) {
		
		
			return this.movedFacilities;
	}
	public ArrayList<LinkRetailersImpl> findAvailableLinks() {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<Id, ActivityFacilityImpl> moveFacilities(
			Map<Id, ActivityFacilityImpl> facilities,
			TreeMap<Id, LinkRetailersImpl> links) {
		// TODO Auto-generated method stub
		return null;
	}
}
