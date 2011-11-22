package playground.yu.newPlans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PlanScoreEraser extends NewPopulation implements PlanAlgorithm {

	public static void main(String[] args) {
		String networkFilename = "test/input/2car1ptRoutes/net2.xml"//
			, populationFilename = "test/input/2car1ptRoutes/PC/200.pop100.xml.gz"//
				, outputPopulationFilename = "test/output/planScoreEraser/outputPop.xml.gz";

		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFilename);
		new MatsimPopulationReader(scenario).readFile(populationFilename);

		Population population = scenario.getPopulation();

		PlanScoreEraser pe = new PlanScoreEraser(scenario.getNetwork(),
				population, outputPopulationFilename);
		pe.run(population);
		pe.writeEndPlans();
	}

	public PlanScoreEraser(Network network, Population population,
			String outputPopulationFilename) {
		super(network, population, outputPopulationFilename);
	}

	@Override
	protected void beforeWritePersonHook(Person person) {
		for (Plan plan : person.getPlans()) {
			run(plan);
		}
	}

	@Override
	public void run(Plan plan) {
		plan.setScore(null);
	}
}
