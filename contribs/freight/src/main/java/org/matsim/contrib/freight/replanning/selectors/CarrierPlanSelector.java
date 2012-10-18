package org.matsim.contrib.freight.replanning.selectors;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;

public interface CarrierPlanSelector {
	
	public CarrierPlan selectPlan(Carrier carrier);

}
