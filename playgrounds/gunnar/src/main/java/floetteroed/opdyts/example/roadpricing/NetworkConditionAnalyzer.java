package floetteroed.opdyts.example.roadpricing;

import opdytsintegration.OccupancyAnalyzer;
import opdytsintegration.TimeDiscretization;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.RoadPricingConfigGroup;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class NetworkConditionAnalyzer {

	public NetworkConditionAnalyzer() {
	}

	public static void main(String[] args) {

		final int analyzedIteration = 100;
		final Config config = ConfigUtils.loadConfig(
				"./input/roadpricing/config.xml", new RoadPricingConfigGroup());
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final OccupancyAnalyzer occupAnalyzer = new OccupancyAnalyzer(
				new TimeDiscretization(0, 1800, 48), scenario.getNetwork()
						.getLinks().keySet());
		final EventsManager events = EventsUtils.createEventsManager(config);
		events.addHandler(occupAnalyzer);
		final MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(config.getModule("controler").getValue(
				"outputDirectory")
				+ "_0/ITERS/it."
				+ analyzedIteration
				+ "/"
				+ analyzedIteration
				+ ".events.xml.gz");
		System.out.println();
		for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
			System.out.print(linkId);
			for (int bin = 0; bin < 48; bin++) {
				System.out.print("\t");
				final Link link = scenario.getNetwork().getLinks().get(linkId);
				final double relOccup = occupAnalyzer.getOccupancy_veh(linkId,
						bin)
						/ (link.getLength() * link.getNumberOfLanes() / 7.5);
				// TODO no getter
				System.out.print(relOccup);
			}
			System.out.println();
		}
	}
}
