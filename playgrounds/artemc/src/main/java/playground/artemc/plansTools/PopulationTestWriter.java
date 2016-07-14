package playground.artemc.plansTools;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class PopulationTestWriter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = ((MutableScenario)scenario).getPopulation();
		PopulationFactory populationFactory = population.getFactory();
		Person newPerson = populationFactory.createPerson(Id.create("newPerson1", Person.class));
		Plan plan = populationFactory.createPlan();
		Activity activity = populationFactory.createActivityFromLinkId("home", Id.create("newLink1", Link.class));
		activity.setEndTime(3600.0*7);
		plan.addActivity(activity);
		Leg leg = populationFactory.createLeg("car");
		
		plan.addLeg(leg);
		
		
		
		newPerson.addPlan(plan);
	}

}
