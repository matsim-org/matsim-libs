package org.matsim.contrib.msp.traveltimematrix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.zone.skims.SparseMatrix;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.contrib.zone.skims.TravelTimeMatrices;
import org.matsim.core.router.util.TravelTime;

import java.util.Locale;

/**
 * The free-speed travel time of <b>every</b> node pair of the network, stored node to node, with no zone
 * system at all.
 * <p>
 * This is the exact end of the resolution ladder, and the reason it exists as its own class is that
 * {@code FreeSpeedTravelTimeMatrix} of {@code contribs/dvrp} cannot express it: that class always builds
 * the zonal half, and consults the sparse half only as a fast path in front of it. Give it an unbounded
 * {@code maxNeighborDistance} and it does build a matrix that answers every pair node to node - but it
 * first routes a full zone-to-zone matrix that, by construction, no query can ever reach. On Dresden that
 * is about a minute of routing thrown away - the zonal <em>memory</em> is the small half, 0.3 MB at a 2 km
 * cell and half a gigabyte at a 100 m one. Here there are no zones to build, no cell size to choose, and
 * nothing to fall back to.
 * <p>
 * <b>What it costs.</b> One shortest-path tree per node, fully relaxed, and a row of every other node:
 * {@code n^2} entries of six bytes plus a {@link java.util.BitSet} per row. The bit sets are indexed by
 * the <em>global</em> node id index rather than by a position in this network, so cutting a study area
 * out of a larger network makes them much bigger than the study area suggests - see
 * {@link org.matsim.contrib.msp.traveltimematrix.experiments.MatrixFootprint}, which estimates all of
 * this before anything is built. On the 23,000 node Dresden study area it is about 4.7 GB and under a
 * minute to build.
 * <p>
 * <b>Why it is the interesting configuration and not merely the expensive one.</b> It takes the
 * discretisation error of the zone system - 5 to 8 % MAPE at a 500 m to 1 km cell on Dresden, and all of
 * it scatter rather than bias, so no calibration factor can remove it - down to exactly zero. What is
 * then left of the error of a {@link TimeVaryingTravelTimeMatrix} over it is the congestion
 * approximation alone, which is the quantity the method is actually about.
 *
 * @see TimeVaryingTravelTimeMatrix#create(Network, TravelTimeMatrixConfigGroup, double) builds this when
 * the configured neighbourhood is unbounded
 */
public final class NodeToNodeTravelTimeMatrix implements TravelTimeMatrix {

	private static final Logger log = LogManager.getLogger(NodeToNodeTravelTimeMatrix.class);

	/**
	 * Any radius past the diameter of the network means "never stop relaxing", and the sparse builder
	 * compares a node's accumulated distance against it. Passing {@link Double#POSITIVE_INFINITY}
	 * straight through would work for the comparison, but this keeps the arithmetic finite.
	 */
	private static final double NO_RADIUS = Double.MAX_VALUE;

	/**
	 * The largest travel time the sparse matrix can carry. It stores travel times as a {@code short} and
	 * reads them back signed, so anything above this comes back negative - which is also how it says
	 * "this pair is not in the row". See {@link #getTravelTime}: the two cases are indistinguishable
	 * from the outside, which is why both end in the same exception rather than in a wrong number.
	 */
	private static final int MAX_STORABLE_TRAVEL_TIME = Short.MAX_VALUE;

	private final SparseMatrix matrix;

	private NodeToNodeTravelTimeMatrix(SparseMatrix matrix) {
		this.matrix = matrix;
	}

	/**
	 * Routes the whole thing: one fully relaxed tree per node of the network.
	 *
	 * @param network the car subnetwork of the study area, see
	 * {@link org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaNetwork}. Has to be connected -
	 * a pair with no path between it has no entry, and {@link #getTravelTime} then throws rather than
	 * guessing.
	 * @param qSimTimeStepSize {@code config.qsim().getTimeStepSize()} of the run, so that the free-speed
	 * times are rounded exactly as the zonal matrix rounds them and the two stay comparable
	 */
	public static NodeToNodeTravelTimeMatrix create(Network network, int numberOfThreads,
		double qSimTimeStepSize) {
		return create(network, numberOfThreads, new QSimFreeSpeedTravelTime(qSimTimeStepSize));
	}

	/**
	 * @param travelTime what the matrix is routed on. Has to be a <b>free-speed</b> travel time if the
	 * result is to be scaled by a congestion index: the index is measured against free speed, so a
	 * matrix that already carries congestion would count it twice.
	 */
	public static NodeToNodeTravelTimeMatrix create(Network network, int numberOfThreads,
		TravelTime travelTime) {

		int nodes = network.getNodes().size();
		log.info("Building the exact node-to-node free-speed matrix over {} links and {} nodes ({} pairs). "
				+ "No zone system is built: every pair is stored node to node, so there is no zonal half for a "
				+ "cell size to describe.",
			network.getLinks().size(), nodes, (long) nodes * nodes);

		TravelTimeMatrices.RoutingParams routingParams = new TravelTimeMatrices.RoutingParams(network, travelTime,
			new TimeAsTravelDisutility(travelTime), numberOfThreads);

		// the sparse builder stops relaxing a node once its accumulated distance reaches the radius; no
		// radius it can reach means every tree is relaxed in full
		SparseMatrix matrix = TravelTimeMatrices.calculateTravelTimeSparseMatrix(routingParams, NO_RADIUS, 0, 0)
			.orElseThrow(() -> new IllegalStateException("The sparse matrix builder returned nothing for an "
				+ "unbounded neighbourhood, which it only does when both thresholds are zero."));

		return new NodeToNodeTravelTimeMatrix(matrix);
	}

	/**
	 * @param departureTime ignored: these are free-speed travel times, which do not depend on the time of
	 * day. The parameter is part of {@link TravelTimeMatrix} and a
	 * {@link TimeVaryingTravelTimeMatrix} wrapping this one is what gives it meaning.
	 * @throws IllegalArgumentException if the pair has no entry. With no zonal half there is nothing to
	 * fall back on, and the two ways that happens are both worth hearing about rather than papering
	 * over - see {@link #MAX_STORABLE_TRAVEL_TIME}.
	 */
	@Override
	public int getTravelTime(Node fromNode, Node toNode, double departureTime) {

		if (fromNode == toNode) {
			return 0;
		}

		int travelTime = this.matrix.get(fromNode, toNode);
		if (travelTime < 0) {
			throw new IllegalArgumentException(String.format(Locale.ROOT,
				"The node-to-node matrix has no free-speed travel time from node %s to node %s. Either there is no "
					+ "path between them - the matrix stores every pair it can reach, so an unreachable pair means "
					+ "the network is not connected, which StudyAreaNetwork.verifyRoutable is there to catch - or "
					+ "the travel time is longer than the %d s the matrix can store and has wrapped around. A zonal "
					+ "matrix would quietly answer such a pair from its zone-to-zone half; this one has none.",
				fromNode.getId(), toNode.getId(), MAX_STORABLE_TRAVEL_TIME));
		}
		return travelTime;
	}

	@Override
	public String toString() {
		return "NodeToNodeTravelTimeMatrix[exact, every pair stored node to node, no zone system]";
	}
}
