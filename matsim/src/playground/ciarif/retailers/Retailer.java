package playground.ciarif.retailers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;
import org.matsim.core.controler.Controler;

public class Retailer {
	private final Id id;
	private final Map<Id,Facility> facilities = new LinkedHashMap<Id,Facility>();
	private RetailerStrategy strategy;
		
	protected Retailer(final Id id, RetailerStrategy rs) { 
		this.id = id;
		this.strategy = rs;
	}

	public final Id getId() {
		return this.id;
	}

	public final boolean addFacility(Facility f) {
		if (f == null) { return false; }
		if (this.facilities.containsKey(f.getId())) { return false; }
		this.facilities.put(f.getId(),f);
		return true;
	}
	
	public final boolean addStrategy (Controler controler, String strategyName, Object [] links) {
		
		if (strategyName.contains(RandomRetailerStrategy.NAME)) {
			this.strategy = new RandomRetailerStrategy(controler.getNetwork(), links);
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
	
	public final Facility getFacility(final Id facId) {
		return this.facilities.get(facId);
	}

	public final Map<Id,Facility> getFacilities() {
		return this.facilities;
	}

	public final Map<Id,Facility> runStrategy() {
		strategy.moveFacilities(this.facilities);
		return this.facilities;
	}
	

}
