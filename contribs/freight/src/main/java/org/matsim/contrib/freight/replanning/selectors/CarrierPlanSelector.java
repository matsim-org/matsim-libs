package org.matsim.contrib.freight.replanning.selectors;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.core.replanning.selectors.GenericPlanSelector;

/**
 * A planSelector is a strategy to retrieve a plan from a carrier's plan memory.
 * 
 * @author sschroeder
 *
 */
public interface CarrierPlanSelector extends GenericPlanSelector<CarrierPlan>{
	
	@Override
	public CarrierPlan selectPlan(HasPlansAndId<CarrierPlan> carrier);

}
