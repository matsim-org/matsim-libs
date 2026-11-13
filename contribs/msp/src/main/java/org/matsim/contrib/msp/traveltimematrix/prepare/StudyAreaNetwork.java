package org.matsim.contrib.msp.traveltimematrix.prepare;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.router.speedy.LeastCostPathTree;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Cuts the part of a network the matrix is built over: the links of one mode inside a study area, as one
 * connected network.
 * <p>
 * A travel time matrix is quadratic in the number of zones and the number of nodes, so a Germany-wide
 * network is not something one builds a matrix over when the question is about one city. Cutting it is
 * therefore not an optimisation but the thing that makes the matrix possible at all.
 * <p>
 * <b>The link ids are kept.</b> That is the whole point of cutting rather than re-generating: the
 * congested travel times and the volumes measured on the <em>full</em> run still address the links of the
 * cut network, so the congestion index of the study area can be measured without re-running anything.
 * {@link NetworkFilterManager} copies the attributes along with them, so the OSM highway type the
 * {@link org.matsim.contrib.msp.traveltimematrix.RoadClassification road classes} are derived from
 * survives as well.
 * <p>
 * <b>Cleaning for a mode is not enough.</b> The network this hands out has to be strongly connected,
 * because a zonal matrix throws rather than leave a pair unanswered, and two things get in the way of
 * that which {@link NetworkUtils#cleanNetwork} does not fix - it takes a mode away from a link rather
 * than removing the link, and it cleans per mode while the matrix's router merges the turn restrictions
 * of all of them. Both are dealt with here, and {@link #verifyRoutable} then checks the result on the
 * graph the matrix will actually route, which costs two shortest-path searches and saves finding out
 * halfway through a matrix.
 * <p>
 * <b>Give the area a buffer.</b> Around a city, the quickest way between two points often leaves the
 * city and comes back - the motorway ring is outside the administrative boundary almost everywhere. Cut
 * at the boundary and those paths are gone, so the matrix reports the trip as slower than it is, in a
 * way that looks like congestion but is not. A few kilometres of buffer around the area the results are
 * reported for is the cheap fix, and the log line below is how to check it was enough: a sharp drop in
 * links against a modest drop in the area means the cut went through the ring.
 */
public final class StudyAreaNetwork {

	private static final Logger log = LogManager.getLogger(StudyAreaNetwork.class);

	private StudyAreaNetwork() {
	}

	/**
	 * The car subnetwork of the study area, cleaned.
	 *
	 * @see #cut(Network, StudyArea, Set)
	 */
	public static Network cutCarNetwork(Network network, @Nullable StudyArea studyArea) {
		return cut(network, studyArea, Set.of(TransportMode.car));
	}

	/**
	 * @param network the network of the run, untouched - the cut network is a copy
	 * @param studyArea the area to keep, or null to keep the whole network and only filter by mode. A
	 * link is kept if <b>both</b> of its nodes are inside: a link with one end outside leads out of the
	 * area, and keeping it would put travel times into the matrix whose congestion was never measured.
	 * @param modes the modes a link has to allow at least one of
	 * @return a new network of the links that survived, with their ids and attributes, reduced to its
	 * largest strongly connected component for the given modes by
	 * {@link NetworkUtils#cleanNetwork(Network, Set)} - a zonal matrix needs every pair of nodes to be
	 * connected, so a dead end left in by the cut would make the matrix throw rather than be slightly
	 * wrong
	 */
	public static Network cut(Network network, @Nullable StudyArea studyArea, Set<String> modes) {

		NetworkFilterManager filter = new NetworkFilterManager(network, new NetworkConfigGroup());
		filter.addLinkFilter(link -> link.getAllowedModes().stream().anyMatch(modes::contains));
		if (studyArea != null) {
			filter.addLinkFilter(link -> inside(studyArea, link));
		}

		Network cut = filter.applyFilters();
		int linksBeforeCleaning = cut.getLinks().size();
		NetworkUtils.cleanNetwork(cut, modes);
		int removedByCleaning = linksBeforeCleaning - cut.getLinks().size();
		int disabledByCleaning = removeLinksWithoutModes(cut, modes);

		log.info("Study area network: {} of {} links ({}%) allow {} and lie inside the area; cleaning for a "
				+ "connected network removed another {} and took the mode away from {} more, leaving {} links and "
				+ "{} nodes.", linksBeforeCleaning, network.getLinks().size(),
			String.format(Locale.ROOT, "%.1f", 100.0 * linksBeforeCleaning / network.getLinks().size()), modes,
			removedByCleaning, disabledByCleaning, cut.getLinks().size(), cut.getNodes().size());

		if (cut.getLinks().isEmpty()) {
			throw new IllegalArgumentException("No link of the network allows " + modes + " inside the study area, so "
				+ "there is nothing to build a matrix over. Check the modes and whether the shape file and the "
				+ "network use the same coordinate reference system.");
		}

		if (!unroutableNodes(cut).isEmpty() && NetworkUtils.hasTurnRestrictions(cut)) {
			log.warn("The cut network is not strongly connected and it carries turn restrictions, so the "
				+ "restrictions are now dropped from it - see removeTurnRestrictions(..) for why that is the right "
				+ "trade for a travel time matrix.");
			removeTurnRestrictions(cut);
		}

		verifyRoutable(cut);
		return cut;
	}

	/**
	 * Drops every turn restriction from the network, which is a thing one may do to a <b>matrix</b>
	 * network and not to a simulation network.
	 * <p>
	 * It is needed because the cleaning and the routing do not mean the same thing by a turn restriction.
	 * {@link NetworkUtils#cleanNetwork} makes the network connected under the restrictions <em>of the
	 * modes it is given</em>, while the router the matrix uses - {@code SpeedyGraphBuilder.build(network)}
	 * with no mode - "merges turn restrictions of all modes", which its own javadoc notes "could result in
	 * a network which is more restrictive than necessary". A network that is properly cleaned for car can
	 * therefore still be unroutable in the matrix's graph, because of a restriction that only ever
	 * applied to trucks. That is the second way to arrive at "Undefined Time", and no amount of cleaning
	 * for car fixes it.
	 * <p>
	 * Dropping them is within the error budget of this method by a wide margin: a turn restriction costs
	 * a path a few seconds of detour, while the matrix answers with a zonal travel time scaled by a
	 * network-wide congestion index. It is also why {@link #cut} hands out a <b>copy</b> - the network of
	 * the run is untouched.
	 */
	public static void removeTurnRestrictions(Network network) {

		long restricted = network.getLinks().values().stream()
			.filter(link -> NetworkUtils.getDisallowedNextLinks(link) != null)
			.peek(NetworkUtils::removeDisallowedNextLinks)
			.count();

		log.info("Removed the turn restrictions of {} links of the matrix network. The travel times of a few paths "
			+ "are now a few seconds too low; the alternative is a matrix that cannot be built at all.", restricted);
	}

	/**
	 * Removes the links that {@link NetworkUtils#cleanNetwork} only took the mode away from, and the nodes
	 * that are then left without any.
	 * <p>
	 * This is the step that is easy to miss, and leaving it out produces the one error that costs an hour
	 * to see: <em>"Undefined Time. Reason could be that the dvrp network is not fully connected"</em>,
	 * thrown by the zonal matrix after it has computed everything else. The cleaner is not at fault - it
	 * does what it says. {@code MultimodalNetworkCleaner} makes the <b>mode</b> connected, so for a link
	 * outside the connected cluster it removes the mode and deletes the link only if no mode is left on
	 * it. A car dead-end that also allows {@code ride} - which on a network from the usual OSM pipeline
	 * is nearly every car dead-end there is - therefore stays in the network as a {@code ride} link.
	 * <p>
	 * And the router does not care: {@code SpeedyGraphBuilder} builds its graph from <b>all</b> links of
	 * the network "without caring about allowedModes". So the matrix would route over a graph that still
	 * has the dead-end in it, pick the node behind it as the central node of its zone, and find no way
	 * back out.
	 *
	 * @return how many links were removed
	 */
	private static int removeLinksWithoutModes(Network network, Set<String> modes) {

		List<Id<Link>> withoutTheMode = network.getLinks().values().stream()
			.filter(link -> link.getAllowedModes().stream().noneMatch(modes::contains))
			.map(Link::getId)
			.toList();

		withoutTheMode.forEach(network::removeLink);
		NetworkUtils.removeNodesWithoutLinks(network);
		return withoutTheMode.size();
	}

	/**
	 * Checks what a travel time matrix actually needs - that every node can be reached from every other
	 * one - and says which nodes break it if any do.
	 * <p>
	 * Two shortest-path searches over the <b>same graph the matrix will route</b>, forwards and
	 * backwards from one node: a node that both searches reach is connected to every other such node, so
	 * two searches settle a question that would otherwise surface as one failed query somewhere in the
	 * middle of a computation of thousands. It is deliberately done on
	 * {@link SpeedyGraphBuilder#build(Network)} and {@link LeastCostPathTree}, and not on the link
	 * topology: those are what the matrix uses, so this also catches a turn restriction that leaves a
	 * node unreachable in the expanded routing graph while the plain graph looks perfectly connected.
	 *
	 * @throws IllegalArgumentException naming up to {@value #NODES_TO_REPORT} of the offending nodes
	 */
	public static void verifyRoutable(Network network) {

		List<Id<Node>> unroutable = unroutableNodes(network);
		if (unroutable.isEmpty()) {
			log.info("Every one of the {} nodes of the study area network can be reached from every other one, so "
				+ "the matrix can answer any pair of them.", network.getNodes().size());
			return;
		}

		throw new IllegalArgumentException(String.format(Locale.ROOT, "The study area network of %d nodes is not "
				+ "strongly connected, so a travel time matrix over it would fail on the pairs it cannot route "
				+ "('Undefined Time. Reason could be that the dvrp network is not fully connected'). %d nodes are "
				+ "cut off from the rest%s. Two things cause this and neither is fixed by cleaning the network for "
				+ "a mode: a link that the cleaner only took the mode away from while the router ignores modes "
				+ "entirely, and a turn restriction of another mode, which the router merges in - see "
				+ "removeLinksWithoutModes and removeTurnRestrictions of this class.",
			network.getNodes().size(), unroutable.size(), examples(unroutable)));
	}

	/**
	 * The nodes that cannot be reached from the rest of the network, or cannot reach it: two searches over
	 * the matrix's own routing graph, forwards and backwards from one node.
	 */
	private static List<Id<Node>> unroutableNodes(Network network) {

		Node reference = network.getNodes().values().iterator().next();
		TravelTime travelTime = new FreeSpeedTravelTime();
		LeastCostPathTree tree = new LeastCostPathTree(SpeedyGraphBuilder.build(network), travelTime,
			new TimeAsTravelDisutility(travelTime));

		tree.calculate(reference, 0, null, null);
		List<Id<Node>> unreachable = undefined(network, tree);

		// backwards from a link out of the reference node rather than from the node itself, which is the
		// only form the backward search takes
		tree.calculateBackwards(reference.getOutLinks().values().iterator().next(), 0, null, null);
		List<Id<Node>> cannotReach = undefined(network, tree);

		return Stream.concat(unreachable.stream(), cannotReach.stream()).distinct().toList();
	}

	/**
	 * How many offending node ids an error message carries. Enough to find them in the network file, few
	 * enough to read.
	 */
	private static final int NODES_TO_REPORT = 10;

	private static List<Id<Node>> undefined(Network network, LeastCostPathTree tree) {
		return network.getNodes().values().stream()
			.filter(node -> tree.getTime(tree.getNodeIndex(node)).isUndefined())
			.map(Node::getId)
			.toList();
	}

	private static String examples(List<Id<Node>> nodes) {
		if (nodes.isEmpty()) {
			return "";
		}
		List<Id<Node>> firstFew = nodes.subList(0, Math.min(NODES_TO_REPORT, nodes.size()));
		return " (" + firstFew + (nodes.size() > firstFew.size() ? ", ..." : "") + ")";
	}

	/**
	 * A link counts as inside when both of its nodes are. Using the nodes rather than the link's own
	 * coordinate matters at the edge of the area: the matrix routes from node to node, so a link whose
	 * far node was cut away is a link into nowhere.
	 */
	private static boolean inside(StudyArea studyArea, Link link) {
		return studyArea.contains(link.getFromNode().getCoord()) && studyArea.contains(link.getToNode().getCoord());
	}
}
