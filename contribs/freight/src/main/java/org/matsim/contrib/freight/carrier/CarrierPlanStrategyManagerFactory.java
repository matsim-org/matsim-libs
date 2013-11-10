package org.matsim.contrib.freight.carrier;

import org.matsim.core.api.internal.MatsimManager;
import org.matsim.core.controler.Controler;

public interface CarrierPlanStrategyManagerFactory {
	
	/**
	 * This is for the time being very unrestrictive to allow both the passing of the "normal" and the "carrier" strategy manager.  kai, nov'13
	 */
	public MatsimManager createStrategyManager(Controler controler);

}
