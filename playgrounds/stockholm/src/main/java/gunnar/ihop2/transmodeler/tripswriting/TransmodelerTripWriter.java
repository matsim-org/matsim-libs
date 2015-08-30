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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransmodelerTripWriter {

	// -------------------- CONSTANTS --------------------

	// TODO Is this defined somewhere in MATSim?
	private static final String car = "car";

	private static final String ID = "ID";
	private static final String OriID = "OriID";
	private static final String DesID = "DesID";
	private static final String OriType = "OriType";
	private static final String DesType = "DesType";
	private static final String OriLink = "OriLink";
	private static final String Path = "Path";
	private static final String EndLink = "EndLink";
	private static final String DepTime = "DepTime";

	private static final String Node = "Node";

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
		tripWriter.addKeys(ID, OriID, DesID, OriType, DesType, OriLink, Path,
				EndLink, DepTime);
		tripWriter.open(tripFileName);

		for (Map.Entry<Id<Person>, ? extends Person> id2personEntry : this.population
				.getPersons().entrySet()) {
			final Plan plan = id2personEntry.getValue().getSelectedPlan();
			if (plan != null) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) { // TODO Use "instanceof"?
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

								// Avoid double-storing identical paths.
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

	public static void main(String[] args) {

	}

}
