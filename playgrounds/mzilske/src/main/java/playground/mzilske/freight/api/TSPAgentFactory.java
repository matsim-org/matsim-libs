package playground.mzilske.freight.api;

import playground.mzilske.freight.TSPAgent;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TransportServiceProviderImpl;

public interface TSPAgentFactory {
	public TSPAgent createTspAgent(TSPAgentTracker tspAgentTracker, TransportServiceProviderImpl tsp);
}
