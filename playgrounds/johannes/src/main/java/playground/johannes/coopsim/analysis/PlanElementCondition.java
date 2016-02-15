package playground.johannes.coopsim.analysis;

import org.matsim.api.core.v01.population.PlanElement;
import playground.johannes.coopsim.pysical.Trajectory;

public interface PlanElementCondition<T extends PlanElement> {

	public boolean test(Trajectory t, T element, int idx);
	
}
