package org.matsim.contrib.freight.controler;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.core.replanning.GenericStrategyManager;

public interface CarrierPlanStrategyManagerFactory {

	public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager();

}
