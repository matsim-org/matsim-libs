package playground.sergioo.passivePlanning.core.population.decisionMakers.types;

import org.matsim.core.api.experimental.facilities.ActivityFacility;

public interface StartTimeDecisionMaker extends DecisionMaker {

	//Methods
	public void setMinimumStartTime(double minimumStartTime);
	public void setFacility(ActivityFacility facility);
	public double decideStartTime();
	
}
