package org.matsim.contrib.msp.traveltimematrix.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix;
import org.matsim.contrib.msp.traveltimematrix.TravelTimeMatrixConfigGroup;
import org.matsim.contrib.msp.traveltimematrix.evaluation.ODSample;
import org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery;
import org.matsim.contrib.msp.traveltimematrix.experiments.HeapProbe;
import org.matsim.contrib.msp.traveltimematrix.experiments.MatrixFootprint;
import org.matsim.contrib.msp.traveltimematrix.experiments.ParallelQueryBenchmark;
import org.matsim.contrib.msp.traveltimematrix.experiments.ResultTable;
import org.matsim.contrib.msp.traveltimematrix.experiments.RoutingBaseline;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import org.matsim.core.router.speedy.SpeedyGraph;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.function.ToIntBiFunction;

/**
 * How long one batch of origin-destination queries takes, on a ladder of thread counts, next to the build
 * that produced the matrix - <b>all in one job on one machine</b>.
 * <pre>
 *   java -Xmx16g -cp matsim-msp-experiments.jar \
 *       org.matsim.contrib.msp.traveltimematrix.run.RunQueryBenchmark \
 *       --network networks/study-area_area.xml.gz --max-neighbor-distance inf \
 *       --batch-sizes 10000,100000,1000000 --thread-counts 1,2,4,8 --threads 8 \
 *       --output output/query-benchmark
 * </pre>
 * <p>
 * <b>Why this is a command of its own and not another column of the sweep.</b> The cost sweep
 * ({@link RunMatrixCostExperiment}) answers how much a query costs on average, driving one thread to a
 * hundred million of them until the rate settles; that is the number a break-even calculation needs and it
 * is deliberately single-threaded, because the matrix and the router both parallelise per query and a
 * ratio between them does not change when both are given eight threads. What it cannot say is how long a
 * caller waits for <em>ten thousand</em> queries on the eight threads a job was given - a batch that small
 * is a few hundred microseconds of work, so the thread pool and the memory hierarchy are a visible share of
 * it, and dividing a settled rate by eight is not an answer. Asking that question needs its own
 * command because it needs its own sampling: a handful of thread counts and batch sizes, repeated, rather
 * than one long run.
 * <p>
 * <b>And why the build is in here too</b>, when {@link RunMatrixCostExperiment} already measures it. The
 * two numbers are only comparable if they were measured on the same hardware, and on a cluster the node a
 * job lands on is not chosen by the person submitting it. Rebuilding the matrix here costs a minute on a
 * city network and removes the whole question: the {@code build_seconds} of this run and the batch times
 * below it came out of the same JVM on the same cpus, so the row can be quoted as it stands. The
 * alternative - calibrating a batch measured on one machine against a build measured on another - needs a
 * measurement the two have in common and still only produces an estimate, which is what the
 * {@code ONE_THREAD} rows are for: they are the quantity the cost sweep also reports, so a reader can see
 * how this machine compares to the one the sweep ran on.
 * <p>
 * <b>The three answerers</b> are the ones the sweep uses, for the same reason: a batch time for the matrix
 * means nothing without the same batch answered by the router it replaces. {@code DIRECT_ROUTING} runs one
 * shortest-path search per query and {@code DIRECT_ROUTING_BY_ORIGIN} reuses the tree while the origin does
 * not change, which is the same router used as well as it can be used. Both are given their own batch sizes
 * ({@code --routing-batch-sizes}), because a batch of a million searches would take days. The fourth,
 * {@code DISPATCH_FLOOR}, answers nothing: it is the cost of the loop and the thread pool, so that the
 * share of a small batch that is not the matrix is a row of the table rather than an argument about it.
 * <p>
 * <b>One row to read with care.</b> {@code DIRECT_ROUTING_BY_ORIGIN} amortises a tree over the queries of
 * one origin, so what it costs per query depends on how many queries of the same origin a thread gets -
 * and splitting a batch over threads cuts the runs of equal origin at the seams. The effect is small when
 * a thread's share is many times the number of destinations per origin (a batch of ten thousand over
 * eight threads is 1,250 queries each against 100 destinations per origin, so about 4% of the trees are
 * extra) and large when it is not (a batch of a thousand is 125 each, and then half the queries of a
 * thread are in a run of their own). Read the largest batch of that answerer, or raise
 * {@code --routing-batch-sizes}; the matrix rows have no such term, since a lookup amortises nothing.
 *
 * @see ParallelQueryBenchmark what one row is measured by
 * @see RunMatrixCostExperiment the mean cost of a query, and the break-even against a router
 */
