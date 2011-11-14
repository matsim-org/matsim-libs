package org.matsim.contrib.freight.replanning;

import org.matsim.contrib.freight.carrier.Carrier;

public interface CarrierPlanStrategyModule {
	
	public void handleActor(Carrier carrier);

}
