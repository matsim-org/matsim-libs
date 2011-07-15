package playground.mzilske.freight;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class Carriers {

	private Map<Id, CarrierImpl> carriers = new HashMap<Id, CarrierImpl>();
	
	public Carriers(Collection<CarrierImpl> carriers){
		makeMap(carriers);
	}
	
	private void makeMap(Collection<CarrierImpl> carriers) {
		for(CarrierImpl c : carriers){
			this.carriers.put(c.getId(), c);
		}
		
	}

	public Carriers(){
		
	}
	
	public Map<Id, CarrierImpl> getCarriers() {
		return carriers;
	}
	
}
