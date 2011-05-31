package kid;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class Vehicle {
	
	private Id id;
	
	private Map<String, String> vehicleAttributes = new HashMap<String,String>();

	public Vehicle(Id id) {
		super();
		this.id = id;
	}

	public Id getId() {
		return id;
	}

	public Map<String, String> getAttributes() {
		return vehicleAttributes;
	}
	
}
