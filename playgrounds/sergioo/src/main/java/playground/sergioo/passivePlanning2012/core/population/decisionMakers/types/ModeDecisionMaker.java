package playground.sergioo.passivePlanning2012.core.population.decisionMakers.types;

import org.matsim.api.core.v01.Id;

public interface ModeDecisionMaker extends DecisionMaker {

	//Methods
	public String decideMode(double time, Id startFacilityId, Id endFacilityId);
	
}
