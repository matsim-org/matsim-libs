package org.matsim.contrib.msp.traveltimematrix.experiments;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.msp.traveltimematrix.TravelTimeMatrixConfigGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

/**
 * How much memory a free-speed matrix of a given resolution will need, worked out <b>before</b> it is
 * built.
 * <p>
 * The point is to be able to refuse a configuration rather than to die in it. The matrix has two halves
 * and both grow fast: the zonal half with the square of the number of zones, the sparse half with the
 * number of node pairs inside {@code maxNeighborDistance}. On a 23,000 node study area the corner of the
 * sweep where the zones are 100 m and the neighbourhood is unbounded needs several gigabytes, and
 * finding that out after twenty minutes of routing - on configuration 28 of 31, with the results of 1 to
 * 27 in memory - is how a sweep is lost.
 * <p>
 * <b>The two estimates are not equally good.</b> The zone count is <em>exact</em>: the zone system is
 * built, which costs one pass over the nodes, and counted. Note what it implies - a zone with no node in
 * it does not exist, so the number of zones is bounded by the number of nodes however small the cells
 * get, and the zonal half can therefore never be worse than {@code nodes^2 * 2} bytes.
 * <p>
 * An <b>unbounded</b> {@code maxNeighborDistance} is a special case in both halves, and the estimate is
 * then exact: the sparse one needs no sampling, because every node is a neighbour of every node, and the
 * zonal one is reported as zero zones and zero bytes because none is built -
 * {@link org.matsim.contrib.msp.traveltimematrix.NodeToNodeTravelTimeMatrix} has no zonal half to fall
 * back to, rather than an unused one. Which is the corner where being exact matters, it being the
 * largest matrix a sweep attempts.
 * <p>
 * The sparse count is a <em>sample</em>, and deliberately an over-estimate: neighbours are counted by
 * straight-line distance over a sample of origins, while the matrix counts them by network distance,
 * which is never shorter. So the real neighbourhood is at most the estimated one and a configuration
 * this class admits will fit. The direction of the error is the whole reason to do it this way: an
 * under-estimate would hand back a budget check that passes and then runs out of heap.
 * <p>
 * <b>The trap in the sparse half</b>, and it is a large one. Every sparse row keeps a {@link java.util.BitSet}
 * to reject a non-neighbour in constant time, and that bit set is indexed by the <em>node id index</em>,
 * not by a position within the study area. Node id indices are global to the JVM and handed out as ids
 * are created, so cutting a 23,000 node study area out of a network of 450,000 nodes leaves the indices
 * spread over all 450,000 - and each row's bit set is then twenty times the size one would expect from
 * the study area alone. On Dresden that is 1.3 GB of bit sets rather than 66 MB, which is the difference
 * between fitting in a laptop's heap and not. Hence {@link #nodeIdSpace()}: the estimate has to be made
 * against the index space the matrix will actually allocate over, which is
 * {@code Id.getNumberOfIds(Node.class)} and not {@code network.getNodes().size()}.
 *
 * @see RunMatrixCostExperiment the sweep this guards
 */
