package org.matsim.contrib.freight.carrier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;

/**
 * A container that maps carriers.
 * 
 * @author sschroeder
 *
 */
public class Carriers {

	private static Logger log = Logger.getLogger(Carriers.class);
	
	private Map<Id<Carrier>, Carrier> carriers = new HashMap<>();

	public Carriers(Collection<Carrier> carriers) {
		makeMap(carriers);
	}

	private void makeMap(Collection<Carrier> carriers) {
		for (Carrier c : carriers) {
			this.carriers.put(c.getId(), c);
		}
	}

	public Carriers() {

	}

	public Map<Id<Carrier>, Carrier> getCarriers() {
		return carriers;
	}

	public void addCarrier(Carrier carrier) {
		if(!carriers.containsKey(carrier.getId())){
			carriers.put(carrier.getId(), carrier);
		}
		else log.warn("carrier " + carrier.getId() + " already exists");
	}

}
