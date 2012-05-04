package playground.sergioo.passivePlanning.api.population;

import java.util.Collection;

import org.matsim.api.core.v01.population.Plan;

public interface BasePlan extends Plan {

	//Methods
	public Collection<FloatActivity> getFloatActivities();
	public void addFloatActivity(FloatActivity floatActivity);
	public Plan getAndSelectPlan();

}
