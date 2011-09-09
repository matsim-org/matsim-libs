package playground.mzilske.freight.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;


public class DriverPerformanceEvent extends CarrierEventImpl implements Event{
	
	public Id driverId;
	public Id carrierVehicleId;
	public double distance;
	public double time;
	public double capacityUsage;
	public double performance;
	public double volumes;
	
	public DriverPerformanceEvent(Id driverId, Id carrierId, Id carrierVehicleId) {
		super(carrierId);
		this.driverId = driverId;
		this.carrierVehicleId = carrierVehicleId;
	}

	@Override
	public double getTime() {
		return 0;
	}

	@Override
	public Map<String, String> getAttributes() {
		return null;
	}

}
