package playground.ciarif.retailers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.controler.Controler;
import org.matsim.facilities.Facility;
import org.matsim.gbl.MatsimRandom;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;

public class Retailer {
	private final Id id;
	private final Map<Id,Facility> facilities = new LinkedHashMap<Id,Facility>();
	private final RetailerStrategy strategy;
		
	protected Retailer(final Id id, RetailerStrategy rs) { 
		//Try to avoid to pass the network here
		this.id = id;
		// TODO balmermi: implement different strategies and instantiate them here
		this.strategy = rs;//new RetailerStrategy (); // implementation of the strategy
		//Try to avoid a hard coded strategy type here but let it come from the config file 
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
