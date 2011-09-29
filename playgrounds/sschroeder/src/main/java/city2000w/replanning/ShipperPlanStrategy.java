package city2000w.replanning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import freight.CommodityFlow;
import freight.ScheduledCommodityFlow;
import freight.ShipperContract;
import freight.ShipperImpl;

public class ShipperPlanStrategy implements PlanStrategy<ShipperImpl>{

	private List<ShipperPlanStrategyModule> strategyModules = new ArrayList<ShipperPlanStrategyModule>();
	
	public void addModule(ShipperPlanStrategyModule module){
		strategyModules.add(module);
	}
	
	public void run(ShipperImpl shipper){
		for(ShipperPlanStrategyModule module : strategyModules){
			module.handleActor(shipper);
		}
		assertSelectedPlanIsConsistentWithContract(shipper);
	}
	
	private void assertSelectedPlanIsConsistentWithContract(ShipperImpl shipper) {
		Set<CommodityFlow> contractedComFlows = new HashSet<CommodityFlow>();
		for(ShipperContract c : shipper.getContracts()){
			contractedComFlows.add(c.getCommodityFlow());
		}
		Set<CommodityFlow> comFlowsInPlan = new HashSet<CommodityFlow>();
		for(ScheduledCommodityFlow s : shipper.getSelectedPlan().getScheduledFlows()){
			comFlowsInPlan.add(s.getCommodityFlow());
		}
		for(CommodityFlow cF : contractedComFlows){
			if(!comFlowsInPlan.contains(cF)){
				throw new IllegalStateException("comflow in contracts not in plan");
			}
		}
		for(CommodityFlow cF : comFlowsInPlan){
			if(!contractedComFlows.contains(cF)){
				throw new IllegalStateException("comflow in plan not in contracts");
			}
		}
	}

}
