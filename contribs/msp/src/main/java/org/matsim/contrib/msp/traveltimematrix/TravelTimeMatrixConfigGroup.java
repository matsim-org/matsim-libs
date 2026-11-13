package org.matsim.contrib.msp.traveltimematrix;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndex;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.nio.file.Path;
import java.util.Map;

/**
 * Config group of the time-varying travel time matrix.
 *
 * @see TimeVaryingTravelTimeMatrix
 */
public final class TravelTimeMatrixConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "timeVaryingTravelTimeMatrix";

	private static final String VARIANT = "variant";
	private static final String TIME_BIN_SIZE = "timeBinSize";
	private static final String END_TIME = "endTime";
	private static final String CONGESTION_INDEX_FILE = "congestionIndexFile";
	private static final String ROAD_CLASSES = "roadClasses";
	private static final String EVALUATE_INDEX_AT = "evaluateIndexAt";
	private static final String CALIBRATION_FACTOR = "calibrationFactor";
	private static final String CELL_SIZE = "cellSize";
	private static final String MAX_NEIGHBOR_DISTANCE = "maxNeighborDistance";
	private static final String MAX_NEIGHBOR_TRAVEL_TIME = "maxNeighborTravelTime";
	private static final String MATRIX_CACHE_FILE = "matrixCacheFile";
	private static final String NUMBER_OF_THREADS = "numberOfThreads";

	/**
	 * How much of the time of day the matrix models. The two are nested: both answer the same query,
	 * and {@link #GLOBAL_INDEX} knows one thing more than {@link #FREE_SPEED}.
	 * <p>
	 * There used to be a third, {@code BY_ROAD_CLASS}, weighting one index per road class by the share
	 * of the pair's free-speed travel time spent on each. It was removed: splitting the index by road
	 * class moves the estimate closer to the travel times the <em>simulation recorded</em>, and the
	 * problem it was meant to solve is that the simulation records no congestion at all on the links an
	 * accessibility analysis mostly asks about - so no re-weighting of what was measured can fix it. See
	 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.CongestionExposure}. The road
	 * <em>classification</em> survives as a diagnostic on the index itself, which is cheap and tells one
	 * something; the per-pair shares cost 3.2 GB on Dresden and told one nothing.
	 */
	public enum Variant {
		/**
		 * The free-speed matrix itself, with no congestion at all. The null case of an experiment: the
		 * error of this variant is what every other one has to beat.
		 */
		FREE_SPEED,
		/**
		 * Level 0: one congestion index for the whole network per time bin,
		 * {@code TT = TT_ff * (1 + beta * CI(t))}.
		 */
		GLOBAL_INDEX
	}

	/**
	 * Which road classes the congestion index is measured per, as a diagnostic - see
	 * {@link org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexCalculator}. The matrix
	 * itself reads only the network-wide curve.
	 */
	public enum RoadClasses {
		/**
		 * {@link RoadClassification#DEFAULT_GROUPS}: motorway-like, primary, secondary, tertiary,
		 * residential, other.
		 */
		DEFAULT_GROUPS,
		/**
		 * One class per OSM highway type that occurs in the network. Finer, and usually too fine - see
		 * {@link RoadClassification#byHighwayType(Network)}.
		 */
		OSM_HIGHWAY_TYPE
	}

	/**
	 * When during the trip the congestion index is read.
	 */
	public enum EvaluateIndexAt {
		/**
		 * At the departure time. What a time-dependent router does for the first link of a trip, and
		 * wrong for the rest of it.
		 */
		DEPARTURE,
		/**
		 * At departure plus half the free-speed travel time, i.e. the middle of the trip. The cheapest
		 * way to account for the fact that a long trip is mostly spent somewhere other than at its
		 * start: a trip leaving at 07:00 and lasting an hour is in the peak, and a trip leaving at 08:30
		 * and lasting an hour is coming out of it.
		 */
		MIDPOINT
	}

	private Variant variant = Variant.GLOBAL_INDEX;
	private int timeBinSize = 900;
	private int endTime = 86400;
	private String congestionIndexFile = null;
	private RoadClasses roadClasses = RoadClasses.DEFAULT_GROUPS;
	private EvaluateIndexAt evaluateIndexAt = EvaluateIndexAt.MIDPOINT;
	private double calibrationFactor = 1.0;
	private double cellSize = 1000;
	private double maxNeighborDistance = 5000;
	private double maxNeighborTravelTime = 0;
	private String matrixCacheFile = null;

	/**
	 * Set instead of {@link #matrixCacheFile} when the name is to be derived from the thresholds. Kept as
	 * a directory rather than resolved on the spot, so that the thresholds may be set afterwards - see
	 * {@link #setMatrixCacheDirectory(String)}.
	 */
	private String matrixCacheDirectory = null;
	private int numberOfThreads = Runtime.getRuntime().availableProcessors();

	public TravelTimeMatrixConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(VARIANT)
	public Variant getVariant() {
		return this.variant;
	}

	@StringSetter(VARIANT)
	public void setVariant(Variant variant) {
		this.variant = variant;
	}

	@StringGetter(TIME_BIN_SIZE)
	public int getTimeBinSize() {
		return this.timeBinSize;
	}

	@StringSetter(TIME_BIN_SIZE)
	public void setTimeBinSize(int timeBinSize) {
		this.timeBinSize = timeBinSize;
	}

	@StringGetter(END_TIME)
	public int getEndTime() {
		return this.endTime;
	}

	@StringSetter(END_TIME)
	public void setEndTime(int endTime) {
		this.endTime = endTime;
	}

	@StringGetter(CONGESTION_INDEX_FILE)
	public String getCongestionIndexFile() {
		return this.congestionIndexFile;
	}

	@StringSetter(CONGESTION_INDEX_FILE)
	public void setCongestionIndexFile(String congestionIndexFile) {
		this.congestionIndexFile = congestionIndexFile;
	}

	@StringGetter(ROAD_CLASSES)
	public RoadClasses getRoadClasses() {
		return this.roadClasses;
	}

	@StringSetter(ROAD_CLASSES)
	public void setRoadClasses(RoadClasses roadClasses) {
		this.roadClasses = roadClasses;
	}

	@StringGetter(EVALUATE_INDEX_AT)
	public EvaluateIndexAt getEvaluateIndexAt() {
		return this.evaluateIndexAt;
	}

	@StringSetter(EVALUATE_INDEX_AT)
	public void setEvaluateIndexAt(EvaluateIndexAt evaluateIndexAt) {
		this.evaluateIndexAt = evaluateIndexAt;
	}

	@StringGetter(CALIBRATION_FACTOR)
	public double getCalibrationFactor() {
		return this.calibrationFactor;
	}

	@StringSetter(CALIBRATION_FACTOR)
	public void setCalibrationFactor(double calibrationFactor) {
		this.calibrationFactor = calibrationFactor;
	}

	@StringGetter(CELL_SIZE)
	public double getCellSize() {
		return this.cellSize;
	}

	@StringSetter(CELL_SIZE)
	public void setCellSize(double cellSize) {
		this.cellSize = cellSize;
	}

	@StringGetter(MAX_NEIGHBOR_DISTANCE)
	public double getMaxNeighborDistance() {
		return this.maxNeighborDistance;
	}

	@StringSetter(MAX_NEIGHBOR_DISTANCE)
	public void setMaxNeighborDistance(double maxNeighborDistance) {
		this.maxNeighborDistance = maxNeighborDistance;
	}

	@StringGetter(MAX_NEIGHBOR_TRAVEL_TIME)
	public double getMaxNeighborTravelTime() {
		return this.maxNeighborTravelTime;
	}

	@StringSetter(MAX_NEIGHBOR_TRAVEL_TIME)
	public void setMaxNeighborTravelTime(double maxNeighborTravelTime) {
		this.maxNeighborTravelTime = maxNeighborTravelTime;
	}

	/**
	 * Whether the neighbourhood is unbounded, i.e. whether {@link #getMaxNeighborDistance()} is infinite.
	 * <p>
	 * It is a configuration of its own and not merely a large radius, because it changes what is built:
	 * every pair is then stored node to node, so there is no zonal half, no zone system, and
	 * {@link #getCellSize()} describes nothing and is ignored. See
	 * {@link NodeToNodeTravelTimeMatrix}, which is what
	 * {@link TimeVaryingTravelTimeMatrix#create} builds in this case instead of the two-half matrix of
	 * {@code contribs/dvrp}.
	 */
	public boolean isUnboundedNeighborhood() {
		return Double.isInfinite(this.maxNeighborDistance);
	}

	/**
	 * @return the cache file: the one that was set explicitly, or the one
	 * {@link #setMatrixCacheDirectory(String)} implies for the thresholds <em>as they are now</em>, or
	 * null if the base matrix is not to be cached at all
	 * <p>
	 * An unbounded neighbourhood is never cached from a directory, whatever
	 * {@link #setMatrixCacheDirectory(String)} was given. The cache format of {@code contribs/dvrp}
	 * starts with the zone system, which this configuration does not have; and it would be a false
	 * economy anyway - the file holds a travel time per node pair, which on Dresden is a 2 GB file
	 * written by {@code n^2} binary searches, against under a minute to route the whole thing again.
	 * An explicitly set {@link #setMatrixCacheFile(String)} is still honoured, so that a caller who has
	 * their own format can say so.
	 */
	@StringGetter(MATRIX_CACHE_FILE)
	public String getMatrixCacheFile() {
		if (this.matrixCacheFile != null) {
			return this.matrixCacheFile;
		}
		return this.matrixCacheDirectory == null || isUnboundedNeighborhood()
			? null
			: Path.of(this.matrixCacheDirectory, getMatrixCacheFileName()).toString();
	}

	@StringSetter(MATRIX_CACHE_FILE)
	public void setMatrixCacheFile(String matrixCacheFile) {
		this.matrixCacheFile = matrixCacheFile;
	}

	@StringGetter(NUMBER_OF_THREADS)
	public int getNumberOfThreads() {
		return this.numberOfThreads;
	}

	@StringSetter(NUMBER_OF_THREADS)
	public void setNumberOfThreads(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	/**
	 * The name a cache file of the current thresholds has to have: {@code free_speed_matrix_cell1000_nb5000.bin}.
	 * <p>
	 * The thresholds are in the <b>name</b> because a cache is only valid for the zone system and the
	 * neighbourhood it was built with, and nothing checks the second of those. Point two different
	 * {@link #getCellSize()} at one cache file and {@code FreeSpeedTravelTimeMatrix} throws a bare
	 * {@code VerifyException} from deep inside itself, because the zones of the file are not the zones it
	 * was asked for. Change only {@link #getMaxNeighborDistance()} and nothing complains at all: the
	 * sparse part of the cache is read back as it was written, so the matrix would quietly answer short
	 * trips with the threshold of a previous run. A sweep over the thresholds is exactly what one does to
	 * choose them, so the file name has to make the two impossible to confuse.
	 *
	 * @see #setMatrixCacheDirectory(String)
	 */
	public String getMatrixCacheFileName() {
		String name = String.format("free_speed_matrix_cell%d_nb%d", (long) this.cellSize,
			(long) this.maxNeighborDistance);
		if (this.maxNeighborTravelTime > 0) {
			name += "_nbtt" + (long) this.maxNeighborTravelTime;
		}
		return name + ".bin";
	}

	/**
	 * Caches the base matrix in this directory, under the name the thresholds imply.
	 * <p>
	 * The name is worked out when {@link #getMatrixCacheFile()} is asked, not here, so the thresholds may
	 * be set before or after this in any order. That is deliberate: resolving it here means a caller who
	 * sets the directory first silently caches under the <em>default</em> thresholds and then reads that
	 * file back for every setting of a sweep - which is a wrong answer, not an error, and is exactly the
	 * mistake this file's own commands made.
	 *
	 * @see #getMatrixCacheFileName()
	 */
	public void setMatrixCacheDirectory(String directory) {
		this.matrixCacheDirectory = directory;
	}

	/**
	 * The parameters of the free-speed base matrix, in the form {@code contribs/dvrp} wants them: a
	 * square grid of {@link #getCellSize()} plus the neighbourhood thresholds of the sparse part.
	 * <p>
	 * Only a square grid is configurable here, which covers the case this contrib is written for. A
	 * shapefile or H3 zone system is a {@link DvrpTravelTimeMatrixParams} away - build one yourself and
	 * pass the matrix to {@link TimeVaryingTravelTimeMatrix} directly instead of going through
	 * {@link TimeVaryingTravelTimeMatrix#create}.
	 */
	public DvrpTravelTimeMatrixParams getMatrixParams() {
		DvrpTravelTimeMatrixParams params = new DvrpTravelTimeMatrixParams();
		SquareGridZoneSystemParams zones = new SquareGridZoneSystemParams();
		zones.setCellSize(this.cellSize);
		params.addParameterSet(zones);
		params.setMaxNeighborDistance(this.maxNeighborDistance);
		params.setMaxNeighborTravelTime(this.maxNeighborTravelTime);
		params.setCachePath(getMatrixCacheFile());
		return params;
	}

	/**
	 * The classification the configured {@link RoadClasses} means, over the given network. Used to
	 * <em>measure</em> the index per road class, not to query it: the matrix reads the network-wide curve
	 * alone.
	 */
	public RoadClassification getRoadClassification(Network network) {
		return switch (this.roadClasses) {
			case DEFAULT_GROUPS -> RoadClassification.byDefaultGroups(network);
			case OSM_HIGHWAY_TYPE -> RoadClassification.byHighwayType(network);
		};
	}

	/**
	 * The congestion index the configured {@link #getCongestionIndexFile()} holds, or
	 * {@link CongestionIndex#freeFlow()} for {@link Variant#FREE_SPEED}.
	 *
	 * @throws IllegalStateException if a variant that needs an index has no file to read it from
	 */
	public CongestionIndex getCongestionIndex() {
		if (this.variant == Variant.FREE_SPEED) {
			return CongestionIndex.freeFlow();
		}
		if (this.congestionIndexFile == null) {
			throw new IllegalStateException("Variant " + this.variant + " needs a congestion index, but '"
				+ CONGESTION_INDEX_FILE + "' is not set in the '" + GROUP_NAME + "' config group. Measure one on a "
				+ "finished run with " + RunComputeCongestionIndex.class.getSimpleName() + ", or set the variant to "
				+ Variant.FREE_SPEED + ".");
		}
		return CongestionIndexProfile.read(this.congestionIndexFile);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(VARIANT, "How much of the time of day the matrix models: " + Variant.FREE_SPEED
			+ " (no congestion at all, the baseline) or " + Variant.GLOBAL_INDEX + " (one index for the whole "
			+ "network).");
		comments.put(TIME_BIN_SIZE, "Resolution of the congestion index [s]. Only used when the index is measured; "
			+ "an index read from a file brings its own bin size.");
		comments.put(END_TIME, "End of the day the index is measured over [s]. A query past it gets the last bin.");
		comments.put(CONGESTION_INDEX_FILE, "The congestion index profile csv, as written by "
			+ RunComputeCongestionIndex.class.getSimpleName() + ". Required for every variant but "
			+ Variant.FREE_SPEED + ".");
		comments.put(ROAD_CLASSES, "Which road classes the congestion index is measured per, as a diagnostic: "
			+ RoadClasses.DEFAULT_GROUPS + " or " + RoadClasses.OSM_HIGHWAY_TYPE + ". The matrix reads the "
			+ "network-wide curve whichever is set.");
		comments.put(EVALUATE_INDEX_AT, "When during the trip the index is read: " + EvaluateIndexAt.DEPARTURE
			+ ", or " + EvaluateIndexAt.MIDPOINT + " (departure + half the free-speed travel time).");
		comments.put(CALIBRATION_FACTOR, "beta of TT = TT_ff * (1 + beta * CI). 1 means the index is used as "
			+ "measured; fit it on a sample of time-dependent routes to correct the systematic error of the "
			+ "approximation.");
		comments.put(CELL_SIZE, "Cell size of the square grid the zonal part of the free-speed matrix is built over "
			+ "[m]. Memory grows with the square of the number of cells. Ignored when '" + MAX_NEIGHBOR_DISTANCE
			+ "' is infinite, which leaves no zonal part to describe.");
		comments.put(MAX_NEIGHBOR_DISTANCE, "Trips shorter than this are answered node to node instead of zone to "
			+ "zone [m], which is what keeps short trips accurate. Memory grows with the number of node pairs "
			+ "within it. Infinity means every pair is stored node to node: the matrix is then exact, no zone "
			+ "system is built at all.");
		comments.put(MAX_NEIGHBOR_TRAVEL_TIME, "The same threshold as a travel time [s]. Meeting either threshold "
			+ "is enough; 0 switches this one off.");
		comments.put(MATRIX_CACHE_FILE, "Binary cache of the free-speed matrix, as a file path. Read if the file "
			+ "exists, written if it does not, so that a series of experiments computes the matrix once. A cache "
			+ "is only valid for the cellSize and maxNeighborDistance it was built with - name it accordingly, or "
			+ "let setMatrixCacheDirectory(..) do it.");
		comments.put(NUMBER_OF_THREADS, "Threads the matrix is computed with.");
		return comments;
	}
}
