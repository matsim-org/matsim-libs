package playground.mzilske.city2000w;

import java.util.Collection;

import playground.mzilske.freight.carrier.CarrierCapabilities;
import playground.mzilske.freight.carrier.CarrierContract;
import playground.mzilske.freight.carrier.CarrierPlan;

public class SelectedPlanReplicator {

	public CarrierPlan replan(CarrierCapabilities carrierCapabilities, Collection<CarrierContract> contracts, CarrierPlan selectedPlan) {
		CarrierPlan plan = new CarrierPlan(selectedPlan.getScheduledTours());
		return plan;
	}
	
	
	
}
