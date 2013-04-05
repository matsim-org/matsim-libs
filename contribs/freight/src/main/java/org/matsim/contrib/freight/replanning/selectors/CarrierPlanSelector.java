package org.matsim.contrib.freight.replanning.selectors;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;

/**
 * A planSelector is a strategy to retrieve a plan from a carrier's plan memory.
 * 
 * @author sschroeder
 *
 */
public interface CarrierPlanSelector {
	
	public CarrierPlan selectPlan(Carrier carrier);

}
