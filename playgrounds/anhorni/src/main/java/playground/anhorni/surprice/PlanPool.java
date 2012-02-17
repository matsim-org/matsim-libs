package playground.anhorni.surprice;

import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

public class PlanPool {
	
	private List<Plan> plans = new Vector<Plan>();
		
	public Plan getPlan(AgentMemory memory) {
		return plans.get(0);
	}
	
	public void create(Population population) {
		for (Person p : population.getPersons().values()) {
			this.plans.add(p.getSelectedPlan());
		}
	}
}
