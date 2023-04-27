package ch.sbb.matsim.contrib.railsim.prototype.prepare;

import ch.sbb.matsim.contrib.railsim.prototype.RailsimUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ihab Kaddoura
 */
public class SplitTransitLinks {
	private static final Logger log = LogManager.getLogger(SplitTransitLinks.class);

	private final HashMap<String, List<Id<Link>>> connectedLinks = new HashMap<>();
	private final Scenario scenario;

	/**
	 * @param scenario
	 */
	public SplitTransitLinks(Scenario scenario) {
		this.scenario = scenario;
	}

	/**
	 * Splits all links in the network into smaller link segments and adjusts the network routes in the transit schedule.
	 * Network links which have a transit stop facility are skipped.
	 *
	 * @param maximumLinkLength
	 */
	public void run(double maximumLinkLength) {
		Set<Id<Link>> stopLinkIds = new HashSet<>();
		for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
			stopLinkIds.add(facility.getLinkId());
		}

		Map<Id<Link>, List<Id<Link>>> link2splitLinks = new HashMap<>();

		List<Link> linksFromNetwork = new ArrayList<>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			linksFromNetwork.add(link);
		}

		for (Link link : linksFromNetwork) {
			if (stopLinkIds.contains(link.getId())) {
				log.info("Skipping link " + link.getId() + " (transit stop)");

			} else if (RailsimUtils.getOppositeDirectionLink(link, scenario.getNetwork()) != null) {
				log.info("Skipping link " + link.getId() + " (one direction track)");
				// TODO: Once we have the use case, allow for splitting these links (and make sure the one direction logic is correctly transferred to all link segments...)

			} else {
				if (link.getLength() > maximumLinkLength) {
					log.info("Splitting link " + link.getId());
					List<Id<Link>> links = connectNodes(link.getId().toString(), link.getFromNode(), link.getToNode(), link.getFreespeed(), link.getLength(), maximumLinkLength, link.getAttributes());
					link2splitLinks.put(link.getId(), links);
				} else {
					log.info("Skipping link " + link.getId() + " (below maximumLength)");
				}
			}
		}

		for (Id<Link> linkId : link2splitLinks.keySet()) {
			// remove old link from the scenario
			this.scenario.getNetwork().removeLink(linkId);

			// replace old link in all transit routes
			for (TransitLine transitLine : this.scenario.getTransitSchedule().getTransitLines().values()) {
				for (TransitRoute transitRoute : transitLine.getRoutes().values()) {


					NetworkRoute networkRoute = transitRoute.getRoute();

					List<Id<Link>> replacedLinkIds = new ArrayList<>();

					for (Id<Link> linkIdInNetworkRoute : networkRoute.getLinkIds()) {
						if (linkIdInNetworkRoute.toString().equals(linkId.toString())) {
							replacedLinkIds.addAll(link2splitLinks.get(linkIdInNetworkRoute));
						} else {
							replacedLinkIds.add(linkIdInNetworkRoute);
						}
					}

					networkRoute.setLinkIds(networkRoute.getStartLinkId(), replacedLinkIds, networkRoute.getEndLinkId());
				}
			}
		}

