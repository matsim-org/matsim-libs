package city2000w.replanning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import playground.mzilske.freight.TSPContract;
import playground.mzilske.freight.TSPShipment;
import playground.mzilske.freight.TransportChain;
import playground.mzilske.freight.TransportServiceProvider;

public class TSPPlanStrategy implements PlanStrategy<TransportServiceProvider>{
	
	private List<TSPPlanStrategyModule> strategyModules = new ArrayList<TSPPlanStrategyModule>();
	
	public void addModule(TSPPlanStrategyModule module){
		strategyModules.add(module);
	}
	
	public void run(TransportServiceProvider tsp){
		for(TSPPlanStrategyModule module : strategyModules){
			module.handleActor(tsp);
		}
		assertSelectedTSPPlanIsConsistentWithContracts(tsp);
	}
	
	private void assertSelectedTSPPlanIsConsistentWithContracts(TransportServiceProvider tsp) {
		Set<TSPShipment> contractedTSPShipments = new HashSet<TSPShipment>();
		for(TSPContract c : tsp.getContracts()){
			contractedTSPShipments.add(c.getShipment());
		}
		Set<TSPShipment> shipmentsInPlan = new HashSet<TSPShipment>();
		for(TransportChain t : tsp.getSelectedPlan().getChains()){
			shipmentsInPlan.add(t.getShipment());
		}
		for(TSPShipment cF : contractedTSPShipments){
			if(!shipmentsInPlan.contains(cF)){
				throw new IllegalStateException("tspShipment in contracts, but not in plan. " + cF);
			}
		}
		for(TSPShipment cF : shipmentsInPlan){
			if(!contractedTSPShipments.contains(cF)){
				throw new IllegalStateException("tspShipment in plan, but not in contracts" + cF);
			}
		}
	}

}
