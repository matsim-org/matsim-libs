package playground.mzilske.teach.tasks2012;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;

public class ScanPlansForPeopleWhoUseALink {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile("output/equil/ITERS/it.1/1.plans.xml");
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			Activity home = (Activity) plan.getPlanElements().get(0);
			home.getCoord();
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					Route route = leg.getRoute();
					NetworkRoute networkRoute = (NetworkRoute) route;
					System.out.println(networkRoute.getStartLinkId());
					for (Id linkId : networkRoute.getLinkIds()) {
						System.out.println(linkId);
					}
					System.out.println(networkRoute.getEndLinkId());
				}
			}
		}
	}
	
}
