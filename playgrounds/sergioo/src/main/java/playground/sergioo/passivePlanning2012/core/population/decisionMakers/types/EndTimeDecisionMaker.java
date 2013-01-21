package playground.sergioo.passivePlanning2012.core.population.decisionMakers.types;

import org.matsim.core.api.experimental.facilities.ActivityFacility;

public interface EndTimeDecisionMaker extends DecisionMaker {

	//Methods
	public void setStartTime(double startTime);
	public void setMaximumEndTime(double maximumEndTime);
	public void setFacility(ActivityFacility facility);
	public double decideEndTime();
	
}
