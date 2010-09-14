package playground.ciarif.retailers.data;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;

import playground.ciarif.retailers.stategies.CatchmentAreaRetailerStrategy;
import playground.ciarif.retailers.stategies.CustomersFeedbackStrategy;
import playground.ciarif.retailers.stategies.GravityModelRetailerStrategy;
import playground.ciarif.retailers.stategies.LogitMaxLinkRetailerStrategy;
import playground.ciarif.retailers.stategies.MaxLinkRetailerStrategy;
import playground.ciarif.retailers.stategies.RandomRetailerStrategy;
import playground.ciarif.retailers.stategies.RetailerStrategy;

public class Retailer {
	private final Id id;
	private final Map<Id,ActivityFacilityImpl> facilities = new TreeMap<Id,ActivityFacilityImpl>();
	private final static Logger log = Logger.getLogger(Retailer.class);
	private RetailerStrategy strategy;
	private Map<Id,ActivityFacilityImpl> movedFacilities = new TreeMap<Id,ActivityFacilityImpl>();
		
	public Retailer(final Id id, RetailerStrategy rs) { 
		this.id = id;
		this.strategy = rs;
	}

	public final Id getId() {
		return this.id;
	}

	public final boolean addFacility(ActivityFacilityImpl f) {
		if (f == null) { return false; }
		if (this.facilities.containsKey(f.getId())) { return false; }
		this.facilities.put(f.getId(),(ActivityFacilityImpl)f);
		
		return true;
	}
	
	public final boolean addStrategy (Controler controler, String strategyName) {
		
		if (strategyName.contains(RandomRetailerStrategy.NAME)) {
			this.strategy = new RandomRetailerStrategy(null);
			return true;
		}
		else if (strategyName.contains(MaxLinkRetailerStrategy.NAME)) {
			this.strategy = new MaxLinkRetailerStrategy (controler);
			return true;
		}
		else if (strategyName.contains(LogitMaxLinkRetailerStrategy.NAME)) {
			this.strategy = new LogitMaxLinkRetailerStrategy (controler);
			return true;
		}
		else if (strategyName.contains(CatchmentAreaRetailerStrategy.NAME)) {
			this.strategy = new CatchmentAreaRetailerStrategy (controler);
			return true;
		}
		if (strategyName.contains(CustomersFeedbackStrategy.NAME)) {
			this.strategy = new CustomersFeedbackStrategy(controler);
			return true;
		}
		if (strategyName.contains(GravityModelRetailerStrategy.NAME)) {
			this.strategy = new GravityModelRetailerStrategy(controler);
			return true;
		}
		else { throw new RuntimeException("The strategy has been not added!"); }
	}
	
	public final ActivityFacilityImpl getFacility(final Id facId) {
		return this.facilities.get(facId);
	}

	public final Map<Id,ActivityFacilityImpl> getFacilities() {
		return this.facilities;
	}

	public final void runStrategy(TreeMap<Id,LinkRetailersImpl> links) {
		this.movedFacilities = strategy.moveFacilities(this.facilities, links);
	}
	
	public Map<Id,ActivityFacilityImpl> getMovedFacilities () {
		return this.movedFacilities;
	}
}
