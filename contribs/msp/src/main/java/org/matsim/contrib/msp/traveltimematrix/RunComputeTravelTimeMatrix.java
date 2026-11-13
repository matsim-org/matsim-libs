package org.matsim.contrib.msp.traveltimematrix;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.accessibility.utils.NetworkUtil;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.msp.traveltimematrix.evaluation.ODSample;
import org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery;
import org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaOptions;
import org.matsim.core.utils.io.IOUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Builds the time-varying travel time matrix for a study area, caches it, and reports what it cost:
 * <pre>
 *   java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.RunComputeTravelTimeMatrix \
 *       --network output_network.xml.gz --shp dresden-with-buffer.shp \
 *       --congestion-index congestion_index.csv --output matrix
 * </pre>
 * The matrix itself is a library object, not a file - an analysis builds it with
 * {@link TimeVaryingTravelTimeMatrix#create(Network, TravelTimeMatrixConfigGroup, double)} and queries
 * it in memory. What this command is for is the two things one does <b>before</b> that:
 * <ul>
 *     <li><b>Cache the expensive part.</b> The free-speed base matrix is the only slow step, it does not
 *     depend on the congestion index, and it is binary-cacheable. Computing it once here means every
 *     later run - the evaluation, a parameter sweep, the analysis itself - reads
 *     {@code free_speed_matrix.bin} instead of routing the whole zone system again.</li>
 *     <li><b>Find out whether the thresholds fit.</b> Memory grows with the square of the number of
 *     zones and with the number of node pairs inside {@code maxNeighborDistance}, and on a real network
 *     neither is obvious beforehand. The summary csv is one row per setting, so a sweep over
 *     {@code --cell-size} and {@code --max-neighbor-distance} produces the table that decides them.</li>
 * </ul>
 * The query benchmark at the end is the other half of that table: a matrix is worth its memory because a
 * query is a lookup, and this is the number to put next to the routing it replaces.
 *
 * @see RunComputeCongestionIndex for the index this needs, which comes first
 * @see org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix to find out how
 * good the result is
 */
@CommandLine.Command(
	name = "travel-time-matrix",
	description = "Builds and caches the time-varying travel time matrix of a study area.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public final class RunComputeTravelTimeMatrix implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunComputeTravelTimeMatrix.class);

	@CommandLine.Option(names = "--congestion-index", description = "The congestion index csv, as written by "
		+ "RunComputeCongestionIndex. Without it only the FREE_SPEED variant can be built.")
	private String congestionIndexFile;

	@CommandLine.Option(names = "--output", description = "Directory for the matrix cache and the summary.",
		defaultValue = "travel-time-matrix")
	private String outputDirectory;

	@CommandLine.Option(names = "--variant", description = "Which variant to build: ${COMPLETION-CANDIDATES}.",
		defaultValue = "GLOBAL_INDEX")
	private TravelTimeMatrixConfigGroup.Variant variant;

	@CommandLine.Option(names = "--benchmark-queries", description = "How many random queries to time at the end. "
		+ "0 skips the benchmark.", defaultValue = "1000000")
	private int benchmarkQueries;

	@CommandLine.Mixin
	private StudyAreaOptions studyArea = new StudyAreaOptions();

	@CommandLine.Mixin
	private MatrixOptions matrix = new MatrixOptions();

	/**
	 * How many zones the configured cell size gives over the study area network, or 0 when the
	 * neighbourhood is unbounded and there is no zone system at all. Counted for the summary only - the
	 * matrix builds its own zone system from the same parameters, and a square grid over the nodes costs
	 * a pass over them.
	 */
	private int zones;

	public static void main(String[] args) {
		new RunComputeTravelTimeMatrix().execute(args);
	}

	@Override
	public Integer call() {

		try {
			Files.createDirectories(Path.of(this.outputDirectory));
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create the output directory " + this.outputDirectory, e);
		}

		Network network = this.studyArea.readSubnetwork();

		TravelTimeMatrixConfigGroup config = config();
		this.zones = config.isUnboundedNeighborhood()
			? 0
			: ZoneSystemUtils.createZoneSystem(network, config.getMatrixParams().getZoneSystemParams())
			.getZones().size();

		long startedAt = System.currentTimeMillis();
		TimeVaryingTravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.create(network, config,
			this.matrix.getQSimTimeStepSize());
		double seconds = (System.currentTimeMillis() - startedAt) / 1000.0;

		log.info("Built {} in {} s.", matrix, String.format(Locale.ROOT, "%.1f", seconds));
		if (config.getMatrixCacheFile() == null) {
			log.info("The base matrix is not cached: an unbounded maxNeighborDistance holds a travel time per node "
				+ "pair, which the cache format of contribs/dvrp cannot express - it starts with the zone system "
				+ "this configuration has none of - and which it is cheaper to route again than to write out.");
		} else {
			log.info("The free-speed base matrix is cached in {}; point 'matrixCacheFile' of the '{}' config group "
					+ "at it and every later run reads it instead of routing again.", config.getMatrixCacheFile(),
				TravelTimeMatrixConfigGroup.GROUP_NAME);
		}

		double microsecondsPerQuery = benchmark(network, matrix);
		writeSummary(network, config, matrix, seconds, microsecondsPerQuery);

		return 0;
	}

	private TravelTimeMatrixConfigGroup config() {

		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setVariant(this.variant);
		config.setCongestionIndexFile(this.congestionIndexFile);
		config.setMatrixCacheDirectory(this.outputDirectory);
		return this.matrix.applyTo(config);
	}

	/**
	 * Times random queries on the finished matrix. Random on purpose: a benchmark over one origin would
	 * measure a warm cache line and not what an analysis does, which is to ask about pairs all over the
	 * study area in no particular order.
	 *
	 * @return microseconds per query, or {@link Double#NaN} if the benchmark was switched off
	 */
	private double benchmark(Network network, TimeVaryingTravelTimeMatrix matrix) {

		if (this.benchmarkQueries <= 0) {
			return Double.NaN;
		}

		// a few thousand random pairs, cycled: enough that the queries jump around the study area the way
		// an analysis does, few enough that the list of them is not what is being measured
		int side = Math.min(100, network.getNodes().size() - 1);
		List<TravelTimeQuery> pairs = ODSample.uniform(network, side, side, new double[]{8 * 3600.0}, 1);

		long answered = 0;
		long checksum = 0;
		long startedAt = System.nanoTime();
		while (answered < this.benchmarkQueries) {
			for (TravelTimeQuery pair : pairs) {
				// a time of day per query, so that the index lookup is part of what is measured
				checksum += matrix.getTravelTime(pair.fromNode(), pair.toNode(), answered % 86400);
				if (++answered >= this.benchmarkQueries) {
					break;
				}
			}
		}
		double microseconds = (System.nanoTime() - startedAt) / 1000.0 / answered;

		log.info("{} queries in {} us each (mean travel time {} s, which is only here to keep the compiler from "
				+ "optimising the benchmark away).", answered, String.format(Locale.ROOT, "%.3f", microseconds),
			checksum / answered);
		return microseconds;
	}

	/**
	 * One row of the table that decides {@code cellSize} and {@code maxNeighborDistance}: what was built,
	 * how long it took, how much it holds and how fast it answers. Appended to, so that a sweep over the
	 * thresholds writes one file.
	 */
	private void writeSummary(Network network, TravelTimeMatrixConfigGroup config,
		TimeVaryingTravelTimeMatrix matrix, double seconds, double microsecondsPerQuery) {

		Path file = Path.of(this.outputDirectory, "matrix_summary.csv");
		boolean exists = Files.exists(file);

		long zonalBytes = (long) this.zones * this.zones * Short.BYTES;
		long cacheBytes = cacheFileSize(config);

		try (Writer writer = IOUtils.getAppendingBufferedWriter(file.toString());
			 CSVPrinter printer = new CSVPrinter(writer, exists
				 ? CSVFormat.DEFAULT
				 : CSVFormat.DEFAULT.builder()
				 .setHeader("variant", "cell_size", "max_neighbor_distance", "links", "nodes", "zones",
					 "zonal_matrix_bytes", "cache_file_bytes", "build_seconds", "microseconds_per_query")
				 .build())) {

			printer.printRecord(this.variant, cellSizeLabel(), neighborhoodLabel(),
				network.getLinks().size(), network.getNodes().size(), this.zones, zonalBytes, cacheBytes,
				String.format(Locale.ROOT, "%.1f", seconds),
				Double.isNaN(microsecondsPerQuery) ? "" : String.format(Locale.ROOT, "%.3f", microsecondsPerQuery));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		log.info("Zonal part {} MB over {} zones (none when the neighbourhood is unbounded), cache file {} MB. "
			+ "Wrote the summary to {}.", zonalBytes / 1024 / 1024, this.zones, cacheBytes / 1024 / 1024, file);
	}

	/**
	 * The neighbourhood radius as the summary reports it: {@value MatrixOptions#UNBOUNDED} rather than a
	 * number, because {@code (long)} of an infinity is {@link Long#MAX_VALUE} and a reader of the csv
	 * would take that for a radius.
	 */
	private Object neighborhoodLabel() {
		return this.matrix.isUnbounded() ? MatrixOptions.UNBOUNDED : (long) this.matrix.getMaxNeighborDistance();
	}

	/**
	 * The cell size, left empty when no zone system was built - a number there would invite a comparison
	 * along an axis that cannot move the result.
	 */
	private Object cellSizeLabel() {
		return this.matrix.isUnbounded() ? "" : (long) this.matrix.getCellSize();
	}

	private long cacheFileSize(TravelTimeMatrixConfigGroup config) {
		if (config.getMatrixCacheFile() == null) {
			return 0;
		}
		try {
			Path cache = Path.of(config.getMatrixCacheFile());
			return Files.exists(cache) ? Files.size(cache) : 0;
		} catch (IOException e) {
			return 0;
		}
	}
}
