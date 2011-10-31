package org.matsim.contrib.freight.mobsim;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.ScheduledTour;


public class CarrierDriverAgentFactoryImpl implements CarrierDriverAgentFactory{

	@Override
	public CarrierDriverAgent createDriverAgent(CarrierAgent carrierAgent,Id driverId, ScheduledTour tour) {
		CarrierDriverAgent driverAgent = new CarrierDriverAgentImpl(carrierAgent, driverId, tour);
		return driverAgent;
	}

}
