package org.matsim.contrib.drt.extension.alonso_mora.travel_time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

/**
 * Matrix-based travel time estimator as used in the paper by Alonso-Mora et al.
 * When initialized, it takes all links that are part of the dispatching network
 * and calculates the travel times between all links at a chosen departure time.
 * If the network is not time-varying and no congestion is simulated, this means
 * that the exact travel time is reproduced.
 * 
 * @author sebhoerl
 */
public class MatrixTravelTimeEstimator implements TravelTimeEstimator {
	static public final String TYPE = "Matrix";
	
	private final List<Integer> id2matrix;
	private final double[][] matrix;

	private final TravelTime travelTime;

	MatrixTravelTimeEstimator(List<Integer> id2matrix, double[][] matrix, TravelTime travelTime) {
		this.matrix = matrix;
		this.id2matrix = id2matrix;
		this.travelTime = travelTime;
	}

	@Override
	public double estimateTravelTime(Link fromLink, Link toLink, double departureTime, double arrivalTimeThreshold) {
		if (fromLink == toLink) {
			return 0.0;
		}

		// Travel time is calculated from "to node" of the origin link and to the "from
		// node" of the destination link
		int originIndex = id2matrix.get(fromLink.getToNode().getId().index());
		int destinationIndex = id2matrix.get(toLink.getFromNode().getId().index());

		double computedTravelTime = matrix[originIndex][destinationIndex];

		// We need to add a delay for entering the first link on the route
		computedTravelTime += VrpPaths.FIRST_LINK_TT;

		// We need to add the traversal time for the destination link
		computedTravelTime += travelTime.getLinkTravelTime(toLink, departureTime + computedTravelTime, null, null);

		// Finally, we need to deduct some delay because we do not need to travel over
		// the final node
		computedTravelTime -= VrpPaths.NODE_TRANSITION_TIME;

		// Note that this gives us the travel time for a route where we depart from idle
		// (i.e. we need to enter the first link)!
		return computedTravelTime;
	}

	static public MatrixTravelTimeEstimator create(Network network, TravelTime travelTime, double departureTime) {
		List<Node> nodes = new ArrayList<>(network.getNodes().values());

		/*
		 * Create index matrix for rapid look-up
		 */

		int maximumIndex = nodes.stream().mapToInt(n -> n.getId().index()).max().getAsInt();
		List<Integer> id2matrix = new ArrayList<>(Collections.nCopies(maximumIndex + 1, -1));

		for (int index = 0; index < nodes.size(); index++) {
			id2matrix.set(nodes.get(index).getId().index(), index);
		}

		/*
		 * Perform calculation of travel time
		 */

		double[][] travelTimes = new double[nodes.size()][nodes.size()];

		Counter counter = new Counter("Calculating travel time matrix ", " of " + nodes.size());

		for (Node originNode : nodes) {
			LeastCostPathTree tree = new LeastCostPathTree(travelTime,
					new OnlyTimeDependentTravelDisutility(travelTime));
			tree.calculate(network, originNode, departureTime);

			for (var entry : tree.getTree().entrySet()) {
				double calculatedTravelTime = entry.getValue().getTime() - departureTime;
				Node destinationNode = network.getNodes().get(entry.getKey());

				int originIndex = id2matrix.get(originNode.getId().index());
				int destinationIndex = id2matrix.get(destinationNode.getId().index());

				travelTimes[originIndex][destinationIndex] = calculatedTravelTime;
			}

			counter.incCounter();
		}

		return new MatrixTravelTimeEstimator(id2matrix, travelTimes, travelTime);
	}
}
