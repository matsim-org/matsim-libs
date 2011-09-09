package playground.mzilske.freight.api;

import playground.mzilske.freight.Carrier;
import playground.mzilske.freight.CarrierAgent;
import playground.mzilske.freight.CarrierAgentTracker;


public interface CarrierAgentFactory {
	
	public CarrierAgent createAgent(CarrierAgentTracker tracker, Carrier carrier);

}
