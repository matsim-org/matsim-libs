package playground.sergioo.passivePlanning2012.api.population;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

public interface FloatActivity extends PlanElement {

	//Methods
	public String getType();
	public void setType(final String type);
	public double getMaximumDuration();
	public void setMaximumDuration(double seconds);
	public ActivityParams getActivityParams();

}
