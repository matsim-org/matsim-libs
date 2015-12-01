package gunnar.ihop2.transmodeler;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class RoutesRemover {

	private RoutesRemover() {
	}

	public static void run(final String fromPopFile, final String toPopFile,
			final String networkFileName) {

		final Config config = ConfigUtils.createConfig();
		config.getModule("network").addParam("inputNetworkFile",
				networkFileName);
		config.getModule("plans").addParam("inputPlansFile", fromPopFile);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						final Leg leg = (Leg) planElement;
						leg.setRoute(null);
					}
				}
			}
		}

		PopulationWriter popwriter = new PopulationWriter(
				scenario.getPopulation(), scenario.getNetwork());
		popwriter.write(toPopFile);
	}

	public static void main(String[] args) {
		run("./ihop2/matsim-output/ITERS/it.0/0.plans.xml.gz",
				"./ihop2/matsim-input/plans-wout-routes.xml",
				"./ihop2/network-output/network.xml");
	}

}
