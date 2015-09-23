package gunnar.ihop2.transmodeler.tripswriting;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.TMPATHID_ATTR;
import gunnar.ihop2.utils.TabularFileWriter;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
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
public class TransmodelerTripWriter {

	// -------------------- CONSTANTS --------------------

	// TODO Is this defined somewhere in MATSim?
	private static final String car = "car";

	private static final String ID = "ID";
	private static final String OriID = "OriID";
	private static final String DesID = "DesID";
	private static final String OriType = "OriType";
	private static final String DesType = "DesType";
	private static final String Class = "Class";
	private static final String OriLink = "OriLink";
	private static final String Path = "Path";
	private static final String EndLink = "EndLink";
	private static final String DepTime = "DepTime";

	private static final String Node = "Node";
	private static final String PC1 = "PC1";

	// -------------------- MEMBERS --------------------

	private final Population population;

	private final Network network;

	private final ObjectAttributes linkAttributes;

	// -------------------- CONSTRUCTION --------------------

	public TransmodelerTripWriter(final Population population,
			final Network network, final ObjectAttributes linkAttributes) {
		this.population = population;
		this.network = network;
		this.linkAttributes = linkAttributes;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void writeTrips(final String pathFileName, final String tripFileName)
			throws FileNotFoundException {

		final Map<List<Id<Link>>, Integer> linkIds2pathId = new LinkedHashMap<List<Id<Link>>, Integer>();
		int tripCnt = 0;

		final PrintWriter pathWriter = new PrintWriter(pathFileName);
		pathWriter.println("1"); // path table version number

		final TabularFileWriter tripWriter = new TabularFileWriter();
		tripWriter.setNoDataValue("");
		tripWriter.setSeparator(",");
		tripWriter.addKeys(ID, OriID, DesID, OriType, DesType, Class, OriLink,
				Path, EndLink, DepTime);
		tripWriter.open(tripFileName);

		for (Map.Entry<Id<Person>, ? extends Person> id2personEntry : this.population
				.getPersons().entrySet()) {
			final Plan plan = id2personEntry.getValue().getSelectedPlan();
			if (plan != null) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						final Leg leg = (Leg) planElement;
						if (car.equals(leg.getMode())) {
							final NetworkRoute route = (NetworkRoute) leg
									.getRoute();
							if (route != null) {

								// Include the from-link, exclude the to-link.
								final Id<Node> fromNodeId = this.network
										.getLinks().get(route.getStartLinkId())
										.getFromNode().getId();
								final Id<Node> toNodeId = this.network
										.getLinks().get(route.getEndLinkId())
										.getFromNode().getId();
								final List<Id<Link>> linkIds = new ArrayList<Id<Link>>(
										1 + route.getLinkIds().size());
								linkIds.add(route.getStartLinkId());
								linkIds.addAll(route.getLinkIds());

								/*
								 * Avoid double-storing identical paths.
								 * 
								 * TODO: This does hopefully NOT remove the
								 * one-on-one coupling between persons and paths
								 * because the trips still have unique IDs. But
								 * TM then also writes trip- and not
								 * vehicle-specific events. Possible solutions:
								 * 
								 * (1) Give a person_id -> {trip_id} mapping to
								 * TM and let it use person_id instead of all
								 * corresponding {trip_id}.
								 * 
								 * (2) Let TM write events per trip and
								 * re-assemble this somehow within MATSim.
								 */
								Integer pathId = linkIds2pathId.get(linkIds);
								if (pathId == null) {
									pathId = linkIds2pathId.size();
									linkIds2pathId.put(linkIds, pathId);

									pathWriter.print(pathId);
									pathWriter.print(" { ");
									for (Id<Link> linkId : linkIds) {
										pathWriter.print(this.linkAttributes
												.getAttribute(
														linkId.toString(),
														TMPATHID_ATTR));
										pathWriter.print(" ");
									}
									pathWriter.println("}");
								}

								// Write out the trip.
								tripWriter.setValue(ID, ++tripCnt);
								tripWriter.setValue(OriID, fromNodeId);
								tripWriter.setValue(DesID, toNodeId);
								tripWriter.setValue(OriType, Node);
								tripWriter.setValue(DesType, Node);
								tripWriter.setValue(Class, PC1);
								tripWriter.setValue(OriLink,
										this.linkAttributes.getAttribute(
												linkIds.get(0).toString(),
												TMPATHID_ATTR));
								tripWriter.setValue(Path, pathId);
								tripWriter.setValue(EndLink,
										this.linkAttributes.getAttribute(
												linkIds.get(linkIds.size() - 1)
														.toString(),
												TMPATHID_ATTR));
								tripWriter.setValue(DepTime,
										leg.getDepartureTime());
								tripWriter.writeValues();
							}
						}
					}
				}
			}
		}

		pathWriter.flush();
		pathWriter.close();

		tripWriter.close();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		final String networkFileName = "./data/transmodeler/network.xml";
		final String plansFileName = "./data/saleem/10.plans.xml.gz";
		final String linkAttributesFileName = "./data/transmodeler/linkAttributes.xml";

		final String pathFileName = "./data/saleem/paths.csv";
		final String tripFileName = "./data/saleem/trips.csv";

		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", networkFileName);
		config.setParam("plans", "inputPlansFile", plansFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final ObjectAttributes linkAttributes = new ObjectAttributes();
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
				linkAttributes);
		reader.parse(linkAttributesFileName);

		final TransmodelerTripWriter tripWriter = new TransmodelerTripWriter(
				scenario.getPopulation(), scenario.getNetwork(), linkAttributes);

		tripWriter.writeTrips(pathFileName, tripFileName);

		System.out.println("... DONE");
	}

}
