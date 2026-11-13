package org.matsim.contrib.msp.traveltimematrix;

import picocli.CommandLine;

/**
 * The options that decide the <b>shape</b> of the free-speed base matrix, and nothing else: how coarse
 * the zone grid is, how far the exact node-to-node part reaches, and what it is routed with.
 * <p>
 * A {@link CommandLine.Mixin}, because these four are repeated by every command that builds a matrix
 * and because they are exactly the parameters an experiment sweeps - keeping them in one place means a
 * sweep and a one-off build cannot drift apart in what they mean by {@code cellSize}.
 * <p>
 * Deliberately <b>not</b> here: the congestion model (variant, index file, calibration factor) and the
 * cache directory. Those differ per command - the resolution experiment has no congestion at all - and
 * folding them in would make this a second copy of
 * {@link TravelTimeMatrixConfigGroup} rather than a way to fill one in. How it routes is
 * {@link RoutingOptions}, mixed in here so that a command that sweeps the thresholds can take that half
 * on its own.
 */
public final class MatrixOptions {

	/**
	 * How an unbounded neighbourhood is written on the command line and in the result tables. Spelled out
	 * rather than left to {@code Double.parseDouble}, which wants {@code Infinity} and refuses this.
	 */
	public static final String UNBOUNDED = "inf";

	@CommandLine.Option(names = "--cell-size", description = "Cell size of the zonal part [m]. Memory grows with "
		+ "the square of the number of zones, which is bounded by the number of nodes. Ignored when "
		+ "--max-neighbor-distance is '" + UNBOUNDED + "'.", defaultValue = "1000")
	private double cellSize;

	@CommandLine.Option(names = "--max-neighbor-distance", description = "Pairs closer than this are answered "
		+ "exactly, node to node, instead of zone to zone [m]. Memory grows with the number of node pairs "
		+ "within it. Give '" + UNBOUNDED + "' for no bound at all: every pair is then stored node to node, the "
		+ "matrix is exact, and --cell-size is ignored because no zone system is built.", defaultValue = "5000")
	private String maxNeighborDistance;

	@CommandLine.Option(names = "--max-neighbor-travel-time", description = "The same threshold as a travel time "
		+ "[s]; 0 switches it off. A pair within either threshold is answered node to node.",
		defaultValue = "0")
	private double maxNeighborTravelTime;

	@CommandLine.Mixin
	private RoutingOptions routing = new RoutingOptions();

	/**
	 * Fills the shape parameters into a config, leaving everything else as the caller set it.
	 *
	 * @return the same config, so this can be chained onto a constructor call
	 */
	public TravelTimeMatrixConfigGroup applyTo(TravelTimeMatrixConfigGroup config) {
		config.setCellSize(this.cellSize);
		config.setMaxNeighborDistance(getMaxNeighborDistance());
		config.setMaxNeighborTravelTime(this.maxNeighborTravelTime);
		config.setNumberOfThreads(getNumberOfThreads());
		return config;
	}

	public double getCellSize() {
		return this.cellSize;
	}

	public double getMaxNeighborDistance() {
		return parseNeighborDistance(this.maxNeighborDistance);
	}

	/**
	 * Whether no bound was asked for, i.e. whether {@code --max-neighbor-distance} is
	 * {@value #UNBOUNDED}. A command that reports its configuration wants this rather than the infinity
	 * itself, which does not print as anything a reader of a csv wants to see.
	 */
	public boolean isUnbounded() {
		return Double.isInfinite(getMaxNeighborDistance());
	}

	/**
	 * A neighbourhood radius as written on a command line: a number of metres, or {@value #UNBOUNDED}
	 * (also {@code infinity}, in any case) for no bound.
	 * <p>
	 * Shared with {@link org.matsim.contrib.msp.traveltimematrix.experiments.RunMatrixResolutionExperiment},
	 * which sweeps a list of these, so that a sweep and a single build cannot disagree about what
	 * {@value #UNBOUNDED} means.
	 *
	 * @throws IllegalArgumentException if it is neither
	 */
	public static double parseNeighborDistance(String given) {
		String trimmed = given.trim();
		if (trimmed.equalsIgnoreCase(UNBOUNDED) || trimmed.equalsIgnoreCase("infinity")) {
			return Double.POSITIVE_INFINITY;
		}
		try {
			return Double.parseDouble(trimmed);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("'" + given + "' is neither a neighbourhood radius in metres nor '"
				+ UNBOUNDED + "', which is how no bound at all is written.", e);
		}
	}

	public double getMaxNeighborTravelTime() {
		return this.maxNeighborTravelTime;
	}

	public double getQSimTimeStepSize() {
		return this.routing.getQSimTimeStepSize();
	}

	public int getNumberOfThreads() {
		return this.routing.getNumberOfThreads();
	}
}
