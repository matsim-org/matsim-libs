package playground.johannes.coopsim.analysis;

import org.matsim.api.core.v01.population.Leg;
import playground.johannes.coopsim.pysical.Trajectory;

public class LegModeCondition implements PlanElementCondition<Leg> {

	private final String mode;
	
	public LegModeCondition(String mode) {
		this.mode = mode;
	}
	
	@Override
	public boolean test(Trajectory t, Leg element, int idx) {
		return mode.equalsIgnoreCase(element.getMode());
	}

}
