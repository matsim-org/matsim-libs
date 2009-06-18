package playground.ciarif.retailers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;

public class RetailZones {
	private final static Logger log = Logger.getLogger(MaxLinkRetailerStrategy.class);
	
	private final Map<Id,RetailZone> retailZones = new LinkedHashMap<Id, RetailZone>();
	
	public final boolean addRetailZone(final RetailZone retailZone) {
		if (retailZone == null) { return false; }
		if (this.retailZones.containsKey(retailZone.getId())) { return false; }
		this.retailZones.put(retailZone.getId(),retailZone);
		log.info("The zone " + retailZone.getId() + " has been added");
		return true;
	}
	
	public Map<Id,RetailZone> getRetailZones() {
		return this.retailZones;
	}
}
