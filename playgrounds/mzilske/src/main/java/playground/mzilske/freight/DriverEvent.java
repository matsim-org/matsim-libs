package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public class DriverEvent implements CarrierEvent{
	
	public Id driverId;
	public Id carrierId;
	public Id carrierVehicleId;
	public double distance;
	public double time;
	public double capacityUsage;
	public double performance;
	public double volumes;
	
	public DriverEvent(Id driverId, Id carrierId, Id carrierVehicleId) {
		super();
		this.driverId = driverId;
		this.carrierId = carrierId;
		this.carrierVehicleId = carrierVehicleId;
	}

}
