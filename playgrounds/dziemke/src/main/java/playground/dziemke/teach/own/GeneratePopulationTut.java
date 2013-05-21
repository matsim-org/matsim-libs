package playground.dziemke.teach.own;

import org.matsim.api.core.v01.Coord;
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
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class GeneratePopulationTut {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		
		config.controler().setLastIteration(0);
		
		config.controler().setOutputDirectory("D:/Workspace/container/potsdam-tut/output");
		
		ActivityParams home = new ActivityParams("home");
		home.setTypicalDuration(16*60*60);
		config.planCalcScore().addActivityParams(home);
		ActivityParams work = new ActivityParams("work");
		work.setTypicalDuration(8*60*60);
		config.planCalcScore().addActivityParams(work);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scenario).readFile("D:/Workspace/container/potsdam-tut/brandenburg-potsdam-merged.xml");
		
		Population population = fillScenario(scenario);
		
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	private static Population fillScenario(Scenario scenario) {
		Population population = scenario.getPopulation();
		PotsdamPopulation potsdamPopulation = new PotsdamPopulation (population);
		population = potsdamPopulation.getPopulation();		
		return population;
	}

}
