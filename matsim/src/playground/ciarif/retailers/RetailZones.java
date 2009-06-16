package playground.ciarif.retailers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;

public class RetailZones {
	private final Map<Id,RetailZone> retailZones = new LinkedHashMap<Id, RetailZone>();
	//private final ArrayList<RetailersAlgorithm> algorithms = new ArrayList<RetailersAlgorithm>();
	
	public final boolean addRetailZone(final RetailZone retailZone) {
		if (retailZone == null) { return false; }
		if (this.retailZones.containsKey(retailZone.getId())) { return false; }
		this.retailZones.put(retailZone.getId(),retailZone);
		return true;
	}
	
	public Map<Id,RetailZone> getRetailZones() {
		return this.retailZones;
	}
}
