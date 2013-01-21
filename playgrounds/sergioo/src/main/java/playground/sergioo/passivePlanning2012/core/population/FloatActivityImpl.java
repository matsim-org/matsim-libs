package playground.sergioo.passivePlanning2012.core.population;

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

import playground.sergioo.passivePlanning2012.api.population.FloatActivity;

public class FloatActivityImpl implements FloatActivity {

	//Attributes
	private String type;
	private double maximumDuration;
	private final ActivityParams activityParams;

	//Methods
	public FloatActivityImpl(String type, double maximumDuration, ActivityParams activityParams) {
		super();
		this.type = type;
		this.maximumDuration = maximumDuration;
		this.activityParams = activityParams;
	}
	@Override
	public String getType() {
		return type;
	}
	@Override
	public void setType(String type) {
		this.type = type;
	}
	@Override
	public double getMaximumDuration() {
		return maximumDuration;
	}
	@Override
	public void setMaximumDuration(double maximumDuration) {
		this.maximumDuration = maximumDuration;
	}
	@Override
	public ActivityParams getActivityParams() {
		return activityParams;
	}

}
