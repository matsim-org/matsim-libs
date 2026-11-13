package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery;
import org.matsim.core.router.speedy.LeastCostPathTree;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.OptionalTime;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntBiFunction;

/**
 * The thing a travel time matrix has to beat: a free-speed router, timed twice.
 * <p>
 * <b>Twice, because there are two honest ways to count it</b> and quoting only the first flatters the
 * matrix by a factor of hundreds.
 * <ul>
 *     <li>{@link #isolated(Network, double) Isolated} runs one shortest-path search per query, in
 *     whatever order the queries arrive. That is what an analysis pays when it cannot control the order -
 *     an accessibility question about one person at one hour, then another person somewhere else.</li>
 *     <li>{@link #byOrigin(Network, double) By origin} recalculates the tree only when the origin
 *     changes, so one search answers a whole run of consecutive queries. That is the same router used as
 *     well as it can be used, and a far harder target. It is only meaningful over a pool that is
 *     <em>sorted</em> by origin, which is what {@link #sortedByOrigin(List)} is for.</li>
 * </ul>
 * Extracted from the resolution sweep because the network-size sweep needs the same two numbers per
 * network - and there the baseline is not one row of the table but the comparison the whole experiment
 * is about, since both the matrix and the router get more expensive as the network grows and the
 * question is which of them grows faster.
 *
 * @see QueryBenchmark what turns one of these into a query rate
 */
public final class RoutingBaseline {

	private RoutingBaseline() {
	}

	/**
	 * One free-speed shortest-path search per query.
	 *
	 * @param qSimTimeStepSize of the run, so that the travel times are rounded the way the matrix rounds
	 * them - a baseline that disagreed with the matrix about free speed would make the accuracy row of a
	 * sweep unreadable
	 */
	public static ToIntBiFunction<Node, Node> isolated(Network network, double qSimTimeStepSize) {
		LeastCostPathTree tree = tree(network, qSimTimeStepSize);
		return (from, to) -> {
			tree.calculate(from, 0, null, null);
			return travelTime(tree, to);
		};
	}

	/**
	 * One search per origin, reused for as long as the origin does not change. Hand it a pool from
	 * {@link #sortedByOrigin(List)} or it degenerates into {@link #isolated}.
	 */
	public static ToIntBiFunction<Node, Node> byOrigin(Network network, double qSimTimeStepSize) {

		LeastCostPathTree tree = tree(network, qSimTimeStepSize);
		Node[] lastOrigin = {null};

		return (from, to) -> {
			if (from != lastOrigin[0]) {
				tree.calculate(from, 0, null, null);
				lastOrigin[0] = from;
			}
			return travelTime(tree, to);
		};
	}

	/**
	 * The pool in the order {@link #byOrigin} needs. By the <b>id</b> rather than by identity, so that
	 * the order is the same from run to run and two sweeps can be compared.
	 */
	public static List<TravelTimeQuery> sortedByOrigin(List<TravelTimeQuery> pool) {
		return pool.stream()
			.sorted(Comparator.comparing(query -> query.fromNode().getId().toString()))
			.toList();
	}

	private static LeastCostPathTree tree(Network network, double qSimTimeStepSize) {
		TravelTime freeSpeed = new QSimFreeSpeedTravelTime(qSimTimeStepSize);
		return new LeastCostPathTree(SpeedyGraphBuilder.build(network), freeSpeed,
			new TimeAsTravelDisutility(freeSpeed));
	}

	/**
	 * The travel time of a tree to one node, with the error message a disconnected network deserves - the
	 * study area network is cleaned and checked, so a matrix over it would have thrown on the same pair.
	 */
	private static int travelTime(LeastCostPathTree tree, Node destination) {
		OptionalTime arrival = tree.getTime(tree.getNodeIndex(destination));
		if (arrival.isUndefined()) {
			throw new IllegalStateException("There is no path to node " + destination.getId() + ", so the study "
				+ "area network is not connected. Cut it with StudyAreaNetwork.cut, which checks that.");
		}
		return (int) Math.round(arrival.seconds());
	}
}
