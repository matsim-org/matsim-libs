package org.matsim.contrib.freight.replanning.selectors;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;

public class SelectBestPlan implements CarrierPlanSelector {
	
	/**
	 * Selects and returns the best plan (with the highest score) of the carrier. 
	 * 
	 * <p>If there is an unscored plan, it is returned.
	 */
	@Override
	public CarrierPlan selectPlan(HasPlansAndId<CarrierPlan> carrier) {
		CarrierPlan best = null;
		for (CarrierPlan p : carrier.getPlans()) {
			if(p.getScore() == null){
				return p;
			}
			if (best == null) {
				best = p;
			} else {
				if (p.getScore() > best.getScore()) {
					best = p;
				}
			}
		}
		return best;
	}


}
