package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.msp.traveltimematrix.RoadClassification;
import org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix;
import org.matsim.contrib.msp.traveltimematrix.TravelTimeMatrixConfigGroup;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndex;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile;
import org.matsim.contrib.msp.traveltimematrix.prepare.MeasuredTraffic;
import org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaNetwork;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.network.NetworkUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Measures how wrong the time-varying travel time matrix is, and writes the error tables:
 * <pre>
 *   java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix \
 *       --network output_network.xml.gz --events output_events.xml.gz --shp dresden-with-buffer.shp \
 *       --congestion-index congestion_index.csv --output-directory evaluation \
 *       --od-pairs TRIPS --reference EXPERIENCED
 * </pre>
 * There are two independent choices to make, and they are the whole command:
 * <p>
 * <b>Which questions to ask</b> ({@code --od-pairs}). The trips a run really made, or generated pairs.
 * Real trips weight the error by what actually happens; generated pairs cover the matrix evenly,
 * including the pairs and hours nobody drove in this scenario - which, for an analysis asking "could
 * this person have reached that shop at 19:00", is most of them.
 * <p>
 * <b>What to compare against</b> ({@code --reference}). What the trips really took, or what a
 * time-dependent router computes on the travel times of the run, or - later - what a routing API says.
 * The first contains everything the matrix cannot know (the route the agent chose, the queue it sat in,
 * the exact minute it left) and is the error an analysis using the matrix really suffers; the second
 * isolates the error of the approximation itself. They typically have <b>opposite signs</b>: against a
 * router the matrix is too slow, because it holds the free-speed path fixed, and against the experienced
 * travel times it can be too fast, because a network-wide index does not see the queue a particular trip
 * sat in.
 * <p>
 * Only one combination is impossible, and it is refused immediately rather than halfway through:
 * {@link Reference#EXPERIENCED} needs {@link ODPairs#TRIPS}, because a pair nobody drove has no
 * experienced travel time to be compared against.
 * <p>
 * Everything else is one code path: the choices only decide what fills in the
 * {@link TravelTimeQuery#referenceTravelTime()} of a list of queries, and {@link MatrixEvaluation} then
 * subtracts. So every table has the same columns whatever was chosen, and two of them can be read side
 * by side.
 *
 * @see MatrixEvaluation what is computed
 * @see org.matsim.contrib.msp.traveltimematrix.RunComputeTravelTimeMatrix to build and cache the matrix
 * itself
 */
@CommandLine.Command(
	name = "evaluate-travel-time-matrix",
	description = "Measures the error of the time-varying travel time matrix against routing or against a run.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public final class RunEvaluateTravelTimeMatrix implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunEvaluateTravelTimeMatrix.class);

	/**
	 * What the matrix is compared against.
	 */
	public enum Reference {
		/**
		 * What the trips of the run really took, node to node, read from the events - see
		 * {@link ExperiencedLegs}. Needs {@link ODPairs#TRIPS}.
		 */
		EXPERIENCED,
		/**
		 * Time-dependent routing over the congested travel times of the run - see
		 * {@link RoutedTravelTimes}. Works with any pairs, and costs one shortest-path tree per distinct
		 * origin and departure time.
		 */
		ROUTED,
		/**
		 * What a routing API answers for the same pairs. Not implemented - see
		 * {@link #apiReference}.
		 */
		API
	}

	/**
	 * Where the origin-destination pairs come from.
	 */
	public enum ODPairs {
		/**
		 * The car trips of the run, as the node-to-node legs they were driven as.
		 */
		TRIPS,
		/**
		 * Generated pairs, one destination per distance band and origin - see {@link ODSample}.
		 */
		RANDOM,
		/**
		 * The queries of an earlier run, read back from its {@code queries} csv, so that two experiments
		 * can be compared on exactly the same questions.
		 */
		FILE
	}

	@CommandLine.Option(names = "--network", description = "Network of the run.", required = true)
	private String networkFile;

	@CommandLine.Option(names = "--events", description = "Events of the run. Required unless the pairs come from "
		+ "a FILE that carries its own reference travel times: the congested travel times a ROUTED reference "
		+ "needs and the legs TRIPS and EXPERIENCED need are both read from them, in one pass.")
	private String eventsFile;

	@CommandLine.Option(names = "--congestion-index", description = "The congestion index csv, as written by "
		+ "RunComputeCongestionIndex, measured on the same events over the same study area.", required = true)
	private String congestionIndexFile;

	@CommandLine.Option(names = "--output-directory", description = "Directory the result csvs are written to.",
		defaultValue = "travel-time-matrix-evaluation")
	private String outputDirectory;

	@CommandLine.Option(names = "--od-pairs", description = "Where the origin-destination pairs come from: "
		+ "${COMPLETION-CANDIDATES}.", defaultValue = "RANDOM")
	private ODPairs odPairs;

	@CommandLine.Option(names = "--reference", description = "What the matrix is compared against: "
		+ "${COMPLETION-CANDIDATES}. EXPERIENCED requires --od-pairs TRIPS.", defaultValue = "ROUTED")
	private Reference reference;

	@CommandLine.Option(names = "--also-route", description = "With --reference EXPERIENCED, also route the same "
		+ "queries, so that the time-dependent optimum and level 3 appear as variants next to the matrix. That is "
		+ "what separates the error of the matrix from the error the simulation's own route choice adds - at the "
		+ "price of one shortest-path tree per distinct origin and departure time.")
	private boolean alsoRoute;

	@CommandLine.Option(names = "--modes", description = "Modes of the subnetwork and of the travel times.",
		defaultValue = "car", split = ",")
	private Set<String> modes;

	@CommandLine.Option(names = "--mode", description = "The network mode whose trips are used by TRIPS and "
		+ "EXPERIENCED.", defaultValue = TransportMode.car)
	private String mode;

	@CommandLine.Option(names = "--max-trips", description = "How many trips to keep with --od-pairs TRIPS, as a "
		+ "reservoir sample over the whole day. A MAPE needs far fewer than a city produces, and with a ROUTED "
		+ "reference every trip costs a shortest-path tree.", defaultValue = "20000")
	private int maximumNumberOfTrips;

	@CommandLine.Option(names = "--origins", description = "How many origins to generate with --od-pairs RANDOM. "
		+ "Each gets one destination per distance band, asked at every departure hour.", defaultValue = "200")
	private int numberOfOrigins;

	@CommandLine.Option(names = "--departure-hours", description = "Hours of the day the generated pairs are asked "
		+ "about.", defaultValue = "0,3,6,7,8,9,12,15,16,17,18,21", split = ",")
	private List<Integer> departureHours;

	@CommandLine.Option(names = "--distance-bands", description = "Lower edges of the distance bands [m]. With "
		+ "RANDOM one destination is generated per band and origin; the error is reported per band either way.",
		defaultValue = "0,2000,5000,10000,20000", split = ",")
	private List<Double> distanceBands;

	@CommandLine.Option(names = "--od-file", description = "The queries csv to read with --od-pairs FILE.")
	private String odFile;

	@CommandLine.Option(names = "--write-queries", description = "Also write one row per query and variant. Large: "
		+ "as many rows as queries times variants.")
	private boolean writeQueries;

	@CommandLine.Option(names = "--seed", description = "Seed of the sampling.", defaultValue = "4711")
	private long seed;

	@CommandLine.Option(names = "--time-bin-size", description = "Resolution of the congested travel times read "
		+ "from the events [s]. Use what the congestion index was measured with.", defaultValue = "900")
	private int timeBinSize;

	@CommandLine.Option(names = "--end-time", description = "End of the day [s].", defaultValue = "86400")
	private int endTime;

	@CommandLine.Option(names = "--qsim-time-step-size", description = "Time step size of the run [s].",
		defaultValue = "1")
	private double qSimTimeStepSize;

	@CommandLine.Option(names = "--cell-size", description = "Cell size of the zonal part of the matrix [m].",
		defaultValue = "1000")
	private double cellSize;

	@CommandLine.Option(names = "--max-neighbor-distance", description = "Trips shorter than this are answered node "
		+ "to node by the matrix [m].", defaultValue = "5000")
	private double maxNeighborDistance;

	@CommandLine.Option(names = "--calibration-factor", description = "beta of TT = TT_ff * (1 + beta * CI).",
		defaultValue = "1.0")
	private double calibrationFactor;

	@CommandLine.Option(names = "--evaluate-index-at", description = "When during the trip the index is read: "
		+ "${COMPLETION-CANDIDATES}.", defaultValue = "MIDPOINT")
	private TravelTimeMatrixConfigGroup.EvaluateIndexAt evaluateIndexAt;

	@CommandLine.Option(names = "--threads", description = "Threads for the matrix and the routing.")
	private Integer numberOfThreads;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	public static void main(String[] args) {
		new RunEvaluateTravelTimeMatrix().execute(args);
	}

	@Override
	public Integer call() {

		checkTheCombination();

		try {
			Files.createDirectories(Path.of(this.outputDirectory));
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create the output directory " + this.outputDirectory, e);
		}

		Network fullNetwork = NetworkUtils.readNetwork(this.networkFile);
		Network network = StudyAreaNetwork.cut(fullNetwork, StudyAreaNetwork.index(this.shp, fullNetwork), this.modes);

		// one pass over the events for everything that is read from them: the congested travel times a
		// routed reference needs, the volumes, and the legs the trips are taken from
		ExperiencedLegs legs = this.odPairs == ODPairs.TRIPS
			? new ExperiencedLegs(network, this.mode, this.maximumNumberOfTrips, this.seed)
			: null;
		MeasuredTraffic traffic = needsEvents()
			? MeasuredTraffic.readFromEvents(network, this.eventsFile, this.modes, this.timeBinSize, this.endTime,
			legs == null ? new ExperiencedLegs[0] : new ExperiencedLegs[]{legs})
			: null;

		CongestionIndexProfile congestionIndex = CongestionIndexProfile.read(this.congestionIndexFile);

		List<TravelTimeQuery> asked = queries(network, legs);
		RoutedTravelTimes routed = routeIfNeeded(network, traffic, asked);
		List<TravelTimeQuery> answered = reference(asked, routed);

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(answered,
			buildVariants(network, congestionIndex, routed),
			this.distanceBands.stream().mapToDouble(Double::doubleValue).toArray(),
			this.writeQueries ? output("queries.csv") : null);

		evaluation.writeErrors(output("errors.csv"));
		TravelTimeQuery.write(answered, output("od_queries.csv"));
		congestionIndex.write(output("congestion_index.csv"));

		return 0;
	}

	/**
	 * The one combination of the two choices that cannot work, and the inputs each of them needs.
	 * <p>
	 * Checked before anything is read, because the alternative is to find out after the events, the
	 * network and the matrix - which on a city-sized scenario is a quarter of an hour of someone's
	 * afternoon.
	 */
	private void checkTheCombination() {

		if (this.reference == Reference.EXPERIENCED && this.odPairs == ODPairs.RANDOM) {
			throw new IllegalArgumentException("--reference " + Reference.EXPERIENCED + " cannot be used with "
				+ "--od-pairs " + ODPairs.RANDOM + ": a generated origin-destination pair was never driven, so "
				+ "there is no experienced travel time to compare the matrix against. Use --od-pairs "
				+ ODPairs.TRIPS + " to evaluate on the trips the run really made, or --reference "
				+ Reference.ROUTED + " to have the travel time of a generated pair computed by a router.");
		}

		if (this.odPairs == ODPairs.FILE && this.odFile == null) {
			throw new IllegalArgumentException("--od-pairs " + ODPairs.FILE + " needs --od-file, the queries csv of "
				+ "an earlier run.");
		}

		if (needsEvents() && this.eventsFile == null) {
			throw new IllegalArgumentException("--events is needed for --od-pairs " + this.odPairs + " with "
				+ "--reference " + this.reference + ": the trips and the congested travel times are both read from "
				+ "them. Only queries read from a FILE that already carries its reference travel times can do "
				+ "without.");
		}
	}

	/**
	 * Whether anything in this run has to be read off the events: the trips, or the travel times that a
	 * router routes on.
	 */
	private boolean needsEvents() {
		return this.odPairs == ODPairs.TRIPS || routes();
	}

	/**
	 * Whether queries are routed - because that is the reference, or because the routed rungs were asked
	 * for as extra variants.
	 */
	private boolean routes() {
		return this.reference == Reference.ROUTED || this.alsoRoute;
	}

	/**
	 * The questions to ask, with the experienced travel time already in them when they come from trips.
	 */
	private List<TravelTimeQuery> queries(Network network, ExperiencedLegs legs) {

		List<TravelTimeQuery> queries = switch (this.odPairs) {
			case TRIPS -> legs.asQueries();
			case RANDOM -> ODSample.byDistanceBands(network, this.numberOfOrigins,
				this.distanceBands.stream().mapToDouble(Double::doubleValue).toArray(),
				this.departureHours.stream().mapToDouble(hour -> hour * 3600.0).toArray(), this.seed);
			case FILE -> TravelTimeQuery.read(this.odFile, network);
		};

		log.info("Evaluating on {} queries from {}, compared against {}.", queries.size(), this.odPairs,
			this.reference);
		return queries;
	}

	private RoutedTravelTimes routeIfNeeded(Network network, MeasuredTraffic traffic,
		List<TravelTimeQuery> queries) {
		return routes()
			? RoutedTravelTimes.compute(network, queries, traffic.getTravelTimes(), this.qSimTimeStepSize, threads())
			: null;
	}

	/**
	 * The queries with the reference travel time of the chosen kind in them.
	 */
	private List<TravelTimeQuery> reference(List<TravelTimeQuery> queries, RoutedTravelTimes routed) {
		return switch (this.reference) {
			case EXPERIENCED -> experiencedReference(queries);
			case ROUTED -> routed.getQueries();
			case API -> apiReference(queries);
		};
	}

	/**
	 * The travel times the trips really took, which the queries already carry - so this only checks that
	 * they do. A query read from a file may not, and then the file is the wrong one for this reference.
	 */
	private List<TravelTimeQuery> experiencedReference(List<TravelTimeQuery> queries) {

		long without = queries.stream().filter(query -> !query.hasReference()).count();
		if (without > 0) {
			throw new IllegalArgumentException(without + " of the " + queries.size() + " queries carry no travel "
				+ "time of their own, so --reference " + Reference.EXPERIENCED + " has nothing to compare against. "
				+ "A queries csv only carries one if it was written by a run that had one, i.e. from --od-pairs "
				+ ODPairs.TRIPS + ".");
		}
		return queries;
	}

	/**
	 * <b>Not implemented.</b> The seam is here and it is deliberately narrow: everything else about this
	 * command is independent of where a reference travel time comes from, so a routing API means filling
	 * in {@link TravelTimeQuery#withReference} for each query and nothing else.
	 * <p>
	 * The way to do it without writing an API client at all: run this command with
	 * {@code --od-pairs RANDOM --reference ROUTED}, take the {@code od_queries.csv} it writes - which is
	 * in the {@code from_node,to_node} shape {@code SampleValidationRoutes} of
	 * {@code contribs/application} reads - fetch the travel times with that command, write them back
	 * into the {@code reference_travel_time} column, and come back with
	 * {@code --od-pairs FILE --reference EXPERIENCED}. That path works today and needs no code; what is
	 * missing here is only the convenience of doing it in one go.
	 */
	private List<TravelTimeQuery> apiReference(List<TravelTimeQuery> queries) {
		throw new UnsupportedOperationException("--reference " + Reference.API + " is not implemented yet. Fetch the "
			+ "travel times of the od_queries.csv this command writes with SampleValidationRoutes of "
			+ "contribs/application, fill in its reference_travel_time column, and evaluate them with --od-pairs "
			+ ODPairs.FILE + " --reference " + Reference.EXPERIENCED + " - which is the same comparison, by hand.");
	}

	/**
	 * Every rung of the ladder, by the name it is reported under, in the order they are to be read: the
	 * routed ones first when they were computed - the reference among them, whose error is the rounding
	 * of a travel time to whole seconds and nothing else, which is the check that the comparison is
	 * wired up correctly - then down the ladder to the free-speed matrix.
	 * <p>
	 * The matrix variants deliberately share <b>one</b> free-speed base matrix, which
	 * {@link TimeVaryingTravelTimeMatrix#create} computes once per config and caches in
	 * {@code --output-directory}. They differ in the congestion model, so computing the base matrix
	 * three times would only buy three chances of it coming out differently.
	 */
	private Map<String, TravelTimeMatrix> buildVariants(Network network, CongestionIndexProfile congestionIndex,
		RoutedTravelTimes routed) {

		TravelTimeMatrixConfigGroup config = config();
		TimeVaryingTravelTimeMatrix byRoadClass = TimeVaryingTravelTimeMatrix.create(network, config,
			this.qSimTimeStepSize);
		TravelTimeMatrix freeSpeedMatrix = byRoadClass.getFreeSpeedMatrix();

		Map<String, TravelTimeMatrix> variants = new LinkedHashMap<>();
		if (routed != null) {
			variants.put(MatrixEvaluation.REFERENCE, routed.asTimeDependentMatrix());
			variants.put(MatrixEvaluation.FIXED_PATH, routed.asFixedPathMatrix());
		}
		variants.put(TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS.name(), byRoadClass);
		variants.put(TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX.name(),
			TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix, congestionIndex, config));
		variants.put(MatrixEvaluation.CONSTANT_INDEX, TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix,
			CongestionIndex.constant(congestionIndex.getDailyMean(RoadClassification.ALL)), config));
		variants.put(TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name(),
			TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix, CongestionIndex.freeFlow(), config));

		log.info("Evaluating {}.", variants.keySet());
		return variants;
	}

	private TravelTimeMatrixConfigGroup config() {
		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setVariant(TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS);
		config.setCongestionIndexFile(this.congestionIndexFile);
		config.setCellSize(this.cellSize);
		config.setMaxNeighborDistance(this.maxNeighborDistance);
		config.setCalibrationFactor(this.calibrationFactor);
		config.setEvaluateIndexAt(this.evaluateIndexAt);
		config.setTimeBinSize(this.timeBinSize);
		config.setEndTime(this.endTime);
		config.setNumberOfThreads(threads());
		config.setMatrixCacheDirectory(this.outputDirectory);
		return config;
	}

	private int threads() {
		return this.numberOfThreads == null ? Runtime.getRuntime().availableProcessors() : this.numberOfThreads;
	}

	private String output(String file) {
		return Path.of(this.outputDirectory, file).toString();
	}
}
