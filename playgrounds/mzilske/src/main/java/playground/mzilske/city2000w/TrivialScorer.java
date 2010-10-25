package playground.mzilske.city2000w;

import playground.mzilske.freight.CarrierAgent;
import playground.mzilske.freight.CarrierPlan;

public class TrivialScorer {

	public double score(CarrierAgent carrierAgent, CarrierPlan selectedPlan) {
		return carrierAgent.score();
	}
	
}
