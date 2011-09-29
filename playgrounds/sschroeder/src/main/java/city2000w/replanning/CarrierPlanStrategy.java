package city2000w.replanning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import playground.mzilske.freight.Carrier;
import playground.mzilske.freight.Contract;
import playground.mzilske.freight.ScheduledTour;
import playground.mzilske.freight.Shipment;

public class CarrierPlanStrategy implements PlanStrategy<Carrier>{
	
	private Logger logger = Logger.getLogger(CarrierPlanStrategy.class);
	
	private List<CarrierPlanStrategyModule> strategyModules = new ArrayList<CarrierPlanStrategyModule>();

	public void addModule(CarrierPlanStrategyModule module){
		strategyModules.add(module);
	}

	public void run(Carrier carrier){
		for(CarrierPlanStrategyModule module : strategyModules){
			logger.info("run " + module.getClass().toString());
			module.handleActor(carrier);
		}
		assertSelectedCarrierPlanIsConsistenWithContracts(carrier);
	}
	
	private void assertSelectedCarrierPlanIsConsistenWithContracts(Carrier carrier) {
		if(carrier.getSelectedPlan() == null){
			return;
		}
		Set<Shipment> contractedCarrierShipments = new HashSet<Shipment>();
		for(Contract c : carrier.getContracts()){
			contractedCarrierShipments.add(c.getShipment());
		}
		Set<Shipment> shipmentsInPlan = new HashSet<Shipment>();
		for(ScheduledTour t : carrier.getSelectedPlan().getScheduledTours()){
			shipmentsInPlan.addAll(t.getTour().getShipments());
		}
		for(Shipment cF : contractedCarrierShipments){
			if(!shipmentsInPlan.contains(cF)){
				throw new IllegalStateException("tspShipment in contracts not in plan");
			}
		}
		for(Shipment cF : shipmentsInPlan){
			if(!contractedCarrierShipments.contains(cF)){
				throw new IllegalStateException("tspShipment in plan not in contracts");
			}
		}
		
	}
}
