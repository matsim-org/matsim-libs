package playground.mzilske.freight.api;

import playground.mzilske.freight.TSPAgent;
import playground.mzilske.freight.TSPAgentTracker;
import playground.mzilske.freight.TransportServiceProvider;

public interface TSPAgentFactory {
	public TSPAgent createTspAgent(TSPAgentTracker tspAgentTracker, TransportServiceProvider tsp);
}
