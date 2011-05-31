package freight;

import java.util.ArrayList;
import java.util.Collection;

import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportChain.ChainElement;
import playground.mzilske.freight.TransportServiceProviderImpl;

public class TestTSPPlanReader {
	
	public static void main(String[] args) {
		Collection<TransportServiceProviderImpl> tsps = new ArrayList<TransportServiceProviderImpl>();
		TSPPlanReader reader = new TSPPlanReader(tsps);
		reader.read("output/tspPlans.xml");
		for(TransportServiceProviderImpl tsp : tsps){
			System.out.println(tsp.getId());
			for(TSPContract c : tsp.getContracts()){
				System.out.println(c.getShipment());
			}
			for(TransportChain chain : tsp.getSelectedPlan().getChains()){
				for(ChainElement cE : chain.getChainElements()){
					System.out.println(cE);
				}
			}
		}
	}

}
