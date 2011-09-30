package playground.mzilske.freight.carrier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;


public class Carriers {

	private Map<Id, Carrier> carriers = new HashMap<Id, Carrier>();
	
	public Carriers(Collection<Carrier> carriers){
		makeMap(carriers);
	}
	
	private void makeMap(Collection<Carrier> carriers) {
		for(Carrier c : carriers){
			this.carriers.put(c.getId(), c);
		}
		
	}

	public Carriers(){
		
	}
	
	public Map<Id, Carrier> getCarriers() {
		return carriers;
	}
	
}
