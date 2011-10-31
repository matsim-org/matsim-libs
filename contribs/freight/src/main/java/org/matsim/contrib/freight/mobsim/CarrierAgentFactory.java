package org.matsim.contrib.freight.mobsim;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.mobsim.CarrierAgent;
import org.matsim.contrib.freight.mobsim.CarrierAgentTracker;


public interface CarrierAgentFactory {
	
	public CarrierAgent createAgent(CarrierAgentTracker tracker, Carrier carrier);

}
