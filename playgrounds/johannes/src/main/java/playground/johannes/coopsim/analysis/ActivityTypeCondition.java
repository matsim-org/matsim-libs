package playground.johannes.coopsim.analysis;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import playground.johannes.coopsim.pysical.Trajectory;

public class ActivityTypeCondition implements PlanElementCondition {

	private final String type;
	
	public ActivityTypeCondition(String type) {
		this.type = type;
	}

	@Override
	public boolean test(Trajectory t, PlanElement element, int idx) {
		return ((Activity)element).getType().equalsIgnoreCase(type);
	}

}
