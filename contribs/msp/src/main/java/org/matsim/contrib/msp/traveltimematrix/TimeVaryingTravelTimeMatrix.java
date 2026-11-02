package org.matsim.contrib.msp.traveltimematrix;

import jakarta.annotation.Nullable;
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
import java.util.List;
import java.util.Locale;

/**
 * A time-varying car travel time matrix: a free-speed matrix, scaled by a congestion index that depends
 * on the time of day.
 * <pre>
 *   TT(o, d, t) = TT_ff(o, d) * (1 + beta * CI(t))                               level 0
 *   TT(o, d, t) = TT_ff(o, d) * (1 + beta * sum_k s_k(o,d) * CI_k(t))            level 1
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
 * travel time get the same congested travel time at level 0, however differently they are actually
 * affected; level 1 separates them by the kind of road they use, and not by anything else. Nor does the
 * path change: the free-speed path is held fixed and slowed down, while a real driver would route around
 * the congestion, so the matrix is biased <b>upwards</b> under heavy congestion. Both are the price of
 * the one lookup, and both are what an evaluation against time-dependent routing has to quantify.
 *
 * @see #global(TravelTimeMatrix, CongestionIndex, TravelTimeMatrixConfigGroup) level 0
 * @see #byRoadClass(TravelTimeMatrix, CongestionIndex, RoadClassShares, TravelTimeMatrixConfigGroup) level 1
 * @see #create(Network, TravelTimeMatrixConfigGroup, double) to build everything from a config
 */
public final class TimeVaryingTravelTimeMatrix implements TravelTimeMatrix {

	private static final Logger log = LogManager.getLogger(TimeVaryingTravelTimeMatrix.class);

	private final TravelTimeMatrix freeSpeedMatrix;
	private final CongestionIndex congestionIndex;

	/**
	 * Null for level 0: the index does not depend on the pair, so there is nothing to weigh.
	 */
	@Nullable
	private final RoadClassShares roadClassShares;

	/**
	 * The index per road class at one point in time, reused across queries to keep the inner loop free of
	 * allocations. Confined to one thread; see {@link #congestionFactorByRoadClass}.
	 */
	private final ThreadLocal<double[]> indexBuffer;

	private final double calibrationFactor;
	private final boolean evaluateAtMidpoint;

	private TimeVaryingTravelTimeMatrix(TravelTimeMatrix freeSpeedMatrix, CongestionIndex congestionIndex,
		@Nullable RoadClassShares roadClassShares, double calibrationFactor, boolean evaluateAtMidpoint) {

		this.freeSpeedMatrix = freeSpeedMatrix;
		this.congestionIndex = congestionIndex;
		this.roadClassShares = roadClassShares;
		this.calibrationFactor = calibrationFactor;
		this.evaluateAtMidpoint = evaluateAtMidpoint;

		int numberOfClasses = roadClassShares == null ? 0 : roadClassShares.getRoadClasses().size();
		this.indexBuffer = ThreadLocal.withInitial(() -> new double[numberOfClasses]);
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
		return new TimeVaryingTravelTimeMatrix(freeSpeedMatrix, congestionIndex, null, config.getCalibrationFactor(),
			config.getEvaluateIndexAt() == TravelTimeMatrixConfigGroup.EvaluateIndexAt.MIDPOINT);
	}

	/**
	 * Level 1: one index per road class, weighted by the road class shares of the origin-destination
	 * pair.
	 *
	 * @throws IllegalArgumentException if the index does not hold every class the shares are indexed by,
	 * which would mean the two were computed over different classifications
	 */
	public static TimeVaryingTravelTimeMatrix byRoadClass(TravelTimeMatrix freeSpeedMatrix,
		CongestionIndex congestionIndex, RoadClassShares roadClassShares, TravelTimeMatrixConfigGroup config) {

		List<String> missing = roadClassShares.getRoadClasses().stream()
			.filter(roadClass -> !congestionIndex.getRoadClasses().contains(roadClass))
			.toList();
		if (!missing.isEmpty()) {
			throw new IllegalArgumentException("The congestion index holds " + congestionIndex.getRoadClasses()
				+ " but the road class shares are split by " + roadClassShares.getRoadClasses() + ", so the index of "
				+ missing + " is missing. The index and the shares have to be built over the same road "
				+ "classification.");
		}

		return new TimeVaryingTravelTimeMatrix(freeSpeedMatrix, congestionIndex, roadClassShares,
			config.getCalibrationFactor(),
			config.getEvaluateIndexAt() == TravelTimeMatrixConfigGroup.EvaluateIndexAt.MIDPOINT);
	}

