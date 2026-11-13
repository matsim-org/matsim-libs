package org.matsim.contrib.msp.traveltimematrix.experiments;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.msp.traveltimematrix.MatrixOptions;
import org.matsim.contrib.msp.traveltimematrix.RoutingOptions;
import org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix;
import org.matsim.contrib.msp.traveltimematrix.TravelTimeMatrixConfigGroup;
import org.matsim.contrib.msp.traveltimematrix.evaluation.ODSample;
import org.matsim.contrib.msp.traveltimematrix.evaluation.RoutedTravelTimes;
import org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeErrorStats;
import org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery;
import org.matsim.contrib.msp.traveltimematrix.prepare.StudyArea;
import org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaNetwork;
import org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaOptions;
import org.matsim.contrib.zone.skims.TravelTimeMatrix;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntBiFunction;

/**
 * <b>What a free-speed travel time matrix costs, and when it is worth building.</b> One sweep over three
 * axes, because the paper asks three questions of the same machinery:
 * <pre>
 *   # part 1: how far does a full matrix scale, and from how many queries does it pay off?
 *   java -Xmx60g -cp matsim-msp.jar \
 *       org.matsim.contrib.msp.traveltimematrix.experiments.RunMatrixCostExperiment \
 *       --network germany.xml.gz --shp dresden-with-buffer.shp --output network-size \
 *       --radii 5000,10000,20000,30000,40000,50000,75000,100000,200000
 *
 *   # part 2: what do zones cost in accuracy, and what do they buy in memory?
 *   java -Xmx12g -cp matsim-msp.jar (same class) \
 *       --network output_network.xml.gz --shp dresden-with-buffer.shp --output resolution \
 *       --cell-sizes 2000,1000,750,500,250,100 --max-neighbor-distances 5000,10000,15000,20000,inf \
 *       --origins 500
 * </pre>
 * The three axes, and what each is for:
 * <ol>
 *     <li><b>{@code --radii}: how big the study area is.</b> The same network cut to a disc of each
 *     radius around one centre, so the number of nodes is swept without swapping scenarios. The node
 *     count is <em>recorded, not controlled</em> - it is the x-axis of every plot, and the radius is only
 *     how the sample is drawn. Left out, the area is whatever {@code --shp} says and there is one row per
 *     matrix configuration, which is the resolution sweep.</li>
 *     <li><b>{@code --cell-sizes}: how coarse the zonal half is.</b> The zonal half answers a pair with
 *     the travel time between the central nodes of two square cells, so this trades accuracy for the
 *     square of the memory. Left out, no zonal half is built at all and only the exact matrix is
 *     measured - which is what part 1 wants.</li>
 *     <li><b>{@code --max-neighbor-distances}: how far the exact node-to-node half reaches.</b>
 *     {@value MatrixOptions#UNBOUNDED} stores every pair node to node, so the matrix is <em>exact</em>,
 *     {@code --cell-size} stops mattering, and it is the most expensive corner. That is the default.</li>
 * </ol>
 * <b>Everything here is at free speed</b>, and that is what makes it a clean experiment rather than a
 * confounded one: with no congestion model in play, the only thing a matrix can be wrong about is its own
 * spatial discretisation. The congestion model is
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix}' business.
 * <p>
 * <b>The baseline is measured per area, not once.</b> {@link Answerer#DIRECT_ROUTING} runs one
 * shortest-path search per query, which is what an analysis that asks about pairs in no particular order
 * pays; {@link Answerer#DIRECT_ROUTING_BY_ORIGIN} sorts the queries by origin first, so one search
 * answers every destination of that origin, which is the same router used as well as it can be used and a
 * far harder target. Both get more expensive as the network grows, and so does building the matrix - the
 * whole question of part 1 is which of them grows faster, so the baseline has to be re-measured for every
 * area. It runs <b>before</b> the matrices of that area, which is what lets every matrix row carry its
 * own {@code break_even_queries}: the query count at which the build time has paid for itself.
 * <p>
 * <b>A configuration is refused rather than attempted</b> when {@link MatrixFootprint} estimates it over
 * {@code --memory-budget}: the sweep writes a {@code skipped_over_memory_budget} row and goes on, instead
 * of running out of heap on configuration 28 of 31 and losing the 27 that worked. Configurations are
 * ordered cheapest first for the same reason. For part 1 those skipped rows are not a failure but
 * <b>the result</b> - the node count at which a full matrix stops fitting in a given machine is the wall
 * that makes zoning necessary at all.
 * <p>
 * <b>One caveat about memory when sweeping radii in a single JVM.</b> Every sparse row keeps a
 * {@code BitSet} indexed by the <em>global</em> node id index, which on a Germany-wide network is spread
 * over half a million however small the cut is - so the bit sets cost {@code nodes x idSpace / 8} rather
 * than {@code nodes^2 / 8}. {@link MatrixFootprint} estimates and logs exactly that, so the budget check
 * is honest, but it means the memory curve of a single-JVM sweep carries a linear term that is an
 * artifact of the id space. For the memory figures that go in a paper, run one radius per JVM over a
 * written-out cut network, which collapses the id space to the nodes that are actually there.
 *
 * <b>And one about the query rate.</b> {@link QueryBenchmark} warms up before it starts the clock, but
 * the JVM as a whole only warms up once - and the areas are run smallest first, so the smallest area pays
 * for it. On a short sweep its {@code microseconds_per_query} can come out several times the next area's,
 * which is an artifact and not a finding. With {@code --queries} in the tens of millions it washes out;
 * on a short run, either compare only from the second area on, or add a small throwaway radius in front.
 *
 * @see MatrixFootprint what is estimated, and why it is an upper bound
 * @see QueryBenchmark the query ladder and the wall-clock budget
 * @see RoutingBaseline the two honest ways to count a router
 */
