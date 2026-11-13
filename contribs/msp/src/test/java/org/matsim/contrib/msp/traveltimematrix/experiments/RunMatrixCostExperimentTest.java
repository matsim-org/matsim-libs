package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.msp.traveltimematrix.TestNetworks;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the whole sweep on a grid network small enough to finish in a second, and asserts the things that
 * are properties of the <b>code</b> rather than of a scenario: that every cell of the grid produces a row
 * in each table, that the corners which must be exact are exact, that the radius axis really cuts
 * different networks, that a configuration over the memory budget is refused instead of attempted, and
 * that the break-even count is computed against the baseline.
 * <p>
 * A grid rather than a run, because nothing here needs a demand: the experiment is entirely about the
 * network and free speed, which is also why it is the one experiment of the study that can be run before
 * a scenario is calibrated.
 * <p>
 * Rows are read <b>by column name</b> and never by position. The tables carry nineteen columns and the
 * layout is the thing most likely to change; a test that hard-codes indices starts failing for reasons
 * that have nothing to do with what it is checking.
 */
class RunMatrixCostExperimentTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * 8 by 8 nodes, 1 km apart, so the area is 7 by 7 km and a 2 km cell is genuinely coarser than the
	 * node spacing - which is what makes the zonal error non-zero and the experiment meaningful.
	 */
	private static final int GRID_SIZE = 8;

	private static final String MATRIX = RunMatrixCostExperiment.Answerer.MATRIX.name();
	private static final String DIRECT = RunMatrixCostExperiment.Answerer.DIRECT_ROUTING.name();
	private static final String BY_ORIGIN = RunMatrixCostExperiment.Answerer.DIRECT_ROUTING_BY_ORIGIN.name();

	private Path networkFile;

	@BeforeEach
	void writeAGridNetwork() {
		this.networkFile = Path.of(utils.getOutputDirectory(), "network.xml");
		NetworkUtils.writeNetwork(TestNetworks.grid(GRID_SIZE), this.networkFile.toString());
	}

	@Test
	void everyCellOfTheResolutionGridGetsARowInEveryTable() throws IOException {

		Path output = run("sweep", "--cell-sizes", "2000,1000", "--max-neighbor-distances", "2000,inf",
			"--origins", "5");

		List<Map<String, String>> accuracy = rows(output, "matrix_accuracy.csv");
		List<Map<String, String>> cost = rows(output, "matrix_cost.csv");
		List<Map<String, String>> scaling = rows(output, "matrix_query_scaling.csv");

		// two cell sizes times the one bounded radius, plus a single unbounded row
		assertEquals(3, cost.stream().filter(row -> row.get("answerer").equals(MATRIX)).count());

		for (String cellSize : List.of("2000", "1000")) {
			assertTrue(accuracy.stream().anyMatch(row -> row.get("cell_size").equals(cellSize)
					&& row.get("max_neighbor_distance").equals("2000") && row.get("distance_band").equals("all")),
				"an overall accuracy row for cellSize " + cellSize);
			assertTrue(scaling.stream().anyMatch(row -> row.get("cell_size").equals(cellSize)
				&& row.get("max_neighbor_distance").equals("2000")), "a query-scaling row for it");
		}

		// the error is reported per distance band as well as overall
		assertTrue(accuracy.stream().anyMatch(row -> row.get("distance_band").contains("-")),
			"at least one distance band row");

		// every matrix configuration was built and timed, and every row says which network it is about
		cost.stream().filter(row -> row.get("answerer").equals(MATRIX)).forEach(row -> {
			assertEquals("ok", row.get("status"));
			assertTrue(Double.parseDouble(row.get("build_seconds")) >= 0);
			assertTrue(Long.parseLong(row.get("queries")) > 0, "queries were timed");
			assertEquals(Integer.toString(GRID_SIZE * GRID_SIZE), row.get("nodes"));
			assertEquals("", row.get("radius"), "no radius was swept, so there is none to report");
		});
	}

	/**
	 * The axis part 1 of the paper is about: the same network cut to several radii, so the number of nodes
	 * is swept without swapping scenarios.
	 */
	@Test
	void theRadiusAxisCutsADifferentNetworkPerRadius() throws IOException {

		// the grid is 7 by 7 km around its own centre, so these three radii genuinely differ
		Path output = run("radii", "--radii", "2000,3000,5000");

		List<Map<String, String>> cost = rows(output, "matrix_cost.csv");
		List<Map<String, String>> matrices = cost.stream()
			.filter(row -> row.get("answerer").equals(MATRIX))
			.toList();

		assertEquals(3, matrices.size(), "one exact matrix per radius");
		assertEquals(List.of("2000", "3000", "5000"), matrices.stream().map(row -> row.get("radius")).toList(),
			"smallest first, so an interrupted sweep has finished the ones that fit");

		// the point of the axis: more radius, more nodes
		List<Integer> nodes = matrices.stream().map(row -> Integer.parseInt(row.get("nodes"))).toList();
		assertTrue(nodes.get(0) < nodes.get(1), "2 km holds fewer nodes than 3 km: " + nodes);
		assertTrue(nodes.get(1) < nodes.get(2), "and 3 km fewer than 5 km: " + nodes);
		assertTrue(nodes.get(2) <= GRID_SIZE * GRID_SIZE);

		// the baseline is re-measured per area, because the router gets more expensive with the network
		// too - which is the comparison the experiment exists to make
		for (String radius : List.of("2000", "3000", "5000")) {
			assertTrue(cost.stream().anyMatch(row -> row.get("radius").equals(radius)
				&& row.get("answerer").equals(DIRECT)), "a baseline row for radius " + radius);
		}
	}

	/**
	 * With no {@code --cell-sizes} only the exact matrix can be built, and that is the default - part 1
	 * measures full matrices only. Asking for a finite radius without a cell size is refused, because the
	 * pairs beyond that radius would have nothing to answer them.
	 */
	@Test
	void withoutCellSizesOnlyTheExactMatrixIsMeasured() throws IOException {

		Path output = run("exact-only");

		List<Map<String, String>> cost = rows(output, "matrix_cost.csv");
		List<Map<String, String>> matrices = cost.stream()
			.filter(row -> row.get("answerer").equals(MATRIX))
			.toList();

		assertEquals(1, matrices.size());
		assertEquals("inf", matrices.get(0).get("max_neighbor_distance"));
		assertEquals("", matrices.get(0).get("cell_size"), "there is no zonal half to describe");
		assertEquals("", matrices.get(0).get("zones"));
		assertEquals("0", matrices.get(0).get("zonal_bytes"), "and none was built");

		assertFalse(Files.exists(output.resolve("matrix_accuracy.csv")),
			"--origins defaults to 0: an exact matrix has no error worth a table");

		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> run("no-cells", "--max-neighbor-distances", "2000"));
		assertTrue(thrown.getMessage().contains("--cell-sizes"), thrown.getMessage());
	}

	@Test
	void anUnboundedNeighborhoodIsEvaluatedOnceWhateverCellSizesAreSwept() throws IOException {

		// it answers every pair from the exact node-to-node half, so the zonal half is never read and the
		// cell size cannot change the result. The first full sweep ran it once per cell size and produced
		// six identical rows differing only in wasted build time, which is what this guards against.
		Path output = run("dedup", "--cell-sizes", "2000,1000,500", "--max-neighbor-distances", "2000,inf");

		List<Map<String, String>> cost = rows(output, "matrix_cost.csv");
		List<Map<String, String>> unbounded = cost.stream()
			.filter(row -> row.get("max_neighbor_distance").equals("inf"))
			.toList();

		assertEquals(1, unbounded.size(), "one unbounded row, not three");
		assertEquals("", unbounded.get(0).get("cell_size"), "its cell_size is reported empty");
		assertEquals("", unbounded.get(0).get("zones"), "and so is its zone count");

		// the bounded radius is still swept across every cell size
		assertEquals(3, cost.stream().filter(row -> row.get("max_neighbor_distance").equals("2000")).count());
	}

	@Test
	void theTwoCornersThatMustBeExactAreExact() throws IOException {

		Path output = run("exact", "--cell-sizes", "2000", "--max-neighbor-distances", "2000,inf",
			"--origins", "5");
		List<Map<String, String>> accuracy = rows(output, "matrix_accuracy.csv");

		// an unbounded neighbourhood stores every pair node to node, so the matrix is the reference
		Map<String, String> unbounded = overall(accuracy, MATRIX, "inf");
		assertTrue(Math.abs(Double.parseDouble(unbounded.get("bias"))) <= 1,
			"bias of the exact matrix: " + unbounded.get("bias") + " s, which can only be rounding");
		assertTrue(Double.parseDouble(unbounded.get("mean_absolute_error")) <= 1);

		// routing every query is how the reference was computed in the first place, so this row coming out
		// at zero is the self-check of the experiment
		Map<String, String> routed = overall(accuracy, DIRECT, "");
		assertTrue(Math.abs(Double.parseDouble(routed.get("bias"))) <= 1,
			"bias of direct routing: " + routed.get("bias"));

		// and the bounded one is worse, which is the whole point of sweeping the radius
		Map<String, String> bounded = overall(accuracy, MATRIX, "2000");
		assertTrue(Double.parseDouble(bounded.get("mean_absolute_error"))
				>= Double.parseDouble(unbounded.get("mean_absolute_error")),
			"a 2 km neighbourhood over a 7 km area leaves the long pairs to the zones, so its mean absolute "
				+ "error cannot be below the exact matrix's");
	}

	@Test
	void theBaselineIsTimedBothWaysAndIsWhatBreakEvenIsComputedAgainst() throws IOException {

		Path output = run("baseline", "--cell-sizes", "2000", "--max-neighbor-distances", "2000");
		List<Map<String, String>> cost = rows(output, "matrix_cost.csv");

		assertTrue(cost.stream().anyMatch(row -> row.get("answerer").equals(DIRECT)),
			"one shortest-path search per query");
		assertTrue(cost.stream().anyMatch(row -> row.get("answerer").equals(BY_ORIGIN)),
			"and the same router used well, which is the harder target");

		// the configuration columns are empty for the baseline: it has no cell size and no neighbourhood
		cost.stream().filter(row -> row.get("answerer").startsWith("DIRECT_")).forEach(row -> {
			assertEquals("", row.get("cell_size"));
			assertEquals("", row.get("max_neighbor_distance"));
			assertEquals("", row.get("build_seconds"), "there is nothing to build");
			assertEquals("", row.get("break_even_queries_isolated"), "a router never breaks even against itself");
		});

		// the number part 1 reports: the matrix row says when its build time has paid for itself
		Map<String, String> matrix = cost.stream()
			.filter(row -> row.get("answerer").equals(MATRIX))
			.findFirst().orElseThrow();
		assertNotEquals("", matrix.get("break_even_queries_isolated"),
			"the matrix is faster per query than an isolated search, so a break-even exists");
		assertTrue(Long.parseLong(matrix.get("break_even_queries_isolated")) >= 0);

		Path without = run("no-baseline", "--cell-sizes", "2000", "--max-neighbor-distances", "2000",
			"--skip-baseline");
		List<Map<String, String>> alone = rows(without, "matrix_cost.csv");
		assertTrue(alone.stream().noneMatch(row -> row.get("answerer").startsWith("DIRECT_")),
			"--skip-baseline leaves the router out");
		assertEquals("", alone.get(0).get("break_even_queries_isolated"),
			"and with nothing to compare against there is no break-even to report");
	}

	@Test
	void aConfigurationOverTheMemoryBudgetIsRefusedRatherThanAttempted() throws IOException {

		// a budget of nothing: the sweep has to write the row and carry on, because the alternative is
		// losing the configurations that did work. For part 1 this row *is* the result - it is the node
		// count at which a full matrix stops fitting.
		Path output = run("over-budget", "--memory-budget", "0", "--origins", "5");

		List<Map<String, String>> cost = rows(output, "matrix_cost.csv");
		Map<String, String> skipped = cost.stream()
			.filter(row -> row.get("answerer").equals(MATRIX))
			.findFirst().orElseThrow();

		assertEquals("skipped_over_memory_budget", skipped.get("status"));
		assertEquals("", skipped.get("build_seconds"), "nothing was built");
		assertEquals("", skipped.get("queries"), "and nothing was timed");
		assertTrue(Long.parseLong(skipped.get("total_bytes")) > 0,
			"but the estimate that refused it is on the record");

		assertTrue(rows(output, "matrix_accuracy.csv").stream()
				.noneMatch(row -> row.get("answerer").equals(MATRIX)),
			"a matrix that was never built has no accuracy");
		assertTrue(cost.stream().anyMatch(row -> row.get("answerer").equals(DIRECT)),
			"and the baseline still ran");
	}

	private Path run(String name, String... options) {

		Path output = Path.of(utils.getOutputDirectory(), name);
		List<String> arguments = new ArrayList<>(List.of(
			"--network", this.networkFile.toString(),
			"--output", output.toString(),
			"--distance-bands", "0,2000,5000",
			"--queries", "2000",
			"--query-checkpoints", "1000,2000",
			"--query-budget", "10",
			"--query-pool", "25",
			"--threads", "2"));
		arguments.addAll(List.of(options));

		new RunMatrixCostExperiment().execute(arguments.toArray(String[]::new));
		return output;
	}

	/**
	 * The rows of a result table, each as a map from column name to value - see the class javadoc for why
	 * not by position.
	 */
	private List<Map<String, String>> rows(Path output, String file) throws IOException {

		List<String> lines = Files.readAllLines(output.resolve(file));
		String[] header = lines.get(0).split(",", -1);

		return lines.stream().skip(1).map(line -> {
			String[] values = line.split(",", -1);
			Map<String, String> row = new LinkedHashMap<>();
			for (int column = 0; column < header.length; column++) {
				row.put(header[column], column < values.length ? values[column] : "");
			}
			return row;
		}).toList();
	}

	private static Map<String, String> overall(List<Map<String, String>> accuracy, String answerer,
		String neighborhood) {
		return accuracy.stream()
			.filter(row -> row.get("answerer").equals(answerer)
				&& row.get("max_neighbor_distance").equals(neighborhood)
				&& row.get("distance_band").equals("all"))
			.findFirst()
			.orElseThrow(() -> new AssertionError("no overall row for " + answerer + " / " + neighborhood));
	}
}
