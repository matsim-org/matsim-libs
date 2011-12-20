package org.matsim.contrib.freight.mobsim;

import org.matsim.contrib.freight.carrier.Carrier;


public interface CarrierAgentFactory {
	
	public CarrierAgent createAgent(CarrierAgentTracker tracker, Carrier carrier);

}
