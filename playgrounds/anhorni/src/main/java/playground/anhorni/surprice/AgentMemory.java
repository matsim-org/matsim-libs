package playground.anhorni.surprice;

import java.util.List;
import java.util.Vector;
import org.matsim.api.core.v01.population.Plan;

public class AgentMemory {
	
	private List<Plan> plans = new Vector<Plan>();
	
	public void addPlan(Plan plan) {
		this.plans.add(plan);
	}

}
