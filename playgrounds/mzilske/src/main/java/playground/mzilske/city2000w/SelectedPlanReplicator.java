package playground.mzilske.city2000w;

import java.util.Collection;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.Contract;

public class SelectedPlanReplicator {

	public CarrierPlan replan(CarrierCapabilities carrierCapabilities, Collection<Contract> contracts, CarrierPlan selectedPlan) {
		CarrierPlan plan = new CarrierPlan(selectedPlan.getScheduledTours());
		return plan;
	}
	
	
	
}
