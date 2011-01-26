package playground.mzilske.city2000w;

import java.util.Collection;

import playground.mzilske.freight.TSPCapabilities;
import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPPlan;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChainBuilder;
import playground.mzilske.freight.TransportChain.ChainElement;

public class TSPPlanReplicator {
	
	public TSPPlan buildPlan(TSPCapabilities tspCapabilities, Collection<TSPContract> contracts, TSPPlan templatePlan) {
		Collection<TransportChain> chains = templatePlan.getChains();
		for (TransportChain chain : chains) {
			TransportChainBuilder builder = new TransportChainBuilder(chain.getShipment());
			for (ChainElement chainElement : chain.getChainElements()) {
				builder.schedule(chainElement);
			}
			TransportChain newChain = builder.build();
			chains.add(newChain);
		}
		TSPPlan plan = new TSPPlan(chains);
		return plan;
	}

}
