package playground.mzilske.city2000w;

import java.util.Collection;

import playground.mzilske.freight.CarrierCapabilities;
import playground.mzilske.freight.CarrierPlan;
import playground.mzilske.freight.CarrierContract;

public class SelectedPlanReplicator {

	public CarrierPlan replan(CarrierCapabilities carrierCapabilities, Collection<CarrierContract> contracts, CarrierPlan selectedPlan) {
		CarrierPlan plan = new CarrierPlan(selectedPlan.getScheduledTours());
		return plan;
	}
	
	
	
}
