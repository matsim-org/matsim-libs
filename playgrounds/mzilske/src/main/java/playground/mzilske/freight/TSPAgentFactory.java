package playground.mzilske.freight;

public interface TSPAgentFactory {
	public TSPAgent createTspAgent(TSPAgentTracker tspAgentTracker, TransportServiceProviderImpl tsp);
}
