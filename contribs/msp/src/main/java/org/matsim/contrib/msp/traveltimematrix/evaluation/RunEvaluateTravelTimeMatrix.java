package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix;
import org.matsim.contrib.msp.traveltimematrix.TravelTimeMatrixConfigGroup;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndex;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile;
import org.matsim.contrib.msp.traveltimematrix.prepare.MeasuredTraffic;
import org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaNetwork;
import org.matsim.contrib.msp.traveltimematrix.RoadClassification;
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
 * Evaluates the variants of the time-varying travel time matrix against time-dependent routing on the
 * same run, and writes the error tables:
 * <pre>
 *   java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix \
 *       --network output_network.xml.gz --events output_events.xml.gz --shp dresden-with-buffer.shp \
 *       --congestion-index congestion_index.csv --output-directory evaluation
 * </pre>
 * It is the experiment of the paper in one command, and it is deliberately one command: the reference,
 * the congestion index and the matrix all have to be built over the <b>same</b> network and the same
 * run, and splitting them up is how they come to disagree. The network is cut once, the events are read
 * once ({@link MeasuredTraffic}), and everything downstream is handed those two.
 * <p>
 * Four files come out of {@code --output-directory}:
 * <ul>
 *     <li>{@code od_sample.csv} - the queries, so that the experiment can be repeated or the same pairs
 *     handed to a route API.</li>
 *     <li>{@code queries.csv} - one row per query and variant.</li>
 *     <li>{@code errors.csv} - MAPE, bias and RMSE per variant, by time of day and distance band. This
 *     is the table.</li>
 *     <li>{@code congestion_index.csv} - the index that was used, copied next to the results it
 *     produced.</li>
 * </ul>
 * The matrix variants are built from one {@link TravelTimeMatrixConfigGroup}, so a sweep over
 * {@code cellSize}, {@code maxNeighborDistance}, {@code calibrationFactor} or {@code evaluateIndexAt}
 * is a sweep over command line options and not over code.
 *
 * @see MatrixEvaluation what is computed
 * @see RoutedTravelTimes the reference
 * @see RunValidateTravelTimeMatrix the other comparison: against the legs the run actually drove, which
 * is the error an analysis using the matrix really suffers
 */
