package playground.anhorni.surprice.preprocess;

import java.util.List;
import java.util.Vector;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

import playground.anhorni.surprice.AgentMemory;
import playground.anhorni.surprice.DecisionModel;

public class PlanPool {
	
	private List<Plan> plans = new Vector<Plan>();
		
	public Plan getPlan(AgentMemory memory, DecisionModel decisionModel) {
		return decisionModel.getPlan(this.plans, memory);
	}
	
	public void create(Population population) {
		for (Person p : population.getPersons().values()) {
			this.plans.add(p.getSelectedPlan());
		}
	}
}
