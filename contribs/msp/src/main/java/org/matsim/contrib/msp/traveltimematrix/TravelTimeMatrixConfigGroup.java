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
	 * How much of the time of day the matrix models - the rungs of the approximation ladder that are
	 * implemented. They are strictly nested: every one of them answers the same query, and each knows
	 * more than the one above it.
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
		GLOBAL_INDEX,
		/**
		 * Level 1: one index per road class, weighted by the share of the free-speed travel time the
		 * origin-destination pair spends on each class. Costs the
		 * {@link RoadClassShares road class shares} in memory and a second pass over the network when
		 * the matrix is built.
		 */
		BY_ROAD_CLASS
	}

	/**
	 * Which road classes {@link Variant#BY_ROAD_CLASS} distinguishes.
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

	@StringGetter(MATRIX_CACHE_FILE)
	public String getMatrixCacheFile() {
		return this.matrixCacheFile;
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
	 * Caches the base matrix in this directory, under the name the thresholds imply. Call it <b>after</b>
	 * the thresholds are set, for the obvious reason.
	 *
	 * @see #getMatrixCacheFileName()
	 */
	public void setMatrixCacheDirectory(String directory) {
		setMatrixCacheFile(Path.of(directory, getMatrixCacheFileName()).toString());
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
		params.setCachePath(this.matrixCacheFile);
		return params;
	}

	/**
	 * The classification the configured {@link RoadClasses} means, over the given network.
	 */
	public RoadClassification getRoadClassification(Network network) {
		if (this.variant != Variant.BY_ROAD_CLASS) {
			return RoadClassification.single();
		}
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
			+ " (no congestion at all, the baseline), " + Variant.GLOBAL_INDEX + " (one index for the whole network) "
			+ "or " + Variant.BY_ROAD_CLASS + " (one index per road class, weighted per origin-destination pair).");
		comments.put(TIME_BIN_SIZE, "Resolution of the congestion index [s]. Only used when the index is measured; "
			+ "an index read from a file brings its own bin size.");
		comments.put(END_TIME, "End of the day the index is measured over [s]. A query past it gets the last bin.");
		comments.put(CONGESTION_INDEX_FILE, "The congestion index profile csv, as written by "
			+ RunComputeCongestionIndex.class.getSimpleName() + ". Required for every variant but "
			+ Variant.FREE_SPEED + ".");
		comments.put(ROAD_CLASSES, "Which road classes " + Variant.BY_ROAD_CLASS + " distinguishes: "
			+ RoadClasses.DEFAULT_GROUPS + " or " + RoadClasses.OSM_HIGHWAY_TYPE + ".");
		comments.put(EVALUATE_INDEX_AT, "When during the trip the index is read: " + EvaluateIndexAt.DEPARTURE
			+ ", or " + EvaluateIndexAt.MIDPOINT + " (departure + half the free-speed travel time).");
		comments.put(CALIBRATION_FACTOR, "beta of TT = TT_ff * (1 + beta * CI). 1 means the index is used as "
			+ "measured; fit it on a sample of time-dependent routes to correct the systematic error of the "
			+ "approximation.");
		comments.put(CELL_SIZE, "Cell size of the square grid the zonal part of the free-speed matrix is built over "
			+ "[m]. Memory grows with the square of the number of cells.");
		comments.put(MAX_NEIGHBOR_DISTANCE, "Trips shorter than this are answered node to node instead of zone to "
			+ "zone [m], which is what keeps short trips accurate. Memory grows with the number of node pairs "
			+ "within it.");
		comments.put(MAX_NEIGHBOR_TRAVEL_TIME, "The same threshold as a travel time [s]. Meeting either threshold "
			+ "is enough; 0 switches this one off.");
		comments.put(MATRIX_CACHE_FILE, "Binary cache of the free-speed matrix, as a file path. Read if the file "
			+ "exists, written if it does not, so that a series of experiments computes the matrix once. A cache "
			+ "is only valid for the cellSize and maxNeighborDistance it was built with - name it accordingly, or "
			+ "let setMatrixCacheDirectory(..) do it.");
		comments.put(NUMBER_OF_THREADS, "Threads the matrix and the road class shares are computed with.");
		return comments;
	}
}
