package org.matsim.contrib.freight.replanning.modules;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyModule;

public class SelectBestPlan implements CarrierPlanStrategyModule {

	@Override
	public void handleCarrier(Carrier carrier) {
		CarrierPlan best = null;
		for (CarrierPlan p : carrier.getPlans()) {
			if (best == null) {
				best = p;
			} else {
				if (p.getScore() > best.getScore()) {
					best = p;
				}
			}
		}
		if (best != null) {
			carrier.setSelectedPlan(best);
		}
	}

}
