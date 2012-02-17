package playground.anhorni.surprice;

import java.util.List;

import org.matsim.api.core.v01.population.Plan;

public class DecisionModel {
	
	public Plan getPlan(List<Plan> plans) {
		return plans.get(0);
	}

}
