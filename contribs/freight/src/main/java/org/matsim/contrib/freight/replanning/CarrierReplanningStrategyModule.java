package org.matsim.contrib.freight.replanning;

import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;

/**
 * This is a module to modify a carrier's plan.
 * 
 * <p>A strategy consists of a set of modules. Each module can modify a plan specifically. The entire set of modules defines 
 * the carriers strategy.
 * 
 * @author sschroeder
 *
 */
public interface CarrierReplanningStrategyModule extends GenericPlanStrategyModule<CarrierPlan>{

	/**
	 * Modifies the carrierPlan.
	 * 
	 * @param carrierPlan to be modified.
	 */
	@Override
	public void handlePlan(CarrierPlan carrierPlan);

}