	/**
	 * Builds the whole thing from a config and a network: the free-speed base matrix (from the cache file
	 * if the config names one that exists), the congestion index from the file the config names, and the
	 * road class shares if the variant needs them.
	 *
	 * @param network the car subnetwork of the study area, see
	 * {@link org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaNetwork}. Has to be the same
	 * network the congestion index was measured over, and has to be connected - a zonal matrix refuses a
	 * pair it finds no path for.
	 * @param qSimTimeStepSize {@code config.qsim().getTimeStepSize()} of the run
	 */
	public static TimeVaryingTravelTimeMatrix create(Network network, TravelTimeMatrixConfigGroup config,
		double qSimTimeStepSize) {

		DvrpTravelTimeMatrixParams params = config.getMatrixParams();
		ZoneSystem zoneSystem = ZoneSystemUtils.createZoneSystem(network, params.getZoneSystemParams());

		log.info("Building the free-speed base matrix over {} links, {} nodes and {} zones.",
			network.getLinks().size(), network.getNodes().size(), zoneSystem.getZones().size());

		FreeSpeedTravelTimeMatrix freeSpeedMatrix = params.getCachePath() == null
			? FreeSpeedTravelTimeMatrix.createFreeSpeedMatrix(network, zoneSystem, params, config.getNumberOfThreads(),
			qSimTimeStepSize)
			: FreeSpeedTravelTimeMatrix.createFreeSpeedMatrixFromCache(network, zoneSystem, params,
			config.getNumberOfThreads(), qSimTimeStepSize, cacheUrl(params.getCachePath()));

		CongestionIndex congestionIndex = config.getCongestionIndex();

		return switch (config.getVariant()) {
			case FREE_SPEED, GLOBAL_INDEX -> global(freeSpeedMatrix, congestionIndex, config);
			case BY_ROAD_CLASS -> {
				RoadClassification roadClasses = config.getRoadClassification(network);
				RoadClassShares shares = RoadClassSharesCalculator.calculateFreeSpeedShares(network, zoneSystem,
					roadClasses, config.getNumberOfThreads(), qSimTimeStepSize);
				yield byRoadClass(freeSpeedMatrix, congestionIndex, shares, config);
			}
		};
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

		double index = this.roadClassShares == null
			? this.congestionIndex.get(at)
			: congestionFactorByRoadClass(fromNode, toNode, at);

		return 1 + this.calibrationFactor * index;
	}

	/**
	 * {@code sum_k s_k(o,d) * CI_k(t)}: the index of each road class, weighted by how much of the
	 * free-speed travel time of this pair is spent on that class.
	 */
	private double congestionFactorByRoadClass(Node fromNode, Node toNode, double at) {
		List<String> roadClasses = this.roadClassShares.getRoadClasses();
		double[] index = this.indexBuffer.get();
		for (int k = 0; k < roadClasses.size(); k++) {
			index[k] = this.congestionIndex.get(at, roadClasses.get(k));
		}
		return this.roadClassShares.weigh(fromNode, toNode, index);
	}

	/**
	 * @return the congestion index this matrix reads, e.g. to write it out next to the results it
	 * produced
	 */
	public CongestionIndex getCongestionIndex() {
		return this.congestionIndex;
	}

	/**
	 * @return the road class shares of level 1, or null at level 0
	 */
	@Nullable
	public RoadClassShares getRoadClassShares() {
		return this.roadClassShares;
	}

	@Override
	public String toString() {
		String level = this.roadClassShares == null
			? "level 0, one index for the whole network"
			: "level 1, " + this.roadClassShares.getRoadClasses().size() + " road classes "
			+ this.roadClassShares.getRoadClasses();
		String bins = this.congestionIndex instanceof CongestionIndexProfile profile
			? profile.getNumberOfBins() + " bins of " + profile.getTimeBinSize() + " s"
			: this.congestionIndex.toString();
		return String.format(Locale.ROOT, "TimeVaryingTravelTimeMatrix[%s, %s, beta = %.3f, index read at %s]", level,
			bins, this.calibrationFactor, this.evaluateAtMidpoint ? "the middle of the trip" : "departure");
	}
}
