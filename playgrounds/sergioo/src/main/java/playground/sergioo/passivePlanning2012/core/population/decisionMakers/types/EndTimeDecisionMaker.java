package playground.sergioo.passivePlanning2012.core.population.decisionMakers.types;

import org.matsim.api.core.v01.Id;

public interface EndTimeDecisionMaker extends DecisionMaker {

	//Methods
	public double decideEndTime(double startTime, double maximumEndTime, String typeOfActivity, Id facilityId);
	
}
