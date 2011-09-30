package playground.mzilske.freight.api;

import playground.mzilske.freight.carrier.Carrier;
import playground.mzilske.freight.carrier.CarrierAgent;
import playground.mzilske.freight.carrier.CarrierAgentTracker;


public interface CarrierAgentFactory {
	
	public CarrierAgent createAgent(CarrierAgentTracker tracker, Carrier carrier);

}
