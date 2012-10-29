package org.matsim.contrib.freight.carrier;

import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyManager;
import org.matsim.core.controler.Controler;

public interface CarrierPlanStrategyManagerFactory {
	
	public CarrierReplanningStrategyManager createStrategyManager(Controler controler);

}
