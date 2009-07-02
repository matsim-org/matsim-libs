package playground.ciarif.retailers;

import java.util.LinkedHashMap;
import java.util.Map;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacility;

public class Retailer {
	private final Id id;
	private final Map<Id,ActivityFacility> facilities = new LinkedHashMap<Id,ActivityFacility>();

	private RetailerStrategy strategy;
		
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
	
	public final boolean addStrategy (Controler controler, String strategyName, Object [] links) {
		
		if (strategyName.contains(RandomRetailerStrategy.NAME)) {
			this.strategy = new RandomRetailerStrategy(controler.getNetwork(), links, controler.getWorld());
			return true;
		}
		else if (strategyName.contains(MaxLinkRetailerStrategy.NAME)) {
			this.strategy = new MaxLinkRetailerStrategy (controler, links);
			return true;
		}
		else if (strategyName.contains(LogitMaxLinkRetailerStrategy.NAME)) {
			this.strategy = new LogitMaxLinkRetailerStrategy (controler, links);
			return true;
		}
		else if (strategyName.contains(CatchmentAreaRetailerStrategy.NAME)) {
			this.strategy = new CatchmentAreaRetailerStrategy (controler, links);
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

	public final Map<Id,ActivityFacility> runStrategy() {
		strategy.moveFacilities(this.facilities);
		return this.facilities;
	}
}
