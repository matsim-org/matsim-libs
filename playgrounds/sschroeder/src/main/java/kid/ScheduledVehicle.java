package kid;

import java.util.List;

public class ScheduledVehicle {
	
	private Vehicle vehicle;
	
	private List<ScheduledTransportChain> scheduledTransportChains;

	public ScheduledVehicle(Vehicle vehicle,
			List<ScheduledTransportChain> scheduledTransportChains) {
		super();
		this.vehicle = vehicle;
		this.scheduledTransportChains = scheduledTransportChains;
	}

	public Vehicle getVehicle() {
		return vehicle;
	}

	public List<ScheduledTransportChain> getScheduledTransportChains() {
		return scheduledTransportChains;
	}
	
	

}
