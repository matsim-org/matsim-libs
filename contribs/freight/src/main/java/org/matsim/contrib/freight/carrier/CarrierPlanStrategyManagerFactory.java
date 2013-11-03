package org.matsim.contrib.freight.carrier;

import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyManagerI;
import org.matsim.core.controler.Controler;

public interface CarrierPlanStrategyManagerFactory {
	
	public CarrierReplanningStrategyManagerI createStrategyManager(Controler controler);

}
