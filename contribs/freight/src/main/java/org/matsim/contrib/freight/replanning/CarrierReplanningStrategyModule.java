package org.matsim.contrib.freight.replanning;

import org.matsim.contrib.freight.carrier.CarrierPlan;

/**
 * This is a module to modify a carrier's plan.
 * 
 * <p>A strategy consists of a set of modules. Each module can modify a plan specifically. The entire set of modules defines 
 * the carriers strategy.
 * 
 * @author sschroeder
 *
 */
public interface CarrierReplanningStrategyModule {

	/**
	 * Modifies the carrierPlan.
	 * 
	 * @param carrierPlan to be modified.
	 */
	public void handlePlan(CarrierPlan carrierPlan);

}
