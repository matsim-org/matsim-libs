package tutorial.programming.example08DemandGeneration;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunPCreateDrivingAgents {

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();

		config.controler().setLastIteration(0);

		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(16 * 60 * 60);
		config.planCalcScore().addActivityParams(home);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(8 * 60 * 60);
		config.planCalcScore().addActivityParams(work);

		Scenario scenario = ScenarioUtils.createScenario(config);

		new MatsimNetworkReader(scenario).readFile("input/network.xml");

		fillScenario(scenario);

		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.run();


	}

	private static Population fillScenario(Scenario scenario) {
		Population population = scenario.getPopulation();

		for (int i = 0; i < 1000; i++) {
			Coord coord = scenario.createCoord(454941 + i*10, 5737814+i*10);
			Coord coordWork = scenario.createCoord(454941-i*10, 5737814-i*10);
			createOnePerson(scenario, population, i, coord, coordWork);
		}

		return population;
	}

	private static void createOnePerson(Scenario scenario,
		Population population, int i, Coord coord, Coord coordWork) {
		Person person = population.getFactory().createPerson(Id.createPersonId("p_"+i));

		Plan plan = population.getFactory().createPlan();


		Activity home = population.getFactory().createActivityFromCoord("home", coord);
		home.setEndTime(9*60*60);
		plan.addActivity(home);

		Leg hinweg = population.getFactory().createLeg("car");
		plan.addLeg(hinweg);

		Activity work = population.getFactory().createActivityFromCoord("work", coordWork);
		work.setEndTime(17*60*60);
		plan.addActivity(work);

		Leg rueckweg = population.getFactory().createLeg("car");
		plan.addLeg(rueckweg);

		Activity home2 = population.getFactory().createActivityFromCoord("home", coord);
		plan.addActivity(home2);

		person.addPlan(plan);
		population.addPerson(person);
	}

}

