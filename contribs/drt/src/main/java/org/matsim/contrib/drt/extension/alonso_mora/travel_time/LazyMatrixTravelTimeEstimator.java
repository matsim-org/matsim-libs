package org.matsim.contrib.drt.extension.alonso_mora.travel_time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;

/**
 * Matrix-based travel time estimator as used in the paper by Alonso-Mora et al.
 * When initialized, it takes all links that are part of the dispatching network
 * and calculates the travel times between all links at a chosen departure time.
 * If the network is not time-varying and no congestion is simulated, this means
 * that the exact travel time is reproduced.
 * 
 * This is a lazy version of the estimator which does not calculate all
 * node-to-node travel times at the beginning of the simulation, but which
 * calculates travel times when they are first requested and later reuses the
 * values.
 * 
 * @author sebhoerl
 */
public class LazyMatrixTravelTimeEstimator implements TravelTimeEstimator {
	static public final String TYPE = "LazyMatrix";
	
	private final List<Integer> id2matrix;
	private final double[][] matrix;

	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;
	private final double matrixDepartureTime;

	LazyMatrixTravelTimeEstimator(List<Integer> id2matrix, double[][] matrix, TravelTime travelTime,
			LeastCostPathCalculator router, double matrixDepartureTime) {
		this.matrix = matrix;
		this.id2matrix = id2matrix;
		this.travelTime = travelTime;
		this.router = router;
		this.matrixDepartureTime = matrixDepartureTime;
	}

	@Override
	public double estimateTravelTime(Link fromLink, Link toLink, double departureTime, double arrivalTimeThreshold) {
		if (fromLink == toLink) {
			return 0.0;
		}

		int originIndex = id2matrix.get(fromLink.getToNode().getId().index());
		int destinationIndex = id2matrix.get(toLink.getFromNode().getId().index());

		double computedTravelTime = matrix[originIndex][destinationIndex];

		if (Double.isNaN(computedTravelTime)) {
			Path path = router.calcLeastCostPath(fromLink.getToNode(), toLink.getFromNode(), matrixDepartureTime, null,
					null);

			computedTravelTime = path.travelTime;
			matrix[originIndex][destinationIndex] = computedTravelTime;
		}

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

	static public LazyMatrixTravelTimeEstimator create(Network network, TravelTime travelTime,
			LeastCostPathCalculatorFactory routerFactory, double departureTime) {
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

		for (int i = 0; i < nodes.size(); i++) {
			for (int j = 0; j < nodes.size(); j++) {
				travelTimes[i][j] = Double.NaN;
			}
		}

		LeastCostPathCalculator router = routerFactory.createPathCalculator(network,
				new OnlyTimeDependentTravelDisutility(travelTime), travelTime);

		return new LazyMatrixTravelTimeEstimator(id2matrix, travelTimes, travelTime, router, departureTime);
	}
}
