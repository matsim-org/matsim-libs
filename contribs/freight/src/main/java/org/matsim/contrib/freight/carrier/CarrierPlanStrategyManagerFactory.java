package org.matsim.contrib.freight.carrier;

import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManager;
import org.matsim.core.controler.Controler;

public interface CarrierPlanStrategyManagerFactory {
	
	public CarrierPlanStrategyManager createStrategyManager(Controler controler);

}
