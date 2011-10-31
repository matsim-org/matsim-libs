package org.matsim.contrib.freight.mobsim;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.ScheduledTour;


public interface CarrierDriverAgentFactory {
	public CarrierDriverAgent createDriverAgent(CarrierAgent carrierAgent, Id driverId, ScheduledTour tour);
}