//		new NetworkWriter(scenario.getNetwork()).write(scenario.getConfig().controler().getOutputDirectory() + "../modified_inputTrainNetwork.xml");
//		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(scenario.getConfig().controler().getOutputDirectory() + "../modified_inputTransitSchedule.xml");
//		new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(scenario.getConfig().controler().getOutputDirectory() + "../modified_inputTransitVehicles.xml");
	}

	/**
	 * Connects two nodes with link(s)
	 * <p>
	 * Creates segmented links between nodes. Each segment is itself a link with maximum length (minimumLinkLength / euclideanDistance).
	 *
	 * @param name               the name of the link.
	 * @param fromNode           the source node.
	 * @param toNode             the target node.
	 * @param speedLevel         the maximum speed level allowed on the link.
	 * @param originalLinkLength the original link length (set to <= 0 to use the euclidean distance)
	 * @param linkSegmentLength  the length of the link segments.
	 * @param attributes
	 * @return A list holding the link ids between the connected nodes.
	 */
	public List<Id<Link>> connectNodes(String name, Node fromNode, Node toNode, double speedLevel, double originalLinkLength, double linkSegmentLength, Attributes attributes) {

		// lookup if link already exists
		String nodeIds = fromNode.getId().toString() + "_" + toNode.getId().toString();
		List<Id<Link>> links = connectedLinks.get(nodeIds);
		if (links != null) {
			log.info("Link already exists, skipping " + nodeIds);
			return links;
		}

		// create links
		log.info("Create link  " + nodeIds);

		List<Node> nodes = new ArrayList<>();
		double distance = originalLinkLength;
		if (originalLinkLength <= 0.) {
			distance = NetworkUtils.getEuclideanDistance(fromNode.getCoord(), toNode.getCoord());
		}
		if (distance <= linkSegmentLength) {
			nodes.add(fromNode);
			nodes.add(toNode);
		} else {
			// add first node
			nodes.add(fromNode);

			// add additional nodes in between...

			int nodeCounter = 0;
			for (double fraction = linkSegmentLength / distance; fraction < 1.; ) {
				LineSegment ls = new LineSegment(fromNode.getCoord().getX(), fromNode.getCoord().getY(), toNode.getCoord().getX(), toNode.getCoord().getY());
				Coordinate point = ls.pointAlong(fraction);
				Coord coord = new Coord(point.x, point.y);
				String idBetweenNode = fromNode.getId().toString() + "-" + toNode.getId().toString() + "-" + nodeCounter;
//				log.info("Adding 'between node' " + idBetweenNode);
				Node betweenNode = addNode(idBetweenNode, coord);
				nodes.add(betweenNode);

				fraction = fraction + linkSegmentLength / distance;
				nodeCounter++;
			}

			// add last node
			nodes.add(toNode);
		}

		links = connectStopsAndGetRailLinks(name, nodes, speedLevel, originalLinkLength, attributes);
		connectedLinks.put(nodeIds, links);
		return links;
	}

	private List<Id<Link>> connectStopsAndGetRailLinks(String name, List<Node> nodes, double speedLevel, double originalLinkLength, Attributes attributes) {

		if (nodes.size() < 2) throw new RuntimeException("At least two route stops required. Aborting...");

		List<Id<Link>> railLinks = new ArrayList<>();

		log.info("Connecting nodes...");

		double length = originalLinkLength / (nodes.size() - 1.);
		length = Math.round(length);
		if (length <= 0.) {
			log.warn("An euclidean distance of " + length + " is not accepted.");
			length = 5.;
			log.warn("... the length will be set to " + length);
		}

		int linkCounter = 0;

		Node previousNode = null;
		for (Node node : nodes) {

			if (previousNode == null) {
				// first node
			} else {
				// not the first terminal
				// add connecting link
				Link connectingLink = getOrCreateLink(name, linkCounter, previousNode, node, speedLevel, length, attributes);
				linkCounter++;

				railLinks.add(connectingLink.getId());

			}
			previousNode = node;
		}
		return railLinks;
	}

	public Node addNode(String name, Coord coord) {

		Id<Node> id = Id.createNodeId(name);

		if (this.scenario.getNetwork().getNodes().get(id) != null) {
			return this.scenario.getNetwork().getNodes().get(id);
		}

		Node node = this.scenario.getNetwork().getFactory().createNode(id, coord);
		this.scenario.getNetwork().addNode(node);
		return node;
	}

	private Link getOrCreateLink(String name, int linkCounter, Node fromNode, Node toNode, double freespeed, double length, Attributes attributes) {

		if (length <= 0.) throw new RuntimeException("Length " + length + " not accepted. Aborting...");

		Id<Link> linkId = Id.create(name + "_" + linkCounter, Link.class);

		if (this.scenario.getNetwork().getLinks().get(linkId) != null) {
			return this.scenario.getNetwork().getLinks().get(linkId);
		}

		Link link = this.scenario.getNetwork().getFactory().createLink(linkId, fromNode, toNode);
		link.setAllowedModes(new HashSet<>(List.of("rail")));
		link.setLength(length);
		link.setFreespeed(freespeed);
		link.setCapacity(3600.);
		link.setNumberOfLanes(1.);
		for (String attribute : attributes.getAsMap().keySet()) {
			link.getAttributes().putAttribute(attribute, attributes.getAttribute(attribute));
		}
		this.scenario.getNetwork().addLink(link);

		return link;
	}

}
