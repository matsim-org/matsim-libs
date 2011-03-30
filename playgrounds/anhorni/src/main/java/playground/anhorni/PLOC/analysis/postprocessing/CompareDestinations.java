package playground.anhorni.PLOC.analysis.postprocessing;

import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;

public class CompareDestinations {
	
	private TreeMap<Id, AgentDestinations> agentDestinations = new TreeMap<Id, AgentDestinations>();
		
	public void handleScenario(Scenario scenario) {		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (this.agentDestinations.get(person.getId()) == null) {
				this.agentDestinations.put(person.getId(), new AgentDestinations(person.getId()));
			}
			Plan bestPlan = CompareScenarios.getBestPlan(person);
			
			int actLegIndex = 0;
			for (PlanElement pe : bestPlan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					this.agentDestinations.get(person.getId()).addDestination(actLegIndex, ((ActivityImpl) pe).getCoord());
					actLegIndex += 2;
				}
			}
		}
	}
	
	public double evaluateScenarios() {
		double distancesFromCenter = 0.0;		
		for (AgentDestinations agentDestinations : this.agentDestinations.values()) {
			distancesFromCenter += agentDestinations.getAverageDistanceFromCenterPointForAllActivities();
		}
		return distancesFromCenter /= this.agentDestinations.values().size();
 	}
}
