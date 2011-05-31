package kid;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

public class ScheduledVehicles {
	
	private Map<Id,ScheduledVehicle> scheduledVehicles = new HashMap<Id, ScheduledVehicle>();

	public Map<Id, ScheduledVehicle> getScheduledVehicles() {
		return scheduledVehicles;
	}
	
}
