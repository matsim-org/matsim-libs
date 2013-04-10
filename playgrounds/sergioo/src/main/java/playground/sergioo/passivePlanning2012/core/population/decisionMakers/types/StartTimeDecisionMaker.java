package playground.sergioo.passivePlanning2012.core.population.decisionMakers.types;

import org.matsim.api.core.v01.Id;

public interface StartTimeDecisionMaker extends DecisionMaker {

	//Methods
	public double decideStartTime(double minimumStartTime, Id facilityId);
	
}
