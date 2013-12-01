package org.matsim.contrib.freight.carrier;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.GenericStrategyManager;

public interface CarrierPlanStrategyManagerFactory {
	
	/**
	 * This is for the time being very unrestrictive to allow both the passing of the "normal" and the "carrier" strategy manager.  kai, nov'13
	 */
	public GenericStrategyManager<CarrierPlan> createStrategyManager(Controler controler);

}
