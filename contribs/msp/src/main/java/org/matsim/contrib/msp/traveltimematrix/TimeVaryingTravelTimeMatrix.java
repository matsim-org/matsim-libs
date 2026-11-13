package org.matsim.contrib.msp.traveltimematrix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndex;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.contrib.zone.skims.FreeSpeedTravelTimeMatrix;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Locale;

/**
 * A time-varying car travel time matrix: a free-speed matrix, scaled by a congestion index that depends
 * on the time of day.
 * <pre>
 *   TT(o, d, t) = TT_ff(o, d) * (1 + beta * CI(t))                               level 0
 * </pre>
 * The point is what it does <b>not</b> store: there is one travel time per origin-destination pair, not
 * one per pair and time bin, so the time of day costs a lookup in a curve of a few dozen numbers and a
 * multiplication. A matrix per 15 minute bin would be 96 times the memory and 96 times the routing.
 * <p>
 * It is a decorator over any {@link TravelTimeMatrix} of {@code contribs/dvrp} and implements that
 * interface itself, whose {@code getTravelTime} already takes a departure time. So it drops into
 * everything that uses a matrix today - DVRP, DRT's
 * {@code DetourTimeEstimator.createMatrixBasedEstimator}, an accessibility or capability analysis -
 * without any of them knowing that the time dimension is now real.
 * <p>
 * <b>What it is not.</b> The congestion index is an aggregate over the network and the fleet, so this is
 * a model of <em>when</em> traffic is slow, not of <em>where</em>. Two pairs of the same free-speed
 * travel time get the same congested travel time, however differently they are actually affected. Nor
 * does the path change: the free-speed path is held fixed and slowed down, while a real driver would route around
 * the congestion, so the matrix is biased <b>upwards</b> under heavy congestion. Both are the price of
 * the one lookup, and both are what an evaluation against time-dependent routing has to quantify.
 *
 * @see #global(TravelTimeMatrix, CongestionIndex, TravelTimeMatrixConfigGroup) level 0
 * @see #create(Network, TravelTimeMatrixConfigGroup, double) to build everything from a config
 */
public final class TimeVaryingTravelTimeMatrix implements TravelTimeMatrix {

	private static final Logger log = LogManager.getLogger(TimeVaryingTravelTimeMatrix.class);

	private final TravelTimeMatrix freeSpeedMatrix;
	private final CongestionIndex congestionIndex;

	private final double calibrationFactor;
	private final boolean evaluateAtMidpoint;

	private TimeVaryingTravelTimeMatrix(TravelTimeMatrix freeSpeedMatrix, CongestionIndex congestionIndex,
		double calibrationFactor, boolean evaluateAtMidpoint) {

		this.freeSpeedMatrix = freeSpeedMatrix;
		this.congestionIndex = congestionIndex;
		this.calibrationFactor = calibrationFactor;
		this.evaluateAtMidpoint = evaluateAtMidpoint;
	}

	/**
	 * Level 0: one congestion index for the whole network.
	 *
	 * @param freeSpeedMatrix the base matrix. Its travel times have to be free-speed travel times - the
	 * congestion index is measured against free speed, so wrapping an already congested matrix would
	 * count the congestion twice.
	 */
	public static TimeVaryingTravelTimeMatrix global(TravelTimeMatrix freeSpeedMatrix,
		CongestionIndex congestionIndex, TravelTimeMatrixConfigGroup config) {
		return new TimeVaryingTravelTimeMatrix(freeSpeedMatrix, congestionIndex, config.getCalibrationFactor(),
			config.getEvaluateIndexAt() == TravelTimeMatrixConfigGroup.EvaluateIndexAt.MIDPOINT);
	}

	/**
	 * Builds the whole thing from a config and a network: the free-speed base matrix (from the cache file
	 * if the config names one that exists) and the congestion index from the file the config names.
	 *
	 * @param network the car subnetwork of the study area, see
	 * {@link org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaNetwork}. Has to be the same
	 * network the congestion index was measured over, and has to be connected - a zonal matrix refuses a
	 * pair it finds no path for.
	 * @param qSimTimeStepSize {@code config.qsim().getTimeStepSize()} of the run
	 * @see #createWithoutZones what an unbounded {@code maxNeighborDistance} builds instead
	 */
	public static TimeVaryingTravelTimeMatrix create(Network network, TravelTimeMatrixConfigGroup config,
		double qSimTimeStepSize) {

		if (config.isUnboundedNeighborhood()) {
			return createWithoutZones(network, config, qSimTimeStepSize);
		}

		DvrpTravelTimeMatrixParams params = config.getMatrixParams();
		ZoneSystem zoneSystem = ZoneSystemUtils.createZoneSystem(network, params.getZoneSystemParams());

		log.info("Building the free-speed base matrix over {} links, {} nodes and {} zones.",
			network.getLinks().size(), network.getNodes().size(), zoneSystem.getZones().size());

		FreeSpeedTravelTimeMatrix freeSpeedMatrix = params.getCachePath() == null
			? FreeSpeedTravelTimeMatrix.createFreeSpeedMatrix(network, zoneSystem, params, config.getNumberOfThreads(),
			qSimTimeStepSize)
			: FreeSpeedTravelTimeMatrix.createFreeSpeedMatrixFromCache(network, zoneSystem, params,
			config.getNumberOfThreads(), qSimTimeStepSize, cacheUrl(params.getCachePath()));

		return global(freeSpeedMatrix, config.getCongestionIndex(), config);
	}

