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
 * Validates the matrix against the car legs a run actually drove:
 * <pre>
 *   java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.evaluation.RunValidateTravelTimeMatrix \
 *       --network output_network.xml.gz --events output_events.xml.gz --shp dresden-with-buffer.shp \
 *       --congestion-index congestion_index.csv --output-directory validation
 * </pre>
 * This is the second of the two comparisons, and the one to report as "how well does it work". Its
 * sibling {@link RunEvaluateTravelTimeMatrix} measures the matrix against a time-dependent router on
 * the same travel times, which isolates the error of the approximation; this one measures it against
 * what the simulation did, which is the error an analysis using the matrix actually suffers. The second
 * is always the larger of the two, and the difference between them is everything the matrix cannot know:
 * the route the agent chose, the queue it sat in, the exact minute it left.
 * <p>
 * Both write the same {@code errors.csv}, by the same {@link MatrixEvaluation}, so the two tables can be
 * put side by side.
 * <p>
 * No routing happens here, which is what makes it cheap enough to run on every leg of a run rather than
 * on a sample of pairs: the events are read once, every car leg is reduced to the node-to-node interval
 * the matrix is about (see {@link ExperiencedLegs}), and each variant is asked one question per leg.
 *
 * @see ExperiencedLegs for what exactly is compared, and why it is not a leg's own travel time
 */
@CommandLine.Command(
	name = "validate-travel-time-matrix",
	description = "Compares the travel time matrix against the car legs a run actually drove.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public final class RunValidateTravelTimeMatrix implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunValidateTravelTimeMatrix.class);

	@CommandLine.Option(names = "--network", description = "Network of the run.", required = true)
	private String networkFile;

	@CommandLine.Option(names = "--events", description = "Events of the run. Both the congested travel times and "
		+ "the legs that are validated are read from them, in one pass.", required = true)
	private String eventsFile;

	@CommandLine.Option(names = "--congestion-index", description = "The congestion index csv, as written by "
		+ "RunComputeCongestionIndex, measured on the same events over the same study area.", required = true)
	private String congestionIndexFile;

	@CommandLine.Option(names = "--output-directory", description = "Directory the result csvs are written to.",
		defaultValue = "travel-time-matrix-validation")
	private String outputDirectory;

	@CommandLine.Option(names = "--modes", description = "Modes of the subnetwork and of the travel times.",
		defaultValue = "car", split = ",")
	private Set<String> modes;

	@CommandLine.Option(names = "--mode", description = "The network mode whose legs are validated.",
		defaultValue = TransportMode.car)
	private String mode;

	@CommandLine.Option(names = "--max-legs", description = "How many legs to keep, as a reservoir sample over the "
		+ "whole day. A MAPE needs far fewer than a city produces.", defaultValue = "100000")
	private int maximumNumberOfLegs;

	@CommandLine.Option(names = "--distance-bands", description = "Lower edges of the distance bands the error is "
		+ "reported by [m].", defaultValue = "0,2000,5000,10000,20000", split = ",")
	private List<Double> distanceBands;

	@CommandLine.Option(names = "--write-legs", description = "Also write one row per leg and variant. Large: as "
		+ "many rows as kept legs times variants.")
	private boolean writeLegs;

	@CommandLine.Option(names = "--seed", description = "Seed of the leg sampling.", defaultValue = "4711")
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

	@CommandLine.Option(names = "--threads", description = "Threads for the matrix.")
	private Integer numberOfThreads;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	public static void main(String[] args) {
		new RunValidateTravelTimeMatrix().execute(args);
	}

	@Override
	public Integer call() {

		try {
			Files.createDirectories(Path.of(this.outputDirectory));
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create the output directory " + this.outputDirectory, e);
		}

		Network fullNetwork = NetworkUtils.readNetwork(this.networkFile);
		Network network = StudyAreaNetwork.cut(fullNetwork, StudyAreaNetwork.index(this.shp, fullNetwork), this.modes);

		// one pass over the events for both: the travel times the index was measured from, and the legs
		// the index is now judged on
		ExperiencedLegs legs = new ExperiencedLegs(network, this.mode, this.maximumNumberOfLegs, this.seed);
		MeasuredTraffic.readFromEvents(network, this.eventsFile, this.modes, this.timeBinSize, this.endTime, legs);

		CongestionIndexProfile congestionIndex = CongestionIndexProfile.read(this.congestionIndexFile);
		List<MatrixEvaluation.Query> queries = legs.asQueries();

		MatrixEvaluation evaluation = MatrixEvaluation.evaluate(queries, buildVariants(network, congestionIndex),
			this.distanceBands.stream().mapToDouble(Double::doubleValue).toArray(),
			this.writeLegs ? output("legs.csv") : null);

		evaluation.writeErrors(output("errors.csv"));
		congestionIndex.write(output("congestion_index.csv"));

		log.info("{} of {} {} legs of the run were used; the rest left the study area or were too short to have a "
				+ "node-to-node travel time.", queries.size(), legs.getNumberOfLegsSeen(), this.mode);
		return 0;
	}

	/**
	 * The matrix variants, sharing one free-speed base matrix. The routing rungs of
	 * {@link RunEvaluateTravelTimeMatrix} are not here: they would need a shortest-path tree per leg
	 * departure time, which is what this comparison trades away in exchange for using every leg.
	 */
	private Map<String, TravelTimeMatrix> buildVariants(Network network, CongestionIndexProfile congestionIndex) {

		TravelTimeMatrixConfigGroup config = config();
		TimeVaryingTravelTimeMatrix byRoadClass = TimeVaryingTravelTimeMatrix.create(network, config,
			this.qSimTimeStepSize);
		TravelTimeMatrix freeSpeedMatrix = byRoadClass.getFreeSpeedMatrix();

		Map<String, TravelTimeMatrix> variants = new LinkedHashMap<>();
		variants.put(TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS.name(), byRoadClass);
		variants.put(TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX.name(),
			TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix, congestionIndex, config));
		variants.put(MatrixEvaluation.CONSTANT_INDEX, TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix,
			CongestionIndex.constant(congestionIndex.getDailyMean(RoadClassification.ALL)), config));
		variants.put(TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name(),
			TimeVaryingTravelTimeMatrix.global(freeSpeedMatrix, CongestionIndex.freeFlow(), config));

		log.info("Validating {}.", variants.keySet());
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
		config.setMatrixCacheDirectory(this.outputDirectory);
		if (this.numberOfThreads != null) {
			config.setNumberOfThreads(this.numberOfThreads);
		}
		return config;
	}

	private String output(String file) {
		return Path.of(this.outputDirectory, file).toString();
	}
}