public record MatrixFootprint(int zones, long zonalBytes, double meanNeighbors, long sparseBytes,
							  int nodeIdSpace) {

	private static final Logger log = LogManager.getLogger(MatrixFootprint.class);

	/**
	 * What one entry of the sparse matrix costs: {@code SparseMatrix.Bucket} keeps an {@code int} node
	 * index next to a {@code short} travel time, so six bytes per stored pair.
	 */
	private static final int BYTES_PER_SPARSE_ENTRY = Integer.BYTES + Short.BYTES;

	/**
	 * What one entry of the zonal matrix costs: {@code Matrix} stores travel times as unsigned shorts.
	 */
	private static final int BYTES_PER_ZONAL_ENTRY = Short.BYTES;

	/**
	 * How many origins to sample when counting neighbours. Each one costs a pass over all nodes, so this
	 * is a few million distance computations - milliseconds - and the mean over 200 origins is far more
	 * precision than a memory budget needs.
	 */
	private static final int DEFAULT_SAMPLED_ORIGINS = 200;

	/**
	 * What one entry of the {@code SparseRow[]} the matrix keeps costs: a reference per node id index.
	 */
	private static final int BYTES_PER_ROW_REFERENCE = 8;

	/**
	 * @return the total heap the matrix is expected to occupy once built [bytes]
	 * <p>
	 * What it does <b>not</b> include is the garbage the build makes on the way: a sparse row is
	 * collected as a list of boxed {@code NodeAndTime} records, split into buckets and sorted before it
	 * is packed into the two arrays that survive, which is about nine times the retained size of that
	 * one row. It is transient and per thread - a few megabytes - so it does not belong in a budget
	 * measured in gigabytes, but it is why a configuration estimated at exactly the heap size will not
	 * fit.
	 */
	public long totalBytes() {
		return this.zonalBytes + this.sparseBytes;
	}

	/**
	 * @see #estimate(Network, double, double, int, long)
	 */
	public static MatrixFootprint estimate(Network network, double cellSize, double maxNeighborDistance) {
		return estimate(network, cellSize, maxNeighborDistance, DEFAULT_SAMPLED_ORIGINS, 4711);
	}

	/**
	 * @param cellSize cell size of the zonal half [m]
	 * @param maxNeighborDistance radius of the sparse half [m]; may be {@link Double#POSITIVE_INFINITY},
	 * which means every pair is stored node to node
	 * @param sampledOrigins how many origins to count neighbours around
	 * @param seed of the sampling, so that a sweep and its log agree
	 */
	public static MatrixFootprint estimate(Network network, double cellSize, double maxNeighborDistance,
		int sampledOrigins, long seed) {

		int nodes = network.getNodes().size();

		// the index space the matrix allocates over, which is every node id this jvm has created and not
		// only the ones left in the study area - see the class comment
		int nodeIdSpace = Math.max(nodes, Id.getNumberOfIds(Node.class));

		// an unbounded radius has no zonal half at all - not an unused one - so there is nothing to size,
		// and no zone system has to be built in order to size it. See NodeToNodeTravelTimeMatrix.
		int zones = 0;
		if (!Double.isInfinite(maxNeighborDistance)) {
			TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
			config.setCellSize(cellSize);
			zones = ZoneSystemUtils.createZoneSystem(network, config.getMatrixParams().getZoneSystemParams())
				.getZones().size();
		}

		double meanNeighbors = meanNeighbors(network, maxNeighborDistance, sampledOrigins, seed);

		// the zonal half is a dense square of unsigned shorts
		long zonalBytes = (long) zones * zones * BYTES_PER_ZONAL_ENTRY;

		// the sparse half is three things: the packed entries of every row, the bit set each row keeps
		// over the whole node index space, and the array of rows itself
		long entries = (long) (meanNeighbors * nodes) * BYTES_PER_SPARSE_ENTRY;
		long bitSets = (long) nodes * ((nodeIdSpace + 7) / 8);
		long rowReferences = (long) nodeIdSpace * BYTES_PER_ROW_REFERENCE;

		return new MatrixFootprint(zones, zonalBytes, meanNeighbors, entries + bitSets + rowReferences,
			nodeIdSpace);
	}

	/**
	 * The mean number of nodes within {@code radius} of a node, by straight-line distance, over a sample
	 * of origins.
	 * <p>
	 * An unbounded radius short-circuits: every node is then a neighbour of every node, which needs no
	 * sampling and which sampling would get slightly wrong at the edge of the area.
	 */
	private static double meanNeighbors(Network network, double radius, int sampledOrigins, long seed) {

		int nodes = network.getNodes().size();
		if (Double.isInfinite(radius)) {
			return nodes;
		}

		List<Coord> coords = new ArrayList<>(nodes);
		network.getNodes().values().forEach(node -> coords.add(node.getCoord()));

		SplittableRandom random = new SplittableRandom(seed);
		int sample = Math.min(sampledOrigins, nodes);
		double squaredRadius = radius * radius;
		long found = 0;

		for (int origin = 0; origin < sample; origin++) {
			Coord from = coords.get(random.nextInt(nodes));
			// squared distances, inlined: this is the inner loop of a few million comparisons and a
			// square root per pair would be the only thing in it
			for (Coord to : coords) {
				double dx = from.getX() - to.getX();
				double dy = from.getY() - to.getY();
				if (dx * dx + dy * dy <= squaredRadius) {
					found++;
				}
			}
		}

		return (double) found / sample;
	}

	/**
	 * @return whether this footprint fits in a budget, logging what it found either way
	 */
	public boolean fitsIn(long budgetBytes, double cellSize, double maxNeighborDistance) {

		boolean fits = totalBytes() <= budgetBytes;
		String message = Double.isInfinite(maxNeighborDistance)
			? String.format(Locale.ROOT,
			"maxNeighborDistance unbounded: no zonal half, sparse %s (every one of the %.0f nodes is a neighbour "
				+ "of every other, over a node index space of %d), total %s against a budget of %s.",
			megabytes(this.sparseBytes), this.meanNeighbors, this.nodeIdSpace, megabytes(totalBytes()),
			megabytes(budgetBytes))
			: String.format(Locale.ROOT,
			"cellSize %.0f m, maxNeighborDistance %.0f m: %d zones, zonal %s, sparse ~%s (mean %.0f neighbours "
				+ "over a node index space of %d), total ~%s against a budget of %s.",
			cellSize, maxNeighborDistance, this.zones, megabytes(this.zonalBytes), megabytes(this.sparseBytes),
			this.meanNeighbors, this.nodeIdSpace, megabytes(totalBytes()), megabytes(budgetBytes));

		if (fits) {
			log.info(message);
		} else {
			log.warn("Skipping {} The estimate is an upper bound - neighbours are counted by straight-line "
				+ "distance - so raise -Xmx or --memory-budget if you want it attempted anyway.", message);
		}
		return fits;
	}

	/**
	 * How much heap a sweep is allowed to spend on one matrix.
	 * <p>
	 * <b>The smaller of what was asked for and what the JVM has</b>, and both halves matter. The
	 * explicit number is the one a study fixes so that its rows mean the same thing on a laptop and on a
	 * cluster node - {@value #DEFAULT_BUDGET_MEGABYTES} MB by default, which is the size of the machines
	 * these sweeps are run on and therefore the wall the paper reports. The heap share is what keeps that
	 * number from being a lie: a 64 GB budget on a JVM started with {@code -Xmx8g} would let every
	 * configuration through and then die of an {@code OutOfMemoryError} in the middle of the sweep, which
	 * is the one outcome a budget exists to prevent. The share is {@value #HEAP_SHARE} rather than 1,
	 * because the network, the routing graph, the samples and the reference are not small either.
	 *
	 * @param requestedMegabytes {@code --memory-budget}, or null for the default
	 */
	public static long budget(@Nullable Long requestedMegabytes) {

		long requested = (requestedMegabytes == null ? DEFAULT_BUDGET_MEGABYTES : requestedMegabytes)
			* 1024 * 1024;
		long heap = (long) (HEAP_SHARE * Runtime.getRuntime().maxMemory());

		if (heap < requested) {
			log.warn("The memory budget is {} rather than the {} asked for: this JVM's -Xmx is {}, and {}% of it "
					+ "is all that can be spent on a matrix without the rest of the sweep running out.",
				megabytes(heap), megabytes(requested), megabytes(Runtime.getRuntime().maxMemory()),
				(int) (100 * HEAP_SHARE));
			return heap;
		}
		return requested;
	}

	/**
	 * The default {@code --memory-budget} [MB]: a 64 GB machine, which is the node these sweeps are sized
	 * for. Deliberately a fixed number and not a share of the heap, so that the point at which a
	 * configuration stops fitting is a property of the study and not of the machine it happened to run
	 * on - that point is a result.
	 */
	public static final long DEFAULT_BUDGET_MEGABYTES = 64 * 1024;

	/**
	 * How much of {@code -Xmx} a single matrix may take when the heap is the binding constraint.
	 */
	private static final double HEAP_SHARE = 0.6;

	static String megabytes(long bytes) {
		return bytes < 1024L * 1024 * 1024
			? String.format(Locale.ROOT, "%d MB", bytes / 1024 / 1024)
			: String.format(Locale.ROOT, "%.1f GB", bytes / 1024.0 / 1024 / 1024);
	}
}
