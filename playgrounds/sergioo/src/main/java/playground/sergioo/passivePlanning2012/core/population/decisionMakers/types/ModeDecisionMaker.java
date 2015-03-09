package playground.sergioo.passivePlanning2012.core.population.decisionMakers.types;

import org.matsim.api.core.v01.Id;
import org.matsim.facilities.ActivityFacility;

public interface ModeDecisionMaker extends DecisionMaker {

	//Methods
	public String decideMode(double time, Id<ActivityFacility> startFacilityId, Id<ActivityFacility> endFacilityId);
	
}
