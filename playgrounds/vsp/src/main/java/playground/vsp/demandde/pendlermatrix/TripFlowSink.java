package playground.vsp.demandde.pendlermatrix;

import org.matsim.facilities.ActivityFacility;

public interface TripFlowSink {
	
	public void process(ActivityFacility quelle, ActivityFacility ziel, int quantity, String mode, String destinationActivityType, double departureTimeOffset);

	public void complete();

}
