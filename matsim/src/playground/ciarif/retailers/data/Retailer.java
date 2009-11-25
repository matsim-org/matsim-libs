package playground.ciarif.retailers.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;

import playground.ciarif.retailers.stategies.CatchmentAreaRetailerStrategy;
import playground.ciarif.retailers.stategies.CustomersFeedbackStrategy;
import playground.ciarif.retailers.stategies.GravityModelRetailerStrategy;
import playground.ciarif.retailers.stategies.LogitMaxLinkRetailerStrategy;
import playground.ciarif.retailers.stategies.MaxLinkRetailerStrategy;
import playground.ciarif.retailers.stategies.RandomRetailerStrategy;
import playground.ciarif.retailers.stategies.RetailerStrategy;

public class Retailer {
	private final Id id;
	private final Map<Id,ActivityFacility> facilities = new LinkedHashMap<Id,ActivityFacility>();
	//private final static Logger log = Logger.getLogger(Retailer.class);
	private RetailerStrategy strategy;
	private Map<Id,ActivityFacility> movedFacilities = new TreeMap<Id,ActivityFacility>();
		
	public Retailer(final Id id, RetailerStrategy rs) { 
		this.id = id;
		this.strategy = rs;
	}

	public final Id getId() {
		return this.id;
	}

	public final boolean addFacility(ActivityFacility f) {
		if (f == null) { return false; }
		if (this.facilities.containsKey(f.getId())) { return false; }
		this.facilities.put(f.getId(),f);
		return true;
	}
	
	public final boolean addStrategy (Controler controler, String strategyName) {
		
		if (strategyName.contains(RandomRetailerStrategy.NAME)) {
			this.strategy = new RandomRetailerStrategy(controler.getNetwork(), controler.getWorld());
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
	
	public final ActivityFacility getFacility(final Id facId) {
		return this.facilities.get(facId);
	}

	public final Map<Id,ActivityFacility> getFacilities() {
		return this.facilities;
	}

	public final void runStrategy(Map<Id,LinkRetailersImpl> links) {
		this.movedFacilities = strategy.moveFacilities(this.facilities, links);
	}
	
	public Map<Id,ActivityFacility> getMovedFacilities () {
		return this.movedFacilities;
	}
}
