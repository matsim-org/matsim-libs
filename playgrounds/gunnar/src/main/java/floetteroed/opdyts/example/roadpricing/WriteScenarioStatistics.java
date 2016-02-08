package floetteroed.opdyts.example.roadpricing;

import opdytsintegration.DistanceBasedFilter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class WriteScenarioStatistics {

	public static void main(String[] args) {

		final String configFileName = "./input/matsim-config.xml";
		System.out.println("Loading config file " + configFileName);

		final Config config = ConfigUtils.loadConfig(configFileName,
				new RoadPricingConfigGroup());
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		System.out.println("Number of nodes:  "
				+ scenario.getNetwork().getNodes().size());
		System.out.println("Number of links:  "
				+ scenario.getNetwork().getLinks().size());
		final double distance = 6000.0;
		System.out.println("Number of links in "
				+ distance
				+ " meter radius around toll zone: "
				+ (new DistanceBasedFilter(674000, 6581000, distance))
						.allAcceptedLinkIds(
								scenario.getNetwork().getLinks().values())
						.size());
		System.out.println("Number of agents: "
				+ scenario.getPopulation().getPersons().size());
	}

}