@CommandLine.Command(
	name = "matrix-cost-experiment",
	description = "Sweeps study area size, cell size and neighbourhood radius against accuracy, runtime and memory.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public final class RunMatrixCostExperiment implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunMatrixCostExperiment.class);

	/**
	 * What answered the queries of a row.
	 */
	public enum Answerer {
		/**
		 * The free-speed matrix of the configuration in the same row.
		 */
		MATRIX,
		/**
		 * No matrix: one free-speed shortest-path search per query, in the order the queries come. The
		 * baseline the matrix has to beat on time and can never beat on accuracy.
		 */
		DIRECT_ROUTING,
		/**
		 * No matrix, queries sorted by origin: one search per origin answers all its destinations.
		 */
		DIRECT_ROUTING_BY_ORIGIN
	}

	/**
	 * How the unbounded neighbourhood is written and read on the command line and in the tables, which is
	 * the same spelling a single build takes - see {@link MatrixOptions#UNBOUNDED}.
	 */
	private static final String INFINITY = MatrixOptions.UNBOUNDED;

	@CommandLine.Option(names = "--output", description = "Directory the result csvs are written to.",
		defaultValue = "matrix-cost-experiment")
	private String outputDirectory;

	@CommandLine.Option(names = "--radii", description = "Radii of the study area to sweep [m], as discs around "
		+ "--centre. Left out, the area is whatever --shp says and the network size is not swept.", split = ",")
	private List<Double> radii;

	@CommandLine.Option(names = "--centre", description = "Centre of the radius sweep as 'x,y' in the coordinate "
		+ "system of the network. Defaults to the centroid of --shp, or the middle of the network's bounding box "
		+ "when there is no --shp.")
	private String centre;

	@CommandLine.Option(names = "--cell-sizes", description = "Cell sizes of the zonal half to sweep [m]. Left out, "
		+ "no zonal half is built and only an exact matrix is measured.", split = ",")
	private List<Double> cellSizes;

	@CommandLine.Option(names = "--max-neighbor-distances", description = "Radii of the exact node-to-node half to "
		+ "sweep [m]. '" + INFINITY + "' stores every pair node to node, which makes the matrix exact.",
		defaultValue = INFINITY, split = ",")
	private List<String> maxNeighborDistances;

	@CommandLine.Option(names = "--origins", description = "How many origins the accuracy sample draws per area. "
		+ "Each gets one destination per distance band. 0 skips the accuracy table altogether, which is right when "
		+ "only exact matrices are measured - their error is zero by construction.", defaultValue = "0")
	private int numberOfOrigins;

	@CommandLine.Option(names = "--distance-bands", description = "Lower edges of the distance bands the error is "
		+ "reported per [m]. One destination is sampled per band and origin.",
		defaultValue = "0,1000,2000,5000,10000,20000", split = ",")
	private List<Double> distanceBands;

	@CommandLine.Option(names = "--queries", description = "How many queries to time per configuration.",
		defaultValue = "10000000")
	private long numberOfQueries;

	@CommandLine.Option(names = "--query-checkpoints", description = "Query counts the rate is reported at, so that "
		+ "a rate which has not settled yet is visible rather than averaged away.",
		defaultValue = "10000,100000,1000000,10000000", split = ",")
	private List<Long> queryCheckpoints;

	@CommandLine.Option(names = "--query-budget", description = "Wall-clock budget per benchmark [s]. What keeps "
		+ "the direct-routing baseline, at milliseconds per query, from running for weeks.", defaultValue = "60")
	private double queryBudgetSeconds;

	@CommandLine.Option(names = "--query-pool", description = "How many distinct pairs the benchmark cycles "
		+ "through. Enough that the queries jump around the study area the way an analysis does, few enough that "
		+ "walking the list is not what is being measured.", defaultValue = "10000")
	private int queryPoolSize;

	@CommandLine.Option(names = "--skip-baseline", description = "Leave out the direct-routing baseline. It is on "
		+ "by default because it is the number the matrix exists to beat, and it is bounded by --query-budget "
		+ "however slow it is - but it is also what break_even_queries is computed against.")
	private boolean skipBaseline;

	@CommandLine.Option(names = "--memory-budget", description = "Configurations estimated to need more memory than "
		+ "this are skipped [MB]. Capped at 60%% of this JVM's -Xmx, whichever is smaller.",
		defaultValue = "" + MatrixFootprint.DEFAULT_BUDGET_MEGABYTES)
	private Long memoryBudgetMegabytes;

	@CommandLine.Option(names = "--build-timeout", description = "Give up on a matrix that has not finished "
		+ "building after this long [s]; 0 switches the check off. A backstop only - memory is the binding "
		+ "constraint for a full matrix, and a build that is merely slow is usually one worth waiting for.",
		defaultValue = "5400")
	private double buildTimeoutSeconds;

	@CommandLine.Option(names = "--cache-directory", description = "Cache every base matrix in this directory, so "
		+ "that a repeated sweep reads them back. Off by default: thirty matrices of up to a few GB each is rarely "
		+ "what is wanted.")
	private String cacheDirectory;

	@CommandLine.Option(names = "--seed", description = "Seed of the sampling.", defaultValue = "4711")
	private long seed;

	@CommandLine.Mixin
	private StudyAreaOptions studyArea = new StudyAreaOptions();

	@CommandLine.Mixin
	private RoutingOptions routing = new RoutingOptions();

	private ResultTable accuracyTable;
	private ResultTable costTable;
	private ResultTable scalingTable;

	public static void main(String[] args) {
		new RunMatrixCostExperiment().execute(args);
	}

	/**
	 * One study area of the sweep: the network it cuts, and the labels every row of it carries.
	 *
	 * @param radius the radius it was cut with [m], or {@link Double#NaN} for the {@code --shp} area,
	 * which has no radius to report
	 */
	private record Area(String label, double radius, Network network) {

		Object radiusLabel() {
			return Double.isNaN(this.radius) ? "" : (long) this.radius;
		}
	}

	/**
	 * One cell of the resolution grid: the two thresholds, and what they are estimated to cost.
	 *
	 * @param unbounded whether {@code maxNeighborDistance} was given as {@value #INFINITY}, which only
	 * changes how it is written in the tables
	 */
	private record Configuration(double cellSize, double maxNeighborDistance, boolean unbounded,
								 MatrixFootprint footprint) {

		String neighborhood() {
			return this.unbounded ? INFINITY : Long.toString((long) this.maxNeighborDistance);
		}

		/**
		 * Whether the zonal half exists at all. It does not when the neighbourhood is unbounded: every
		 * pair is then answered node to node, so
		 * {@link org.matsim.contrib.msp.traveltimematrix.NodeToNodeTravelTimeMatrix} is built instead of
		 * the two-half matrix and no zone system is created. The cell size of such a row is therefore not
		 * merely unread, it describes nothing - so it is reported empty, because a number there would
		 * invite a reader to compare rows along an axis that cannot move the result.
		 */
		Object cellSizeLabel() {
			return this.unbounded ? "" : (long) this.cellSize;
		}

		/**
		 * @see #cellSizeLabel()
		 */
		Object zonesLabel() {
			return this.unbounded ? "" : this.footprint.zones();
		}

		@Override
		public String toString() {
			return this.unbounded
				? "maxNeighborDistance " + INFINITY + " (the exact matrix, no zone system)"
				: String.format(Locale.ROOT, "cellSize %.0f m, maxNeighborDistance %.0f m", this.cellSize,
				this.maxNeighborDistance);
		}
	}

	/**
	 * What the baseline of one area cost per query, so that a matrix row can say when it has paid for
	 * itself. {@link Double#NaN} for both when {@code --skip-baseline} was given.
	 */
	private record Baseline(double isolatedMicroseconds, double byOriginMicroseconds) {

		static final Baseline NONE = new Baseline(Double.NaN, Double.NaN);
	}

	@Override
	public Integer call() {

		try {
			Files.createDirectories(Path.of(this.outputDirectory));
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot create the output directory " + this.outputDirectory, e);
		}

		long memoryBudget = MatrixFootprint.budget(this.memoryBudgetMegabytes);
		double[] bands = this.distanceBands.stream().mapToDouble(Double::doubleValue).toArray();

		openTables();

		List<Area> areas = areas();
		log.info("Sweeping {} study area(s) with a memory budget of {} per matrix.", areas.size(),
			MatrixFootprint.megabytes(memoryBudget));

		int done = 0;
		for (Area area : areas) {
			log.info("=== area {} of {}: {}, {} links, {} nodes ===", ++done, areas.size(), area.label(),
				area.network().getLinks().size(), area.network().getNodes().size());
			runOneArea(area, bands, memoryBudget);
		}

		this.accuracyTable.logSummary();
		this.costTable.logSummary();
		this.scalingTable.logSummary();

		return 0;
	}

	/**
	 * The study areas to sweep, each already cut and verified routable.
	 * <p>
	 * The full network is read <b>once</b> and cut repeatedly - a Germany-wide network file is tens of
	 * megabytes of gzipped xml, and reading it per radius would be most of the runtime of the small
	 * areas. The areas are cut in the order they are to be run, smallest first, so that an interrupted
	 * sweep has finished the ones that fit.
	 */
	private List<Area> areas() {

		Network full = this.studyArea.readFullNetwork();

		if (this.radii == null || this.radii.isEmpty()) {
			StudyArea area = StudyArea.fromShapeFile(this.studyArea.getShp(), full);
			return List.of(new Area(area == null ? "whole-network" : area.toString(), Double.NaN,
				StudyAreaNetwork.cut(full, area, this.studyArea.getModes())));
		}

		Coord centre = centre(full);
		log.info("Sweeping {} radii around {}, {}.", this.radii.size(), (long) centre.getX(), (long) centre.getY());

		return this.radii.stream()
			.sorted()
			.map(radius -> {
				log.info("--- cutting the study area at a radius of {} m ---", (long) radius.doubleValue());
				StudyArea area = StudyArea.circle(centre, radius);
				return new Area("r" + (long) radius.doubleValue(), radius,
					StudyAreaNetwork.cut(full, area, this.studyArea.getModes()));
			})
			.toList();
	}

	/**
	 * The centre of the radius sweep: what {@code --centre} says, else the centroid of {@code --shp},
	 * else the middle of the network. A study about a particular city should give one of the first two -
	 * the middle of a Germany-wide network is in Thuringia.
	 */
	private Coord centre(Network full) {

		if (this.centre != null) {
			String[] parts = this.centre.split(",");
			if (parts.length != 2) {
				throw new IllegalArgumentException("--centre has to be 'x,y' in the coordinate system of the "
					+ "network, not '" + this.centre + "'.");
			}
			return new Coord(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()));
		}

		if (this.studyArea.getShp().isDefined()) {
			return StudyArea.centreOf(this.studyArea.getShp(), full);
		}

		log.warn("No --centre and no --shp, so the radii are measured from the middle of the network's bounding "
			+ "box. That is rarely the city the study is about.");
		return StudyArea.centreOf(full);
	}

	private void runOneArea(Area area, double[] bands, long memoryBudget) {

		Network network = area.network();

		// the accuracy reference: the exact free-speed travel time of every sampled pair of *this*
		// network, routed once and reused by every configuration of it, because a free-speed travel time
		// cannot depend on how the matrix is cut up
		List<TravelTimeQuery> sample = routeTheSampleOnce(network, bands);
		List<TravelTimeQuery> pool = benchmarkPool(network);

		// before the matrices, so that every matrix row can carry the query count at which its build time
		// has paid for itself
		Baseline baseline = this.skipBaseline ? Baseline.NONE : runTheBaseline(area, sample, pool, bands);

		List<Configuration> configurations = configurations(network);
		int done = 0;
		for (Configuration configuration : configurations) {
			log.info("--- {}: configuration {} of {}: {} ---", area.label(), ++done, configurations.size(),
				configuration);
			runOneConfiguration(area, configuration, sample, pool, bands, memoryBudget, baseline);
		}
	}

	/**
	 * Every cell of the resolution grid with its estimated footprint, ordered cheapest first.
	 * <p>
	 * <b>An unbounded neighbourhood appears once, whatever cell sizes were asked for.</b> It bypasses the
	 * zonal half completely - see {@link Configuration#cellSizeLabel()} - so every cell size gives the
	 * same matrix, the same zero error and the same query time; the only thing that changes is how much
	 * time is wasted building a zone system nobody reads. The first full sweep did run it six times, and
	 * the six rows were identical but for build times of 45 to 69 seconds, all of that difference being
	 * waste.
	 */
	private List<Configuration> configurations(Network network) {

		// the coarsest cell size given keeps the wasted zone-system build as small as the inputs allow;
		// with no cell size at all there is nothing to waste, and only unbounded radii can be swept
		double coarsest = this.cellSizes == null || this.cellSizes.isEmpty()
			? Double.NaN
			: this.cellSizes.stream().mapToDouble(Double::doubleValue).max().orElseThrow();

		List<Configuration> configurations = new ArrayList<>();
		for (String given : this.maxNeighborDistances) {

			double radius = MatrixOptions.parseNeighborDistance(given);

			if (Double.isInfinite(radius)) {
				configurations.add(new Configuration(coarsest, radius, true,
					MatrixFootprint.estimate(network, Double.isNaN(coarsest) ? 1000 : coarsest, radius)));
				continue;
			}

			if (Double.isNaN(coarsest)) {
				throw new IllegalArgumentException("--max-neighbor-distances holds the finite radius " + given
					+ " m, which needs a zonal half to answer the pairs beyond it - but no --cell-sizes were "
					+ "given. Give some, or sweep only '" + INFINITY + "'.");
			}

			for (double cellSize : this.cellSizes) {
				configurations.add(new Configuration(cellSize, radius, false,
					MatrixFootprint.estimate(network, cellSize, radius)));
			}
		}

		configurations.sort(Comparator.comparingLong(configuration -> configuration.footprint().totalBytes()));
		return configurations;
	}

	/**
	 * The accuracy sample, with the exact free-speed travel time and path length of every pair filled in,
	 * or an empty list when {@code --origins} is 0.
	 * <p>
	 * One departure time, because free speed does not have a time of day - which is also why the whole
	 * experiment reports a single daily value and no hourly breakdown.
	 */
	private List<TravelTimeQuery> routeTheSampleOnce(Network network, double[] bands) {

		if (this.numberOfOrigins <= 0) {
			return List.of();
		}

		List<TravelTimeQuery> sampled = ODSample.byDistanceBands(network, this.numberOfOrigins, bands,
			new double[]{0}, this.seed);

		log.info("Routing the reference for {} sampled pairs, once for every configuration of this area.",
			sampled.size());
		return RoutedTravelTimes.compute(network, sampled, null, this.routing.getQSimTimeStepSize(),
			this.routing.getNumberOfThreads()).getQueries();
	}

	/**
	 * The pairs the benchmark cycles through. Uniformly random rather than banded: a benchmark is about
	 * how fast an answer comes, and the matrix answers a short pair from the sparse half and a long one
	 * from the zonal half, so a sample weighted towards either would not be the mean cost of a query.
	 */
	private List<TravelTimeQuery> benchmarkPool(Network network) {
		int side = (int) Math.max(2, Math.round(Math.sqrt(this.queryPoolSize)));
		List<TravelTimeQuery> pool = ODSample.uniform(network, side, side, new double[]{0}, this.seed + 1);
		log.info("The benchmark cycles {} distinct pairs.", pool.size());
		return pool;
	}

	private void runOneConfiguration(Area area, Configuration configuration, List<TravelTimeQuery> sample,
		List<TravelTimeQuery> pool, double[] bands, long memoryBudget, Baseline baseline) {

		if (!configuration.footprint().fitsIn(memoryBudget, configuration.cellSize(),
			configuration.maxNeighborDistance())) {
			writeCost(Answerer.MATRIX, area, configuration, Double.NaN, null, baseline,
				"skipped_over_memory_budget");
			return;
		}

		// the previous matrix is unreachable by now but may not have been collected, and the next one is
		// the largest allocation this process makes. A hint is all this can be, and it is worth the ask.
		System.gc();

		long startedAt = System.nanoTime();
		TravelTimeMatrix matrix = TimeVaryingTravelTimeMatrix.create(area.network(), config(area, configuration),
			this.routing.getQSimTimeStepSize()).getFreeSpeedMatrix();
		double buildSeconds = (System.nanoTime() - startedAt) / 1e9;

		log.info("Built in {} s.", String.format(Locale.ROOT, "%.1f", buildSeconds));

		ToIntBiFunction<Node, Node> answer = (from, to) -> matrix.getTravelTime(from, to, 0);
		writeAccuracy(Answerer.MATRIX, area, configuration, sample, bands, answer);

		QueryBenchmark.Result benchmark = QueryBenchmark.run(pool, this.numberOfQueries, checkpoints(),
			this.queryBudgetSeconds, answer);

		writeCost(Answerer.MATRIX, area, configuration, buildSeconds, benchmark, baseline, status(buildSeconds));
		writeScaling(Answerer.MATRIX, area, configuration, benchmark);
	}

	/**
	 * Whether the build took longer than {@code --build-timeout}. Reported rather than enforced: by the
	 * time it is known the matrix is built, and throwing away a finished matrix because it was slow would
	 * lose the row that says how slow. The timeout is there to be read in the table and to stop the next
	 * sweep from being configured the same way.
	 */
	private String status(double buildSeconds) {
		if (this.buildTimeoutSeconds > 0 && buildSeconds > this.buildTimeoutSeconds) {
			log.warn("The matrix took {} s to build, over the --build-timeout of {} s. The row is kept - it is "
					+ "already paid for - but a larger area will not finish.", String.format(Locale.ROOT, "%.0f",
					buildSeconds), (long) this.buildTimeoutSeconds);
			return "over_build_timeout";
		}
		return "ok";
	}

	/**
	 * The baseline of one area: the same questions and the same benchmark, answered by a router instead of
	 * a matrix.
	 * <p>
	 * Its accuracy row is a self-check and nothing else - the reference was computed by this very router,
	 * so anything but zero means the two disagree about the network. Its query time is the point, and it
	 * is what every matrix row of this area is compared against.
	 */
	private Baseline runTheBaseline(Area area, List<TravelTimeQuery> sample, List<TravelTimeQuery> pool,
		double[] bands) {

		log.info("--- {}: the baseline, no matrix ---", area.label());

		ToIntBiFunction<Node, Node> isolated = RoutingBaseline.isolated(area.network(),
			this.routing.getQSimTimeStepSize());
		writeAccuracy(Answerer.DIRECT_ROUTING, area, null, sample, bands, isolated);

		QueryBenchmark.Result perQuery = QueryBenchmark.run(pool, this.numberOfQueries, checkpoints(),
			this.queryBudgetSeconds, isolated);
		writeCost(Answerer.DIRECT_ROUTING, area, null, Double.NaN, perQuery, Baseline.NONE, "ok");
		writeScaling(Answerer.DIRECT_ROUTING, area, null, perQuery);

		// the same router used well: with the pool sorted by origin, one search answers a whole run of
		// consecutive queries. The sort is what makes this measurement mean anything.
		QueryBenchmark.Result perOrigin = QueryBenchmark.run(RoutingBaseline.sortedByOrigin(pool),
			this.numberOfQueries, checkpoints(), this.queryBudgetSeconds,
			RoutingBaseline.byOrigin(area.network(), this.routing.getQSimTimeStepSize()));
		writeCost(Answerer.DIRECT_ROUTING_BY_ORIGIN, area, null, Double.NaN, perOrigin, Baseline.NONE, "ok");
		writeScaling(Answerer.DIRECT_ROUTING_BY_ORIGIN, area, null, perOrigin);

		return new Baseline(perQuery.microsecondsPerQuery(), perOrigin.microsecondsPerQuery());
	}

	/**
	 * The error of one answerer against the exact free-speed reference, overall and per distance band.
	 * Nothing is written when {@code --origins} is 0.
	 * <p>
	 * Accumulated here rather than through {@code MatrixEvaluation}, because an experiment needs the area
	 * and the configuration in every row and a single evaluation knows nothing about either - but the band
	 * labels and the metrics come from the same classes the evaluation uses, so the numbers are
	 * comparable.
	 */
	private void writeAccuracy(Answerer answerer, Area area, @Nullable Configuration configuration,
		List<TravelTimeQuery> sample, double[] bands, ToIntBiFunction<Node, Node> answer) {

		if (sample.isEmpty()) {
			return;
		}

		AccuracyByDistance accuracy = new AccuracyByDistance(bands);
		for (TravelTimeQuery query : sample) {
			accuracy.add(query, answer.applyAsInt(query.fromNode(), query.toNode()));
		}

		accuracy.forEachGroup((band, stats) -> {
			List<Object> record = new ArrayList<>(labels(area, configuration));
			record.add(answerer);
			record.add(band);
			record.addAll(stats.toRecord());
			this.accuracyTable.append(record);
		});

		log.info("{} {}{}: {}", area.label(), answerer, configuration == null ? "" : " (" + configuration + ")",
			accuracy.getOverall());
	}

	private void writeCost(Answerer answerer, Area area, @Nullable Configuration configuration, double buildSeconds,
		@Nullable QueryBenchmark.Result benchmark, Baseline baseline, String status) {

		MatrixFootprint footprint = configuration == null ? null : configuration.footprint();
		double querySeconds = benchmark == null ? Double.NaN : benchmark.seconds();
		double matrixMicroseconds = benchmark == null ? Double.NaN : benchmark.microsecondsPerQuery();

		List<Object> record = new ArrayList<>(labels(area, configuration));
		record.add(answerer);
		record.addAll(List.of(
			footprint == null ? "" : footprint.zonalBytes(),
			footprint == null ? "" : footprint.sparseBytes(),
			footprint == null ? "" : footprint.totalBytes(),
			seconds(buildSeconds),
			benchmark == null ? "" : benchmark.queries(),
			seconds(querySeconds),
			benchmark == null ? "" : String.format(Locale.ROOT, "%.4f", matrixMicroseconds),
			benchmark != null && benchmark.stoppedOnBudget(),
			breakEven(buildSeconds, matrixMicroseconds, baseline.isolatedMicroseconds()),
			breakEven(buildSeconds, matrixMicroseconds, baseline.byOriginMicroseconds()),
			status));
		this.costTable.append(record);
	}

	/**
	 * The number of queries at which building the matrix has paid for itself against a router:
	 * {@code buildTime / (routerPerQuery - matrixPerQuery)}.
	 * <p>
	 * Empty when there is nothing to compare against, and empty when the matrix is <em>not</em> faster per
	 * query, which can happen for a matrix so large that every lookup is a cache miss - and is a far more
	 * interesting thing to see in a table than a negative number that reads like a small one.
	 */
	private static Object breakEven(double buildSeconds, double matrixMicroseconds, double routerMicroseconds) {
		if (Double.isNaN(buildSeconds) || Double.isNaN(matrixMicroseconds) || Double.isNaN(routerMicroseconds)
			|| routerMicroseconds <= matrixMicroseconds) {
			return "";
		}
		return Math.round(buildSeconds * 1e6 / (routerMicroseconds - matrixMicroseconds));
	}

	private void writeScaling(Answerer answerer, Area area, @Nullable Configuration configuration,
		QueryBenchmark.Result benchmark) {

		benchmark.checkpoints().forEach(checkpoint -> {
			List<Object> record = new ArrayList<>(labels(area, configuration));
			record.addAll(List.of(answerer, checkpoint.queries(), seconds(checkpoint.seconds()),
				String.format(Locale.ROOT, "%.4f", checkpoint.microsecondsPerQuery())));
			this.scalingTable.append(record);
		});
	}

	/**
	 * The columns every row of every table starts with: which area, and which matrix configuration. The
	 * configuration columns are empty for a baseline row, which has no matrix to describe.
	 */
	private static List<Object> labels(Area area, @Nullable Configuration configuration) {
		return List.of(area.label(), area.radiusLabel(), area.network().getLinks().size(),
			area.network().getNodes().size(),
			configuration == null ? "" : configuration.cellSizeLabel(),
			configuration == null ? "" : configuration.neighborhood(),
			configuration == null ? "" : configuration.zonesLabel());
	}

	private void openTables() {

		List<String> labels = List.of("area", "radius", "links", "nodes", "cell_size", "max_neighbor_distance",
			"zones");

		List<String> accuracy = new ArrayList<>(labels);
		accuracy.addAll(List.of("answerer", "distance_band"));
		accuracy.addAll(TravelTimeErrorStats.HEADER);
		this.accuracyTable = new ResultTable(Path.of(output("matrix_accuracy.csv")), accuracy);

		List<String> cost = new ArrayList<>(labels);
		cost.addAll(List.of("answerer", "zonal_bytes", "sparse_bytes", "total_bytes", "build_seconds", "queries",
			"query_seconds", "microseconds_per_query", "stopped_on_budget", "break_even_queries_isolated",
			"break_even_queries_by_origin", "status"));
		this.costTable = new ResultTable(Path.of(output("matrix_cost.csv")), cost);

		List<String> scaling = new ArrayList<>(labels);
		scaling.addAll(List.of("answerer", "queries", "seconds", "microseconds_per_query"));
		this.scalingTable = new ResultTable(Path.of(output("matrix_query_scaling.csv")), scaling);
	}

	/**
	 * The config of one cell: free speed and nothing else, so that the only thing the matrix can be wrong
	 * about is its own discretisation.
	 */
	private TravelTimeMatrixConfigGroup config(Area area, Configuration configuration) {

		TravelTimeMatrixConfigGroup config = new TravelTimeMatrixConfigGroup();
		config.setVariant(TravelTimeMatrixConfigGroup.Variant.FREE_SPEED);
		config.setCellSize(Double.isNaN(configuration.cellSize()) ? 1000 : configuration.cellSize());
		config.setMaxNeighborDistance(configuration.maxNeighborDistance());
		config.setNumberOfThreads(this.routing.getNumberOfThreads());
		if (this.cacheDirectory != null) {
			// the area is part of the name: two areas of the same thresholds are different matrices, and
			// reading one back for the other would be a silent wrong answer rather than an error
			config.setMatrixCacheDirectory(Path.of(this.cacheDirectory, area.label()).toString());
		}
		return config;
	}

	private long[] checkpoints() {
		return this.queryCheckpoints.stream().mapToLong(Long::longValue).sorted().toArray();
	}

	private static String seconds(double value) {
		return Double.isNaN(value) ? "" : String.format(Locale.ROOT, "%.2f", value);
	}

	private String output(String file) {
		return Path.of(this.outputDirectory, file).toString();
	}
}
