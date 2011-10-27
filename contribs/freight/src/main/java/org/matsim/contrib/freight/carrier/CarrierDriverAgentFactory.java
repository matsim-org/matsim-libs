package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;


public interface CarrierDriverAgentFactory {
	public CarrierDriverAgent createDriverAgent(CarrierAgent carrierAgent, Id driverId, ScheduledTour tour);
}
