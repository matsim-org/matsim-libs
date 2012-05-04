package playground.sergioo.passivePlanning.core.mobsim.passivePlanning.definitions;

import org.matsim.api.core.v01.population.Plan;

public interface SinglePlannerAgent {

	//Methods
	public Plan getPlan();
	public void setTime(double time);
	public boolean isPlanned();
	public boolean planLegActivity();

}
