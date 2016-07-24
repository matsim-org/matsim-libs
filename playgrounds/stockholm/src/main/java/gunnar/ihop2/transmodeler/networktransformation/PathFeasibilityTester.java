package gunnar.ihop2.transmodeler.networktransformation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class PathFeasibilityTester {

	private final Map<String, TransmodelerLink> tmLinkId2tmLink;

	private final ObjectAttributes linkAttributes;

	public PathFeasibilityTester(final String tmNodesFileName,
			final String tmLinksFileName, final String tmSegmentsFileName,
			final String tmLanesFileName,
			final String tmLaneConnectorsFileName,
			final String linkAttributesFileName) throws IOException {
		final TransmodelerNodesReader nodesReader = new TransmodelerNodesReader(
				tmNodesFileName);
		final TransmodelerLinksReader linksReader = new TransmodelerLinksReader(
				tmLinksFileName, nodesReader.id2node);
		final TransmodelerSegmentsReader segmentsReader = new TransmodelerSegmentsReader(
				tmSegmentsFileName, linksReader.id2link);
		final TransmodelerLaneReader lanesReader = new TransmodelerLaneReader(
				tmLanesFileName, segmentsReader.unidirSegmentId2link);
		final TransmodelerLaneConnectorReader connectorsReader = new TransmodelerLaneConnectorReader(
				tmLaneConnectorsFileName, lanesReader.upstrLaneId2link,
				lanesReader.downstrLaneId2link);
		this.tmLinkId2tmLink = linksReader.id2link;

		this.linkAttributes = new ObjectAttributes();
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
				linkAttributes);
		reader.readFile(linkAttributesFileName);
	}

	private List<Id<Link>> extractLinks(final Leg leg) {
		final List<Id<Link>> result = new ArrayList<>();
		if (leg.getRoute() instanceof NetworkRoute) {
			final NetworkRoute route = (NetworkRoute) leg.getRoute();
			result.add(route.getStartLinkId());
			result.addAll(route.getLinkIds());
			// result.add(route.getEndLinkId());
		}
		return result;
	}

	public void run(final String networkFile, final String populationFile) {

		final Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		config.plans().setInputFile(populationFile);

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			final Plan plan = person.getSelectedPlan();
			if (plan != null) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						final List<Id<Link>> linkIds = this
								.extractLinks((Leg) planElement);
						for (int i = 0; i < linkIds.size() - 1; i++) {
							final String fromLinkId = linkIds.get(i).toString();
							final String toLinkId = linkIds.get(i + 1)
									.toString();
							if (!this.tmLinkId2tmLink.get(fromLinkId).downstreamLink2turnLength
									.containsKey(this.tmLinkId2tmLink
											.get(toLinkId))) {
								System.out.println("Forbidden turn from link "
										+ fromLinkId + " to link " + toLinkId
										+ "; link sequence is " + linkIds);
							}
						}
					}
				}
			}
		}

	}

	public static void main(String[] args) throws IOException {

		System.out.println("STARTED ...");

		final String tmNodesFileName = "./data_ZZZ/transmodeler/Nodes.csv";
		final String tmLinksFileName = "./data_ZZZ/transmodeler/Links.csv";
		final String tmSegmentsFileName = "./data_ZZZ/transmodeler/Segments.csv";
		final String tmLanesFileName = "./data_ZZZ/transmodeler/Lanes.csv";
		final String tmLaneConnectorsFileName = "./data_ZZZ/transmodeler/Lane Connectors.csv";
		final String linkAttributesFileName = "./test/matsim-testrun/input/link-attributes.xml";

		final PathFeasibilityTester tester = new PathFeasibilityTester(
				tmNodesFileName, tmLinksFileName, tmSegmentsFileName,
				tmLanesFileName, tmLaneConnectorsFileName,
				linkAttributesFileName);

		final String matsimNetworkFile = "./test/matsim-testrun/input/network-plain.xml";
		final String matsimPopulationFile = "./test/matsim-testrun/matsim-output/ITERS/it.0/0.plans.xml.gz";
		tester.run(matsimNetworkFile, matsimPopulationFile);

		System.out.println("... DONE");
	}

}
