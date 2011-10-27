package org.matsim.contrib.freight.api;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierAgent;
import org.matsim.contrib.freight.carrier.CarrierAgentTracker;


public interface CarrierAgentFactory {
	
	public CarrierAgent createAgent(CarrierAgentTracker tracker, Carrier carrier);

}
