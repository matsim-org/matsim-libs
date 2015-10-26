package playground.johannes.coopsim.analysis;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import playground.johannes.coopsim.pysical.Trajectory;

public class LegPurposeCondition implements PlanElementCondition<Leg> {

	private final String purpose;
	
	public LegPurposeCondition(String purpose) {
		this.purpose = purpose;
	}
	
	@Override
	public boolean test(Trajectory t, Leg element, int idx) {
		Activity act = (Activity) t.getElements().get(idx + 1);
		return act.getType().equalsIgnoreCase(purpose);
	}

}
