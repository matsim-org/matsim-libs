package playground.mzilske.freight;


public interface CarrierAgentFactory {
	
	public CarrierAgent createAgent(CarrierAgentTracker tracker, CarrierImpl carrier);

}
