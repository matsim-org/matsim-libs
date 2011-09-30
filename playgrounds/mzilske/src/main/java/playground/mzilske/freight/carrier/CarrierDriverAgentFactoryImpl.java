package playground.mzilske.freight.carrier;

import org.matsim.api.core.v01.Id;


public class CarrierDriverAgentFactoryImpl implements CarrierDriverAgentFactory{

	@Override
	public CarrierDriverAgent createDriverAgent(CarrierAgent carrierAgent,Id driverId, ScheduledTour tour) {
		CarrierDriverAgent driverAgent = new CarrierDriverAgentImpl(carrierAgent, driverId, tour);
		return driverAgent;
	}

}
