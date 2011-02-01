package playground.mzilske.freight;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class Carriers {

	private Map<Id, CarrierImpl> carriers = new HashMap<Id, CarrierImpl>();
	
	public Map<Id, CarrierImpl> getCarriers() {
		return carriers;
	}
	
}
