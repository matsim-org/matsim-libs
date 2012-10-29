package org.matsim.contrib.freight.replanning;

import org.matsim.contrib.freight.carrier.CarrierPlan;

public interface CarrierReplanningStrategyModule {

	public void handlePlan(CarrierPlan carrierPlan);

}
