package org.matsim.contrib.msp.traveltimematrix.evaluation;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.msp.traveltimematrix.RoadClassification;
import org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix;
import org.matsim.contrib.msp.traveltimematrix.MatrixOptions;
import org.matsim.contrib.msp.traveltimematrix.TravelTimeMatrixConfigGroup;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndex;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile;
import org.matsim.contrib.msp.traveltimematrix.prepare.MeasuredTraffic;
import org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaOptions;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * Measures how wrong the time-varying travel time matrix is, and writes the error tables:
 * <pre>
 *   java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix \
 *       --network output_network.xml.gz --events output_events.xml.gz --shp dresden-with-buffer.shp \
 *       --congestion-index congestion_index.csv --output-directory evaluation \
 *       --od-pairs TRIPS --references FREE_SPEED_ROUTING,CONGESTED_ROUTING,EXPERIENCED
 * </pre>
 * Both of those are the defaults, so the line above is what the command does with neither option given.
 * Add {@code --max-neighbor-distance inf} for the exact base matrix, with no zone system at all.
 *
 * There are two independent choices to make, and they are the whole command.
 * <p>
 * <b>Which questions to ask</b> ({@code --od-pairs}), and the default is {@link ODPairs#TRIPS} - the
 * trips a run really made - for a reason that took a round of experiments to find. A generated pair is
 * mostly made of links the run never put a vehicle on. Those links were never slowed down, so they carry
 * no excess travel time in any reference measured on that run, while the congestion index - which is
 * volume-weighted, i.e. measured where the traffic was - still says the trip should be several percent
 * slower. Applied to such a sample the index therefore <b>overestimates</b>, and on a uniform sample of
 * node pairs it can be worse than using no congestion model at all. That is a property of the questions,
 * not of the method, so the questions have to be the ones a congestion index can be asked: the trips that
 * happened. {@link CongestionExposure}, written next to the error table, is the measurement of exactly
 * this - and it is also the reason {@link ODPairs#RANDOM} is kept rather than removed, since the error on
 * pairs nobody drove is the error an accessibility analysis really suffers, and naming it is better than
 * hiding it.
 * <p>
 * <b>What to compare against</b> ({@code --references}), and this is a <em>list</em> whose default
 * follows from the pairs ({@link #defaultReferences()}): the matrix is
 * wrong in two separable ways, so a single reference cannot say which of them is at fault. Each
 * reference answers a different question, and one run reports all of the chosen ones side by side as a
 * grid - see {@link MatrixEvaluation} for the shape of it. The checks worth running are:
 * <ol>
 *     <li><b>Is the comparison wired up?</b> The {@link Reference#CONGESTED_ROUTING} reference against
 *     the {@code CONGESTED_ROUTING} <em>variant</em>: both sides are the same router on the same travel
 *     times, so the only error that may appear is the rounding of a travel time to whole seconds. It
 *     costs nothing - the variant is a lookup in what was routed anyway - and it is the row to glance at
 *     before believing any of the others. Against {@link Reference#EXPERIENCED} the same variant stops
 *     being a tautology and becomes the <em>most</em> informative row of all: it is the router against
 *     reality, i.e. everything that is wrong with the comparison before the matrix is even involved.</li>
 *     <li><b>What do the zones cost?</b> {@link Reference#FREE_SPEED_ROUTING} against the
 *     {@code FREE_SPEED} variant. No congestion on either side, so the only thing left to be wrong about
 *     is the matrix's own discretisation: the zonal part answers a long pair with the travel time between
 *     the <em>central nodes</em> of two zones. This is the error floor of every other variant, and
 *     {@code --cell-size} and {@code --max-neighbor-distance} are what move it.</li>
 *     <li><b>What does the congestion model cost?</b> {@link Reference#CONGESTED_ROUTING} or
 *     {@link Reference#EXPERIENCED} against {@code FREE_SPEED} (no congestion model at all - the null
 *     case it has to beat) and {@code GLOBAL_INDEX} (level 0, one index curve).</li>
 * </ol>
 * <b>How coarse the matrix is</b> ({@code --cell-size} and {@code --max-neighbor-distance}) is not one of
 * the two choices but the thing being judged, and one setting of it deserves naming here:
 * {@code --max-neighbor-distance inf}. Every pair is then stored node to node, so no zone system is built
 * at all, {@code --cell-size} is ignored, and the matrix's own discretisation error is <b>zero by
 * construction</b>. That is the configuration in which the error against
 * {@link Reference#CONGESTED_ROUTING} is the error of the congestion model and of nothing else - which is
 * the quantity the method is about, and which a zonal matrix cannot show separately, because at a 500 m
 * to 1 km cell the discretisation alone is already 5 to 8 % and it is all scatter, so it does not simply
 * add up with the congestion error. It costs one shortest-path tree per node - under a minute and about
 * 4.7 GB on the Dresden study area.
 * <p>
 * The references typically have <b>opposite signs</b>: against a router the matrix is too slow, because
 * it holds the free-speed path fixed, and against the experienced travel times it can be too fast,
 * because a network-wide index does not see the queue a particular trip sat in. Which is why they are a
 * list and not a choice.
 * <p>
 * Only one combination is impossible, and it is refused immediately rather than halfway through:
 * {@link Reference#EXPERIENCED} needs {@link ODPairs#TRIPS}, because a pair nobody drove has no
 * experienced travel time to be compared against.
 * <p>
 * Everything else is one code path: the choices only decide which columns of
 * {@link TravelTimeQuery} the references read, and {@link MatrixEvaluation} then subtracts. So every
 * table has the same columns whatever was chosen.
 * <p>
 * <b>What comes out</b> of {@code --output-directory}: {@code errors.csv} (the table - MAPE, bias and
 * RMSE per reference and variant, overall and by hour and distance band), {@code congestion_exposure.csv}
 * (how much congestion the queries carry against how much the index applies to them, which is what says
 * whether a bad row is the model's fault or the sample's), {@code od_queries.csv} (the questions, for
 * {@code --od-pairs FILE}), {@code congestion_index.csv} (the index that was used, next to the results it
 * produced) and, with {@code --write-queries}, one row per query and variant.
 *
 * @see MatrixEvaluation what is computed
 * @see CongestionExposure why it comes out that way
 * @see org.matsim.contrib.msp.traveltimematrix.RunComputeTravelTimeMatrix to build and cache the matrix
 * itself
 */
@CommandLine.Command(
	name = "evaluate-travel-time-matrix",
	description = "Measures the error of the time-varying travel time matrix against routing and against a run.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public final class RunEvaluateTravelTimeMatrix implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunEvaluateTravelTimeMatrix.class);

	/**
	 * What the matrix is compared against. More than one can be chosen, and normally is.
	 */
	public enum Reference {
		/**
		 * Routing on the network with no congestion at all: the exact free-speed travel time of the
		 * pair, as {@link RoutedTravelTimes} computes it. Isolates the error of the <b>zone system</b>,
		 * because nothing else differs between it and the free-speed variant of the matrix. Needs no
		 * events, and costs one shortest-path tree per distinct origin.
		 */
		FREE_SPEED_ROUTING,
		/**
		 * Time-dependent routing over the congested travel times of the run - see
		 * {@link RoutedTravelTimes}. The exact answer <em>under the model the matrix lives in</em>, so it
		 * isolates the error of the congestion approximation. Works with any pairs, and costs one
		 * shortest-path tree per distinct origin <em>and departure time</em>.
		 */
		CONGESTED_ROUTING,
		/**
		 * What the trips of the run really took, node to node, read from the events - see
		 * {@link ExperiencedLegs}. Contains everything the matrix cannot know: the route the agent chose,
		 * the queue it sat in, the exact minute it left. Needs {@link ODPairs#TRIPS}, or a
		 * {@link ODPairs#FILE} that carries measured travel times.
		 */
		EXPERIENCED,
		/**
		 * What a routing API answers for the same pairs. Not implemented - see {@link #apiReference}.
		 */
		API
	}

	/**
	 * Where the origin-destination pairs come from.
	 */
	public enum ODPairs {
		/**
		 * The car trips of the run, as the node-to-node legs they were driven as - see
		 * {@link ExperiencedLegs}. The <b>default</b>, because these are the pairs a volume-weighted
		 * congestion index is a statement about; see the class javadoc and {@link CongestionExposure}.
		 */
		TRIPS,
		/**
		 * Generated pairs, one destination per distance band and origin - see {@link ODSample}. Covers the
		 * matrix evenly, including the pairs and hours nobody drove - which for an analysis asking "could
		 * this person have reached that shop at 19:00" is most of them, and on which the congestion index
		 * overestimates badly, because most of such a pair runs over links the run never slowed down.
		 */
		RANDOM,
		/**
		 * The queries of an earlier run, read back from its {@code od_queries.csv}, so that two
		 * experiments can be compared on exactly the same questions.
		 */
		FILE
	}

	@CommandLine.Option(names = "--events", description = "Events of the run. Required unless neither the pairs "
		+ "nor any reference needs them: the congested travel times a CONGESTED_ROUTING reference routes on and "
		+ "the legs TRIPS and EXPERIENCED need are both read from them, in one pass.")
	private String eventsFile;

	@CommandLine.Option(names = "--congestion-index", description = "The congestion index csv, as written by "
		+ "RunComputeCongestionIndex, measured on the same events over the same study area.", required = true)
	private String congestionIndexFile;

	@CommandLine.Option(names = "--output-directory", description = "Directory the result csvs are written to.",
		defaultValue = "travel-time-matrix-evaluation")
	private String outputDirectory;

	@CommandLine.Option(names = "--od-pairs", description = "Where the origin-destination pairs come from: "
		+ "${COMPLETION-CANDIDATES}. TRIPS is the default because a generated pair is mostly made of links "
		+ "the run never put a vehicle on, so a volume-weighted congestion index overestimates it - see "
		+ "CongestionExposure.", defaultValue = "TRIPS")
	private ODPairs odPairs;

	@CommandLine.Option(names = "--references", description = "What the variants are compared against, one or "
		+ "more of ${COMPLETION-CANDIDATES}; every variant is reported against every one of them. "
		+ "FREE_SPEED_ROUTING isolates the error of the zone system, CONGESTED_ROUTING the error of the congestion "
		+ "model, EXPERIENCED the error an analysis really suffers (and needs --od-pairs TRIPS). Left out, it is "
		+ "all three with --od-pairs TRIPS and the two routed ones otherwise, EXPERIENCED being impossible there.",
		split = ",")
	private List<Reference> references;

	@CommandLine.Option(names = "--with-constant-index", description = "Also evaluate the daily mean congestion "
		+ "index as a constant factor - what DetourTimeEstimator of contribs/drt does today. It is the variant "
		+ "that says whether the time variation earns its keep, as opposed to congestion being modelled at all.")
	private boolean withConstantIndex;

	@CommandLine.Option(names = "--mode", description = "The network mode whose trips are used by TRIPS and "
		+ "EXPERIENCED.", defaultValue = TransportMode.car)
	private String mode;

	@CommandLine.Option(names = "--max-trips", description = "How many trips to keep with --od-pairs TRIPS, as a "
		+ "reservoir sample over the whole day. A MAPE needs far fewer than a city produces, and with a "
		+ "CONGESTED_ROUTING reference every trip costs a shortest-path tree.", defaultValue = "20000")
	private int maximumNumberOfTrips;

	/**
	 * The defaults of the two options that shape a generated sample, so that passing them together with
	 * {@code --od-pairs TRIPS} - where they do nothing - can be noticed and warned about.
	 */
	private static final int DEFAULT_ORIGINS = 200;
	private static final List<Integer> DEFAULT_DEPARTURE_HOURS = List.of(0, 3, 6, 7, 8, 9, 12, 15, 16, 17, 18, 21);

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

	@CommandLine.Option(names = "--calibration-factor", description = "beta of TT = TT_ff * (1 + beta * CI).",
		defaultValue = "1.0")
	private double calibrationFactor;

	@CommandLine.Option(names = "--evaluate-index-at", description = "When during the trip the index is read: "
		+ "${COMPLETION-CANDIDATES}.", defaultValue = "MIDPOINT")
	private TravelTimeMatrixConfigGroup.EvaluateIndexAt evaluateIndexAt;

	@CommandLine.Mixin
	private StudyAreaOptions studyArea = new StudyAreaOptions();

	@CommandLine.Mixin
	private MatrixOptions matrix = new MatrixOptions();

	public static void main(String[] args) {
		new RunEvaluateTravelTimeMatrix().execute(args);
	}

	@Override
	public Integer call() {

		Set<Reference> chosen = checkTheCombination();

		try {
			Files.createDirectories(Path.of(this.outputDirectory));
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create the output directory " + this.outputDirectory, e);
		}

		Network network = this.studyArea.readSubnetwork();

		// one pass over the events for everything that is read from them: the congested travel times a
		// routed reference needs, the volumes, and the legs the trips are taken from
		ExperiencedLegs legs = this.odPairs == ODPairs.TRIPS
			? new ExperiencedLegs(network, this.mode, this.maximumNumberOfTrips, this.seed)
			: null;
		MeasuredTraffic traffic = needsEvents(chosen)
			? MeasuredTraffic.readFromEvents(network, this.eventsFile, this.studyArea.getModes(), this.timeBinSize,
			this.endTime,
			legs == null ? new ExperiencedLegs[0] : new ExperiencedLegs[]{legs})
			: null;

		CongestionIndexProfile congestionIndex = CongestionIndexProfile.read(this.congestionIndexFile);

		List<TravelTimeQuery> asked = queries(network, legs, chosen);
		RoutedTravelTimes routed = routeIfNeeded(network, traffic, asked, chosen);
		List<TravelTimeQuery> answered = routed == null ? asked : routed.getQueries();

		double[] bands = this.distanceBands.stream().mapToDouble(Double::doubleValue).toArray();
		Map<String, ToDoubleFunction<TravelTimeQuery>> references = references(chosen, routed);
		Variants variants = buildVariants(network, congestionIndex, routed);

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(answered, references, variants.matrices(), bands,
			this.writeQueries ? output("queries.csv") : null);
		evaluation.writeErrors(output("errors.csv"));

		// the same queries and the same references, asked why rather than how wrong: how much congestion
		// is in these pairs at all, against how much the index applies to them
		CongestionExposure.measure(answered, references, variants.globalIndex(), bands)
			.write(output("congestion_exposure.csv"));

		TravelTimeQuery.write(answered, output("od_queries.csv"));
		congestionIndex.write(output("congestion_index.csv"));

		return 0;
	}

	/**
	 * The combinations of the two choices that cannot work, and the inputs each of them needs.
	 * <p>
	 * Checked before anything is read, because the alternative is to find out after the events, the
	 * network and the matrix - which on a city-sized scenario is a quarter of an hour of someone's
	 * afternoon.
	 *
	 * @return the chosen references, deduplicated, in the order they were given
	 */
	private Set<Reference> checkTheCombination() {

		if (this.references != null && this.references.isEmpty()) {
			throw new IllegalArgumentException("--references is empty, so there is nothing to compare the matrix "
				+ "against.");
		}

		Set<Reference> chosen = new LinkedHashSet<>(this.references == null ? defaultReferences() : this.references);

		if (chosen.contains(Reference.EXPERIENCED) && this.odPairs == ODPairs.RANDOM) {
			throw new IllegalArgumentException("--references " + Reference.EXPERIENCED + " cannot be used with "
				+ "--od-pairs " + ODPairs.RANDOM + ": a generated origin-destination pair was never driven, so "
				+ "there is no experienced travel time to compare the matrix against. Use --od-pairs "
				+ ODPairs.TRIPS + " to evaluate on the trips the run really made, or drop " + Reference.EXPERIENCED
				+ " and keep the routed references, which work on any pair.");
		}

		if (this.odPairs == ODPairs.FILE && this.odFile == null) {
			throw new IllegalArgumentException("--od-pairs " + ODPairs.FILE + " needs --od-file, the od_queries csv "
				+ "of an earlier run.");
		}

		if (this.odPairs != ODPairs.RANDOM && (this.numberOfOrigins != DEFAULT_ORIGINS
			|| !this.departureHours.equals(DEFAULT_DEPARTURE_HOURS))) {
			// --od-pairs is TRIPS by default now, and these two only shape a generated sample. Said once
			// here rather than left to be noticed in a result table whose sample size is not the one asked
			// for.
			log.warn("--origins and --departure-hours only shape a generated sample and are ignored with "
				+ "--od-pairs {}: a real trip comes with its own origin and its own departure second. Use "
				+ "--max-trips to size this sample, or --od-pairs {} to generate pairs.", this.odPairs,
				ODPairs.RANDOM);
		}

		if (this.matrix.isUnbounded()) {
			log.info("--max-neighbor-distance is {}, so the base matrix is exact: every pair is answered node to "
				+ "node, no zone system is built, and --cell-size is ignored. The discretisation error is then "
				+ "zero by construction, which is what makes this the configuration that isolates the congestion "
				+ "model.", MatrixOptions.UNBOUNDED);
		}

		if (needsEvents(chosen) && this.eventsFile == null) {
			throw new IllegalArgumentException("--events is needed for --od-pairs " + this.odPairs + " with "
				+ "--references " + chosen + ": the trips and the congested travel times are both read from them. "
				+ "Only queries read from a FILE, judged by references the file already carries, can do without.");
		}

		return chosen;
	}

	/**
	 * The references to use when {@code --references} was not given, which depends on where the pairs come
	 * from and is the one place the two choices are not independent.
	 * <p>
	 * With {@link ODPairs#TRIPS} it is all three, because all three are then possible and the costly one
	 * has already been paid for: the events are read either way, and a trip carries its experienced
	 * travel time for free. With anything else {@link Reference#EXPERIENCED} is left out, because a
	 * generated pair was never driven - the combination this command refuses - and a file may or may not
	 * carry measured travel times, so demanding them by default would turn a reusable query file into an
	 * error message.
	 */
	private List<Reference> defaultReferences() {
		return this.odPairs == ODPairs.TRIPS
			? List.of(Reference.FREE_SPEED_ROUTING, Reference.CONGESTED_ROUTING, Reference.EXPERIENCED)
			: List.of(Reference.FREE_SPEED_ROUTING, Reference.CONGESTED_ROUTING);
	}

	/**
	 * Whether anything in this run has to be read off the events: the trips, or the congested travel
	 * times a router routes on.
	 */
	private boolean needsEvents(Set<Reference> chosen) {
		return this.odPairs == ODPairs.TRIPS || chosen.contains(Reference.CONGESTED_ROUTING);
	}

	/**
	 * The questions to ask, with the experienced travel time already in them when they come from trips.
	 */
	private List<TravelTimeQuery> queries(Network network, ExperiencedLegs legs, Set<Reference> chosen) {

		List<TravelTimeQuery> queries = switch (this.odPairs) {
			case TRIPS -> legs.asQueries();
			case RANDOM -> ODSample.byDistanceBands(network, this.numberOfOrigins,
				this.distanceBands.stream().mapToDouble(Double::doubleValue).toArray(),
				this.departureHours.stream().mapToDouble(hour -> hour * 3600.0).toArray(), this.seed);
			case FILE -> TravelTimeQuery.read(this.odFile, network);
		};

		log.info("Evaluating on {} queries from {}, compared against {}.", queries.size(), this.odPairs, chosen);
		return queries;
	}

	/**
	 * Routes the queries if any chosen reference needs it, and only as far as it is needed: the congested
	 * trees - one per origin and departure time - are built only for
	 * {@link Reference#CONGESTED_ROUTING}, while a free-speed reference costs one tree per origin. A file
	 * that already carries the free-speed travel times of its pairs is not routed again.
	 */
	@Nullable
	private RoutedTravelTimes routeIfNeeded(Network network, MeasuredTraffic traffic,
		List<TravelTimeQuery> queries, Set<Reference> chosen) {

		boolean congested = chosen.contains(Reference.CONGESTED_ROUTING);
		boolean freeSpeed = chosen.contains(Reference.FREE_SPEED_ROUTING)
			&& !queries.stream().allMatch(TravelTimeQuery::hasFreeSpeed);

		if (!congested && !freeSpeed) {
			return null;
		}
		return RoutedTravelTimes.compute(network, queries, congested ? traffic.getTravelTimes() : null,
			this.matrix.getQSimTimeStepSize(), this.matrix.getNumberOfThreads());
	}

	/**
	 * Each chosen reference as the travel time it reads off a query, by the name it is reported under.
	 * They are all one-liners, and that is the design: the command decides what "right" means, and
	 * {@link MatrixEvaluation} only subtracts.
	 */
	private Map<String, ToDoubleFunction<TravelTimeQuery>> references(Set<Reference> chosen,
		RoutedTravelTimes routed) {

		Map<String, ToDoubleFunction<TravelTimeQuery>> references = new LinkedHashMap<>();

		for (Reference reference : chosen) {
			switch (reference) {
				case FREE_SPEED_ROUTING -> references.put(reference.name(), TravelTimeQuery::freeSpeedTravelTime);
				case CONGESTED_ROUTING -> references.put(reference.name(), query ->
					routed.getTimeDependentTravelTime(query.fromNode(), query.toNode(), query.departureTime()));
				case EXPERIENCED -> references.put(reference.name(), experiencedReference());
				case API -> apiReference();
			}
		}

		return references;
	}

	/**
	 * The travel times the trips really took, which the queries already carry - so this only reads them
	 * back. A query read from a file may not carry one, and {@link MatrixEvaluation} then says so,
	 * naming this reference.
	 */
	private ToDoubleFunction<TravelTimeQuery> experiencedReference() {
		return TravelTimeQuery::observedTravelTime;
	}

	/**
	 * <b>Not implemented.</b> The seam is here and it is deliberately narrow: everything else about this
	 * command is independent of where a reference travel time comes from, so a routing API means filling
	 * in the measured travel time of each query and nothing else.
	 * <p>
	 * The way to do it without writing an API client at all: run this command with
	 * {@code --od-pairs RANDOM --references FREE_SPEED_ROUTING}, take the {@code od_queries.csv} it
	 * writes - which is in the {@code from_node,to_node} shape {@code SampleValidationRoutes} of
	 * {@code contribs/application} reads - fetch the travel times with that command, write them into the
	 * {@code observed_travel_time} column, and come back with
	 * {@code --od-pairs FILE --references EXPERIENCED}. That path works today and needs no code; what is
	 * missing here is only the convenience of doing it in one go.
	 */
	private void apiReference() {
		throw new UnsupportedOperationException("--references " + Reference.API + " is not implemented yet. Fetch "
			+ "the travel times of the od_queries.csv this command writes with SampleValidationRoutes of "
			+ "contribs/application, fill in its observed_travel_time column, and evaluate them with --od-pairs "
			+ ODPairs.FILE + " --references " + Reference.EXPERIENCED + " - which is the same comparison, by hand.");
	}

	/**
	 * Every rung of the ladder, by the name it is reported under, in the order they are to be read: the
	 * routed ones first when they were computed, then up the ladder of the matrix from no congestion
	 * model at all to one index per road class.
	 * <p>
	 * The matrix variants deliberately share <b>one</b> free-speed base matrix, which
	 * {@link TimeVaryingTravelTimeMatrix#create} computes once per config and caches in
	 * {@code --output-directory}. They differ in the congestion model, so computing the base matrix
	 * three times would only buy three chances of it coming out differently.
	 */
	private Variants buildVariants(Network network, CongestionIndexProfile congestionIndex,
		@Nullable RoutedTravelTimes routed) {

		TravelTimeMatrixConfigGroup config = config();
		TravelTimeMatrix freeSpeedMatrix = TimeVaryingTravelTimeMatrix.create(network, config,
			this.matrix.getQSimTimeStepSize()).getFreeSpeedMatrix();

		Map<String, TravelTimeMatrix> variants = new LinkedHashMap<>();
		if (routed != null && routed.hasCongestion()) {
			variants.put(MatrixEvaluation.CONGESTED_ROUTING, routed.asTimeDependentMatrix());
			variants.put(MatrixEvaluation.FIXED_PATH, routed.asFixedPathMatrix());
		}
		variants.put(TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name(),
			TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix, CongestionIndex.freeFlow(), config));
		if (this.withConstantIndex) {
			variants.put(MatrixEvaluation.CONSTANT_INDEX, TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix,
				CongestionIndex.constant(congestionIndex.getDailyMean(RoadClassification.ALL)), config));
		}
		TimeVaryingTravelTimeMatrix globalIndex = TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix,
			congestionIndex, config);
		variants.put(TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX.name(), globalIndex);

		log.info("Evaluating {}.", variants.keySet());
		return new Variants(variants, globalIndex);
	}

	/**
	 * The rungs to evaluate, and level 0 by itself.
	 * <p>
	 * Level 0 is handed out separately because {@link CongestionExposure} needs two numbers of it that are
	 * not on the {@link TravelTimeMatrix} interface the rest of the evaluation speaks - the free-speed
	 * travel time of a pair and the congestion factor applied to it - and because it is the rung whose
	 * error the exposure explains. It is the same object that is in the map, not a second one built the
	 * same way: the two tables have to describe one matrix.
	 *
	 * @param matrices every rung, by the name it is reported under
	 * @param globalIndex level 0, which is always among them
	 */
	private record Variants(Map<String, TravelTimeMatrix> matrices, TimeVaryingTravelTimeMatrix globalIndex) {
	}

	/**
	 * The config the base matrix is built from.
	 * <p>
	 * The variant set here is the <b>top</b> rung, because every lower rung is this one's base matrix
	 * wrapped in a different congestion index and the base matrix is to be built once - see
	 * {@link #buildVariants}.
	 */
	private TravelTimeMatrixConfigGroup config() {
		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setVariant(TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX);
		config.setCongestionIndexFile(this.congestionIndexFile);
		config.setCalibrationFactor(this.calibrationFactor);
		config.setEvaluateIndexAt(this.evaluateIndexAt);
		config.setTimeBinSize(this.timeBinSize);
		config.setEndTime(this.endTime);
		config.setMatrixCacheDirectory(this.outputDirectory);
		return this.matrix.applyTo(config);
	}

	private String output(String file) {
		return Path.of(this.outputDirectory, file).toString();
	}
}