@CommandLine.Command(
	name = "evaluate-travel-time-matrix",
	description = "Compares the variants of the time-varying travel time matrix against time-dependent routing.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public final class RunEvaluateTravelTimeMatrix implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunEvaluateTravelTimeMatrix.class);

	@CommandLine.Option(names = "--network", description = "Network of the run.", required = true)
	private String networkFile;

	@CommandLine.Option(names = "--events", description = "Events of the run, the congested travel times the "
		+ "reference routes on are read from.", required = true)
	private String eventsFile;

	@CommandLine.Option(names = "--congestion-index", description = "The congestion index csv, as written by "
		+ "RunComputeCongestionIndex. Measured on the same events, over the same study area.", required = true)
	private String congestionIndexFile;

	@CommandLine.Option(names = "--output-directory", description = "Directory the result csvs are written to.",
		defaultValue = "travel-time-matrix-evaluation")
	private String outputDirectory;

	@CommandLine.Option(names = "--modes", description = "Modes of the subnetwork and of the travel times.",
		defaultValue = "car", split = ",")
	private Set<String> modes;

	@CommandLine.Option(names = "--origins", description = "Number of origins to sample. Each of them costs one "
		+ "shortest-path tree per departure time.", defaultValue = "200")
	private int numberOfOrigins;

	@CommandLine.Option(names = "--departure-hours", description = "Hours of the day to query every pair at.",
		defaultValue = "0,3,6,7,8,9,12,15,16,17,18,21", split = ",")
	private List<Integer> departureHours;

	@CommandLine.Option(names = "--distance-bands", description = "Lower edges of the distance bands [m]. One "
		+ "destination is sampled per band and origin, and the error is reported per band.",
		defaultValue = "0,2000,5000,10000,20000", split = ",")
	private List<Double> distanceBands;

	@CommandLine.Option(names = "--od-sample", description = "Read the queries from this csv instead of sampling "
		+ "them, e.g. to evaluate twice on exactly the same pairs.")
	private String odSampleFile;

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

	@CommandLine.Option(names = "--threads", description = "Threads for the matrix and the reference.")
	private Integer numberOfThreads;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	public static void main(String[] args) {
		new RunEvaluateTravelTimeMatrix().execute(args);
	}

	@Override
	public Integer call() {

		createOutputDirectory();

		Network fullNetwork = NetworkUtils.readNetwork(this.networkFile);
		Network network = StudyAreaNetwork.cut(fullNetwork, StudyAreaNetwork.index(this.shp, fullNetwork), this.modes);

		MeasuredTraffic traffic = MeasuredTraffic.readFromEvents(network, this.eventsFile, this.modes,
			this.timeBinSize, this.endTime);
		CongestionIndexProfile congestionIndex = CongestionIndexProfile.read(this.congestionIndexFile);

		ODSample sample = sample(network);
		RoutedTravelTimes reference = RoutedTravelTimes.compute(network, sample, traffic.getTravelTimes(),
			this.qSimTimeStepSize, threads());

		Map<String, TravelTimeMatrix> variants = buildVariants(network, congestionIndex, reference);

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(reference, variants,
			this.distanceBands.stream().mapToDouble(Double::doubleValue).toArray(),
			output("queries.csv"));

		sample.write(output("od_sample.csv"));
		congestionIndex.write(output("congestion_index.csv"));
		evaluation.writeErrors(output("errors.csv"));

		return 0;
	}

	/**
	 * The result files and the matrix cache all go into {@code --output-directory}, so it has to exist
	 * before the first of them is written - and the matrix cache is written in the middle of the run,
	 * after the expensive part.
	 */
	private void createOutputDirectory() {
		try {
			Files.createDirectories(Path.of(this.outputDirectory));
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create the output directory " + this.outputDirectory, e);
		}
	}

	/**
	 * The queries: read from a file when one is given, so that two runs of this command can be compared
	 * on exactly the same pairs, and sampled by distance band otherwise.
	 *
	 * @see ODSample#byDistanceBands(Network, int, double[], double[], long) why not uniformly
	 */
	private ODSample sample(Network network) {

		double[] departureTimes = this.departureHours.stream().mapToDouble(hour -> hour * 3600.0).toArray();

		if (this.odSampleFile != null) {
			log.info("Reading the queries from {} instead of sampling them; --origins, --distance-bands, "
				+ "--departure-hours and --seed are then ignored.", this.odSampleFile);
			return ODSample.read(this.odSampleFile, network);
		}

		return ODSample.byDistanceBands(network, this.numberOfOrigins,
			this.distanceBands.stream().mapToDouble(Double::doubleValue).toArray(), departureTimes, this.seed);
	}

	/**
	 * Every rung of the ladder, by the name it is reported under, in the order they are to be read: the
	 * reference first - whose error is the rounding of a travel time to whole seconds and nothing else,
	 * which is the check that the comparison is wired up correctly - then down the ladder to the
	 * free-speed matrix.
	 * <p>
	 * The three matrix variants deliberately share <b>one</b> free-speed base matrix, which
	 * {@link TimeVaryingTravelTimeMatrix#create} computes once per config and caches in
	 * {@code --output-directory}. They differ in the congestion model, so computing the base matrix
	 * three times would only buy three chances of it coming out differently.
	 */
	private Map<String, TravelTimeMatrix> buildVariants(Network network, CongestionIndexProfile congestionIndex,
		RoutedTravelTimes reference) {

		TravelTimeMatrixConfigGroup config = config(TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS);
		TimeVaryingTravelTimeMatrix byRoadClass = TimeVaryingTravelTimeMatrix.create(network, config,
			this.qSimTimeStepSize);
		TravelTimeMatrix freeSpeedMatrix = byRoadClass.getFreeSpeedMatrix();

		Map<String, TravelTimeMatrix> variants = new LinkedHashMap<>();
		variants.put(MatrixEvaluation.REFERENCE, reference.asTimeDependentMatrix());
		variants.put(MatrixEvaluation.FIXED_PATH, reference.asFixedPathMatrix());
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

	/**
	 * The config of one variant, with the matrix parameters of the command line. The cache file is in
	 * the output directory and is shared by the variants, which is what keeps the base matrix a single
	 * computation.
	 */
	private TravelTimeMatrixConfigGroup config(TravelTimeMatrixConfigGroup.Variant variant) {
		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setVariant(variant);
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