	/**
	 * The exact variant: one {@link NodeToNodeTravelTimeMatrix} and <b>no zone system whatsoever</b>.
	 * <p>
	 * This is the whole point of {@link TravelTimeMatrixConfigGroup#isUnboundedNeighborhood()} being a
	 * configuration rather than a large number. Asking {@code contribs/dvrp} for an unbounded radius does
	 * produce a matrix that answers every pair node to node, but it routes a complete zone-to-zone matrix
	 * first and then never reads it, because {@code FreeSpeedTravelTimeMatrix.getTravelTime} consults the
	 * sparse half first and returns as soon as the pair is present - which, with no radius, is always. So
	 * the zone system here is not cheap-but-unused, it is a second full matrix build thrown away, and the
	 * cell size that sizes it cannot move a single answer. Here it is not built, and
	 * {@link TravelTimeMatrixConfigGroup#getCellSize()} is ignored.
	 * <p>
	 * It is the configuration the congestion model is meant to be judged in: with the discretisation
	 * error zero by construction, whatever error is left is the congestion model's own.
	 */
	private static TimeVaryingTravelTimeMatrix createWithoutZones(Network network,
		TravelTimeMatrixConfigGroup config, double qSimTimeStepSize) {

		log.info("maxNeighborDistance is unbounded, so the base matrix is exact and no zone system is built; "
			+ "cellSize ({} m) is ignored.", (long) config.getCellSize());

		NodeToNodeTravelTimeMatrix freeSpeedMatrix = NodeToNodeTravelTimeMatrix.create(network,
			config.getNumberOfThreads(), qSimTimeStepSize);

		return global(freeSpeedMatrix, config.getCongestionIndex(), config);
	}

	/**
	 * The cache file as a URL, <b>without requiring it to exist</b>: the matrix writes the cache when it
	 * is not there yet, so the usual {@code IOUtils.resolveFileOrResource} - which refuses a missing
	 * file - cannot be used here. The cache is therefore always a file path, never a classpath resource.
	 */
	private static URL cacheUrl(String path) {
		try {
			return Path.of(path).toUri().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("'" + path + "' is not a usable path for the matrix cache file.", e);
		}
	}

	@Override
	public int getTravelTime(Node fromNode, Node toNode, double departureTime) {
		int freeSpeedTravelTime = this.freeSpeedMatrix.getTravelTime(fromNode, toNode, departureTime);
		if (freeSpeedTravelTime == 0) {
			return 0;
		}
		double factor = congestionFactor(fromNode, toNode, departureTime, freeSpeedTravelTime);
		return (int) Math.round(freeSpeedTravelTime * factor);
	}

	/**
	 * The free-speed travel time the congested one is derived from, i.e. the base matrix unchanged.
	 * Exposed because an evaluation needs both numbers of the same query, and asking the base matrix
	 * separately would mean keeping a reference to it alongside this one.
	 */
	public int getFreeSpeedTravelTime(Node fromNode, Node toNode) {
		return this.freeSpeedMatrix.getTravelTime(fromNode, toNode, 0);
	}

	/**
	 * The factor the free-speed travel time is multiplied by, i.e. {@code 1 + beta * CI}. Exposed for the
	 * same reason as {@link #getFreeSpeedTravelTime}: it is the quantity an experiment reports and plots.
	 *
	 * @param freeSpeedTravelTime the free-speed travel time of the pair [s], which decides where in the
	 * trip the index is read when {@link TravelTimeMatrixConfigGroup.EvaluateIndexAt#MIDPOINT} is set
	 */
	public double congestionFactor(Node fromNode, Node toNode, double departureTime, double freeSpeedTravelTime) {

		// the middle of the trip rather than its start, because a trip is mostly spent somewhere other
		// than at its origin. Free-speed rather than congested travel time, which would make this
		// recursive - the difference is second order and the bin is 15 minutes wide.
		double at = this.evaluateAtMidpoint ? departureTime + freeSpeedTravelTime / 2 : departureTime;

		return 1 + this.calibrationFactor * this.congestionIndex.get(at);
	}

	/**
	 * The free-speed base matrix this one scales, unchanged.
	 * <p>
	 * Exposed so that several variants can share <b>one</b> base matrix: they differ in the congestion
	 * model only, and computing the base matrix once per variant would buy nothing but the chance of it
	 * coming out differently. Note that what comes back is free-speed travel times, so it is the right
	 * argument for {@link #global} - and wrapping a matrix that already carries congestion is exactly
	 * what that method refuses to be given.
	 */
	public TravelTimeMatrix getFreeSpeedMatrix() {
		return this.freeSpeedMatrix;
	}

	/**
	 * @return the congestion index this matrix reads, e.g. to write it out next to the results it
	 * produced
	 */
	public CongestionIndex getCongestionIndex() {
		return this.congestionIndex;
	}

	@Override
	public String toString() {
		String bins = this.congestionIndex instanceof CongestionIndexProfile profile
			? profile.getNumberOfBins() + " bins of " + profile.getTimeBinSize() + " s"
			: this.congestionIndex.toString();
		return String.format(Locale.ROOT, "TimeVaryingTravelTimeMatrix[one index for the whole network, %s, "
				+ "beta = %.3f, index read at %s]", bins, this.calibrationFactor,
			this.evaluateAtMidpoint ? "the middle of the trip" : "departure");
	}
}
