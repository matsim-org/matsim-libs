package playground.sergioo.passivePlanning2012.core.mobsim.passivePlanning.definitions;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;

public interface SinglePlannerAgent {

	//Methods
	public Plan getPlan();
	public int getPlanElementIndex();
	public void setPlanElementIndex(int index);
	public boolean planLegActivityLeg(double startTime, Id startFacilityId, double endTime, Id endFacilityId);
	public void advanceToNextActivity(double now);

}
