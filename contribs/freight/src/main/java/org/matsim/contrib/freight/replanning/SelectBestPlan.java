package org.matsim.contrib.freight.replanning;

import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;

public class SelectBestPlan implements CarrierPlanStrategyModule{

	@Override
	public void handleActor(Carrier carrier) {
		CarrierPlan best = null;
		for(CarrierPlan p : carrier.getPlans()){
			if(best == null){
				best = p;
			}
			else{
				if(p.getScore()>best.getScore()){
					best=p;
				}
			}
		}
		if(best != null){
			carrier.setSelectedPlan(best);
		}
	}

}
