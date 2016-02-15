package playground.johannes.coopsim.analysis;

import org.matsim.api.core.v01.population.PlanElement;
import playground.johannes.coopsim.pysical.Trajectory;

public class DefaultCondition implements PlanElementCondition {

	private static DefaultCondition instance;
	
	public static DefaultCondition getInstance() {
		if(instance == null)
			instance = new DefaultCondition();
		
		return instance;
	}
	
	@Override
	public boolean test(Trajectory t, PlanElement element, int idx) {
		return true;
	}

}
