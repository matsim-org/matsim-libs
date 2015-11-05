package gunnar.ihop2.transmodeler.tripswriting;

import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.TMLINKDIRPREFIX_ATTR;
import static gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork.TMPATHID_ATTR;
import gunnar.ihop2.transmodeler.networktransformation.Transmodeler2MATSimNetwork;
import gunnar.ihop2.utils.TabularFileWriter;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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

	private final ObjectAttributes linkAttributes;

	// -------------------- CONSTRUCTION --------------------

	public TransmodelerTripWriter(final Population population,
			final ObjectAttributes linkAttributes) {
		this.population = population;
		this.linkAttributes = linkAttributes;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void writeTrips(final String pathFileName, final String tripFileName)
			throws FileNotFoundException {

		Logger.getLogger(this.getClass())
				.warn("Recently added directional information (\"-\") to "
						+ "origin and destination link has not been tested within Regent.");

		final Map<List<Id<Link>>, Integer> linkIds2pathId = new LinkedHashMap<List<Id<Link>>, Integer>();
		int tripCnt = 0;

		final PrintWriter pathWriter = new PrintWriter(pathFileName);
		pathWriter.println("1"); // path table version number

		final SortedSet<TransmodelerTrip> sortedTrips = new TreeSet<>();

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

								// final Id<Node> fromNodeId = this.network
								// .getLinks().get(route.getStartLinkId())
								// .getFromNode().getId();
								// final Id<Node> toNodeId = this.network
								// .getLinks().get(route.getEndLinkId())
								// .getFromNode().getId();
								final String fromNodeTmId = (String) this.linkAttributes
										.getAttribute(
												route.getStartLinkId()
														.toString(),
												Transmodeler2MATSimNetwork.TMFROMNODEID_ATTR);
								final String toNodeTmId = (String) this.linkAttributes
										.getAttribute(
												route.getEndLinkId().toString(),
												Transmodeler2MATSimNetwork.TMFROMNODEID_ATTR);
								System.out.print("ROUTE fromNode = "
										+ fromNodeTmId + ", toNode = "
										+ toNodeTmId);

								final List<Id<Link>> linkIds = new ArrayList<Id<Link>>(
										1 + route.getLinkIds().size());
								linkIds.add(route.getStartLinkId());
								// linkIds.addAll(route.getLinkIds());
								for (Id<Link> linkId : route.getLinkIds()) {
									if (this.linkAttributes.getAttribute(
											linkId.toString(), TMPATHID_ATTR) != null) {
										linkIds.add(linkId);
									}
								}
								System.out.println(", links = " + linkIds);

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
									// TODO new added one here because that's what TM wants
									pathId = 1 + linkIds2pathId.size();
									linkIds2pathId.put(linkIds, pathId);

									pathWriter.print(pathId);
									pathWriter.print(" { ");
									for (Id<Link> linkId : linkIds) {
										pathWriter.print(this.linkAttributes
												.getAttribute(
														linkId.toString(),
														TMLINKDIRPREFIX_ATTR));
										pathWriter.print(this.linkAttributes
												.getAttribute(
														linkId.toString(),
														TMPATHID_ATTR));
										pathWriter.print(" ");
									}
									pathWriter.println("}");
								}

								// Memorize the trip.

								final String fromLink = (String) this.linkAttributes
										.getAttribute(
												linkIds.get(0).toString(),
												TMLINKDIRPREFIX_ATTR)
										+ (String) this.linkAttributes
												.getAttribute(linkIds.get(0)
														.toString(),
														TMPATHID_ATTR);
								final String toLink = (String) this.linkAttributes
										.getAttribute(
												linkIds.get(linkIds.size() - 1)
														.toString(),
												TMLINKDIRPREFIX_ATTR)
										+ (String) this.linkAttributes
												.getAttribute(
														linkIds.get(
																linkIds.size() - 1)
																.toString(),
														TMPATHID_ATTR);
								sortedTrips
										.add(new TransmodelerTrip(++tripCnt,
												fromNodeTmId, toNodeTmId,
												fromLink, pathId, toLink, leg
														.getDepartureTime()));
							}
						}
					}
				}
			}
		}

		pathWriter.flush();
		pathWriter.close();

		final TabularFileWriter tripWriter = new TabularFileWriter();
		tripWriter.setNoDataValue("");
		tripWriter.setSeparator(",");
		tripWriter.addKeys(ID, OriID, DesID, OriType, DesType, Class, OriLink,
				Path, EndLink, DepTime);
		tripWriter.open(tripFileName);
		for (TransmodelerTrip trip : sortedTrips) {
			tripWriter.setValue(ID, trip.id);
			tripWriter.setValue(OriID, trip.fromNodeId);
			tripWriter.setValue(DesID, trip.toNodeId);
			tripWriter.setValue(OriType, Node);
			tripWriter.setValue(DesType, Node);
			tripWriter.setValue(Class, PC1);
			tripWriter.setValue(OriLink, trip.fromLinkId);
			tripWriter.setValue(Path, trip.pathId);
			tripWriter.setValue(EndLink, trip.toLinkId);
			tripWriter.setValue(DepTime, trip.dptTime_s);
			tripWriter.writeValues();
		}
		tripWriter.close();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) throws FileNotFoundException {

		System.out.println("STARTED ...");

		// final String networkFileName = "./data_ZZZ/run/network-plain.xml";
		// final String plansFileName =
		// "./data_ZZZ/run/output/ITERS/it.0/0.plans.xml.gz";
		// final String linkAttributesFileName =
		// "./data_ZZZ/run/link-attributes.xml";
		//
		// final String pathFileName = "./data_ZZZ/run/paths.csv";
		// final String tripFileName = "./data_ZZZ/run/trips.csv";

		final String networkFileName = "./test/regentmatsim/input/network-expanded.xml";
		final String plansFileName = "./test/regentmatsim/matsim-output/ITERS/it.0/0.plans.xml.gz";
		final String linkAttributesFileName = "./test/regentmatsim/input/link-attributes.xml";

		final String pathFileName = "./test/regentmatsim/exchange/paths.csv";
		final String tripFileName = "./test/regentmatsim/exchange/trips.csv";

		final Config config = ConfigUtils.createConfig();
		config.setParam("network", "inputNetworkFile", networkFileName);
		config.setParam("plans", "inputPlansFile", plansFileName);
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final ObjectAttributes linkAttributes = new ObjectAttributes();
		final ObjectAttributesXmlReader reader = new ObjectAttributesXmlReader(
				linkAttributes);
		reader.parse(linkAttributesFileName);

		final TransmodelerTripWriter tripWriter = new TransmodelerTripWriter(
				scenario.getPopulation(), linkAttributes);

		tripWriter.writeTrips(pathFileName, tripFileName);

		System.out.println("... DONE");
	}

}
