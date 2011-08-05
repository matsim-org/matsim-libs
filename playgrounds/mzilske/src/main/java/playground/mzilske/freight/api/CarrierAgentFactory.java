package playground.mzilske.freight.api;

import playground.mzilske.freight.CarrierAgent;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierImpl;


public interface CarrierAgentFactory {
	
	public CarrierAgent createAgent(CarrierAgentTracker tracker, CarrierImpl carrier);

}
