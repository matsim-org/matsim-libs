package playground.ciarif.retailers.data;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class Retailers {
	
	private final Map<Id,Retailer> retailers = new LinkedHashMap<Id, Retailer>();
	//private final ArrayList<RetailersAlgorithm> algorithms = new ArrayList<RetailersAlgorithm>();
	
	public final boolean addRetailer(final Retailer retailer) {
		if (retailer == null) { return false; }
		if (this.retailers.containsKey(retailer.getId())) { return false; }
		this.retailers.put(retailer.getId(),retailer);
		return true;
	}
	
	public Map<Id,Retailer> getRetailers() {
		return this.retailers;
	}

	protected Retailer getLastRetailer(){
		int i = (this.retailers.values().size()-1);
		if (i==0){return null;} //the container is empty
		else {
			return this.retailers.get(i);// THIS WILL NOT WORK! mrieser, 25jan2010
			// retailers is a Map. a Map can only be queried with get(key), not get(index)
			// because a map is not sorted, so the "last" element is not specified
			// what the code above actually does, is it looks for a retailer in the Map
			// with the id = new Integer(i), which will always return null!
		}
	}
}