@CommandLine.Command(
	name = "query-benchmark",
	description = "Times a batch of origin-destination queries on several threads, against the routers the "
		+ "matrix replaces, in the same job as the build it is compared to.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public final class RunQueryBenchmark implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunQueryBenchmark.class);

	/**
	 * What answered a batch. The three the cost sweep uses, plus the floor.
	 */
	private enum Answerer {
		MATRIX, DIRECT_ROUTING, DIRECT_ROUTING_BY_ORIGIN, DISPATCH_FLOOR
	}

	private static final List<String> HEADER = List.of("answerer", "links", "nodes", "cell_size",
		"max_neighbor_distance", "build_seconds", "memory_bytes", "threads", "queries", "repetitions",
		"seconds_median", "seconds_min", "seconds_max", "microseconds_per_query", "queries_per_second",
		"speedup_over_one_thread");

	@CommandLine.Option(names = "--output", description = "Directory query_benchmark.csv is written to.",
		defaultValue = "output/query-benchmark")
	private String output;

	@CommandLine.Option(names = "--batch-sizes", description = "Batch sizes to time for the matrix: how many "
		+ "queries one measurement answers in total, over all its threads.", split = ",",
		defaultValue = "10000,100000,1000000,10000000")
	private List<Long> batchSizes;

	@CommandLine.Option(names = "--routing-batch-sizes", description = "Batch sizes for the two routing "
		+ "baselines, which are four orders of magnitude slower per query and need their own, smaller ladder. "
		+ "Empty skips the baselines altogether.", split = ",", defaultValue = "1000,10000")
	private List<Long> routingBatchSizes;

	@CommandLine.Option(names = "--thread-counts", description = "Thread counts to time every batch size on. "
		+ "Include 1: it is what makes the speedup column a ratio, and it is the number the cost sweep "
		+ "reports, so it is also what lets this run be compared to one on another machine.", split = ",",
		defaultValue = "1,2,4,8")
	private List<Integer> threadCounts;

	@CommandLine.Option(names = "--repetitions", description = "How many times each measurement is repeated. "
		+ "The median is reported and the spread with it - a sub-millisecond batch is noisy and the table "
		+ "should say so.", defaultValue = "11")
	private int repetitions;

	@CommandLine.Option(names = "--routing-repetitions", description = "Repetitions for the routing baselines, "
		+ "whose measurements are seconds rather than microseconds and need far fewer.", defaultValue = "3")
	private int routingRepetitions;

	@CommandLine.Option(names = "--query-pool", description = "How many distinct pairs the benchmark cycles. "
		+ "Large enough that a batch is not one row of the matrix read over and over, small enough that the "
		+ "pool itself is not the memory being measured.", defaultValue = "10000")
	private int queryPoolSize;

	@CommandLine.Option(names = "--seed", description = "Seed of the sampling.", defaultValue = "4711")
	private long seed;

	@CommandLine.Mixin
	private MatrixOptions matrix = new MatrixOptions();

	@CommandLine.Mixin
	private StudyAreaOptions studyArea = new StudyAreaOptions();

	public static void main(String[] args) {
		new RunQueryBenchmark().execute(args);
	}

	@Override
	public Integer call() {

		if (this.threadCounts.stream().noneMatch(threads -> threads == 1)) {
			log.warn("--thread-counts does not include 1, so there is nothing to compute a speedup against and "
				+ "nothing in this table can be compared to a run on another machine.");
		}

		Path directory = Path.of(this.output);
		try {
			Files.createDirectories(directory);
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create the output directory " + directory, e);
		}
		ResultTable table = new ResultTable(directory.resolve("query_benchmark.csv"), HEADER);

		Network network = this.studyArea.readSubnetwork();
		List<TravelTimeQuery> pool = pool(network);

		// built here, and timed, so that the build and the batches below it are one measurement on one
		// machine - which is the whole reason this command exists. See the class javadoc.
		log.info("--- building the matrix ({}) ---", describeShape());
		HeapProbe.Result<TravelTimeMatrix> built = HeapProbe.measure(() ->
			TimeVaryingTravelTimeMatrix.create(network, config(), this.matrix.getQSimTimeStepSize())
				.getFreeSpeedMatrix());
		log.info("Built in {} s on {} thread(s), retaining {}.", String.format(Locale.ROOT, "%.2f",
			built.seconds()), this.matrix.getNumberOfThreads(), MatrixFootprint.megabytes(built.retainedBytes()));

		TravelTimeMatrix matrix = built.value();
		Build build = new Build(network, built.seconds(), built.retainedBytes());

		measure(table, build, Answerer.MATRIX, this.batchSizes, this.repetitions, pool,
			thread -> (from, to) -> matrix.getTravelTime(from, to, 0));

		measure(table, build, Answerer.DISPATCH_FLOOR, this.batchSizes, this.repetitions, pool,
			ParallelQueryBenchmark.dispatchFloor());

		if (this.routingBatchSizes != null && !this.routingBatchSizes.isEmpty()) {

			// one graph for every router of every thread of every repetition - it is thread-safe and it is
			// the expensive part, while the tree over it is a few arrays. The trees themselves have to be
			// fresh per repetition, which is why this is built out here rather than inside the factory.
			SpeedyGraph graph = RoutingBaseline.graph(network);

			measure(table, build, Answerer.DIRECT_ROUTING, this.routingBatchSizes, this.routingRepetitions, pool,
				thread -> RoutingBaseline.isolated(graph, this.matrix.getQSimTimeStepSize()));

			// the pool sorted by origin, or reusing the tree would never hit. Each thread walks a
			// contiguous stretch of it, so the sort survives the split over threads.
			measure(table, build, Answerer.DIRECT_ROUTING_BY_ORIGIN, this.routingBatchSizes,
				this.routingRepetitions, RoutingBaseline.sortedByOrigin(pool),
				thread -> RoutingBaseline.byOrigin(graph, this.matrix.getQSimTimeStepSize()));
		} else {
			log.info("--routing-batch-sizes is empty, so the batches have nothing to be compared against.");
		}

		table.logSummary();
		return 0;
	}

	/**
	 * One answerer over the whole ladder: every batch size on every thread count, each repeated.
	 * <p>
	 * Batch sizes ascending and thread counts ascending within them, so that an interrupted run leaves the
	 * cheap rows done rather than the expensive ones half done. A thread count a batch cannot be split over
	 * is skipped with a line in the log instead of failing the run, because the only way to hit it is a
	 * batch smaller than the thread count and that is a typo, not a result.
	 */
	private void measure(ResultTable table, Build build, Answerer answerer, List<Long> sizes, int repetitions,
		List<TravelTimeQuery> pool, IntFunction<ToIntBiFunction<Node, Node>> answer) {

		log.info("--- {} ---", answerer);

		for (long queries : sizes.stream().sorted().toList()) {

			double oneThreadSeconds = Double.NaN;

			for (int threads : this.threadCounts.stream().sorted().toList()) {

				if (queries < threads) {
					log.warn("Skipping {} queries on {} threads: there is not one query per thread.", queries,
						threads);
					continue;
				}

				ParallelQueryBenchmark.Result result = ParallelQueryBenchmark.run(pool, threads, queries,
					repetitions, answer);

				if (threads == 1) {
					oneThreadSeconds = result.medianSeconds();
				}

				table.append(row(answerer, build, result, oneThreadSeconds));
			}
		}
	}

	private List<Object> row(Answerer answerer, Build build, ParallelQueryBenchmark.Result result,
		double oneThreadSeconds) {

		List<Object> record = new ArrayList<>(HEADER.size());
		record.add(answerer);
		record.add(build.network().getLinks().size());
		record.add(build.network().getNodes().size());
		record.add(this.matrix.isUnbounded() ? "" : (long) this.matrix.getCellSize());
		record.add(this.matrix.isUnbounded() ? MatrixOptions.UNBOUNDED : (long) this.matrix.getMaxNeighborDistance());
		record.add(String.format(Locale.ROOT, "%.2f", build.seconds()));
		record.add(build.retainedBytes());
		record.add(result.threads());
		record.add(result.queries());
		record.add(result.repetitions());
		record.add(seconds(result.medianSeconds()));
		record.add(seconds(result.minSeconds()));
		record.add(seconds(result.maxSeconds()));
		record.add(String.format(Locale.ROOT, "%.4f", result.microsecondsPerQuery()));
		record.add(String.format(Locale.ROOT, "%.0f", result.queriesPerSecond()));
		record.add(Double.isNaN(oneThreadSeconds)
			? ""
			: String.format(Locale.ROOT, "%.2f", oneThreadSeconds / result.medianSeconds()));
		return record;
	}

	/**
	 * The pairs every measurement cycles. Uniform over the network rather than over real trips, for the
	 * reason the cost sweep samples the same way: a matrix answers a short pair from its exact half and a
	 * long one from its zonal half, so a sample weighted towards either is not the mean cost of a query.
	 */
	private List<TravelTimeQuery> pool(Network network) {
		int side = (int) Math.max(2, Math.round(Math.sqrt(this.queryPoolSize)));
		List<TravelTimeQuery> pool = ODSample.uniform(network, side, side, new double[]{0}, this.seed + 1);
		log.info("The benchmark cycles {} distinct pairs over {} nodes.", pool.size(),
			network.getNodes().size());
		return pool;
	}

	/**
	 * Free speed and nothing else, for the reason {@link RunMatrixCostExperiment} configures its cells the
	 * same way: a congestion index is one more multiplication per query and would make no measurable
	 * difference to a benchmark, while needing an events file that has nothing to do with how fast a
	 * lookup is.
	 */
	private TravelTimeMatrixConfigGroup config() {
		TravelTimeMatrixConfigGroup config = this.matrix.applyTo(new TravelTimeMatrixConfigGroup());
		config.setVariant(TravelTimeMatrixConfigGroup.Variant.FREE_SPEED);
		return config;
	}

	private String describeShape() {
		return this.matrix.isUnbounded()
			? "exact, node to node, no zones"
			: String.format(Locale.ROOT, "cells of %.0f m, exact within %.0f m", this.matrix.getCellSize(),
			this.matrix.getMaxNeighborDistance());
	}

	private static String seconds(double value) {
		return String.format(Locale.ROOT, "%.6f", value);
	}

	/**
	 * What the matrix cost to build, carried into every row so that the table a reader is given is
	 * self-contained: the build and the batches of one row were measured on the same machine, and the row
	 * says both.
	 */
	private record Build(Network network, double seconds, long retainedBytes) {
	}
}
