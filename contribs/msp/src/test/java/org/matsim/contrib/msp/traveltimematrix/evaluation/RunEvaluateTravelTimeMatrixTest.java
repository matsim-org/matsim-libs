package org.matsim.contrib.msp.traveltimematrix.evaluation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.msp.run.RunMspScenario;
import org.matsim.contrib.msp.traveltimematrix.RunComputeCongestionIndex;
import org.matsim.contrib.msp.traveltimematrix.TravelTimeMatrixConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the whole experiment on a real run, in each of the combinations of its two choices: the trips of
 * the run or generated pairs, judged by a free-speed router, by a time-dependent router and by what the
 * trips really took.
 * <p>
 * The tests are named after the checks the command exists to make - the comparison is wired up, the zone
 * system costs this much, the congestion model costs that much - because that is the structure of the
 * result and not only of this file.
 * <p>
 * {@code equil} has fifteen nodes and hardly any congestion, so what is asserted is that the pipeline
 * holds together - every variant is evaluated against every reference, every query is answered, the
 * files are written - and not what the errors are. What the errors are is a property of a scenario, not
 * of this code. The one exception is the self-check, which is a property of the code.
 */
class RunEvaluateTravelTimeMatrixTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	private static final String FREE_SPEED_ROUTING =
		RunEvaluateTravelTimeMatrix.Reference.FREE_SPEED_ROUTING.name();
	private static final String CONGESTED_ROUTING =
		RunEvaluateTravelTimeMatrix.Reference.CONGESTED_ROUTING.name();
	private static final String EXPERIENCED = RunEvaluateTravelTimeMatrix.Reference.EXPERIENCED.name();

	private static final String FREE_SPEED = TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name();
	private static final String GLOBAL_INDEX = TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX.name();

	private Path runOutput;
	private Path congestionIndex;

	@BeforeEach
	void runAScenarioAndMeasureItsCongestionIndex() {

		URL context = ExamplesUtils.getTestScenarioURL("equil");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));

		this.runOutput = Path.of(utils.getOutputDirectory(), "run");
		config.controller().setOutputDirectory(this.runOutput.toString());
		config.controller().setLastIteration(0);
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.gzip);

		Controler controler = MATSimApplication.prepare(new RunMspScenario(config), config);
		controler.run();

		this.congestionIndex = Path.of(utils.getOutputDirectory(), "congestion_index.csv");
		new RunComputeCongestionIndex().execute(
			"--network", network(),
			"--events", events(),
			"--time-bin-size", "3600",
			"--output", this.congestionIndex.toString());
	}

	@Test
	void theVariantNamedAfterAReferenceIsTheSelfCheckOfTheGrid() {

		// the diagonal of the grid only lines up if the two names are the same string, and a reader who
		// sees them line up concludes that the row is a tautology. So they have to stay equal.
		assertEquals(CONGESTED_ROUTING, MatrixEvaluation.CONGESTED_ROUTING);
	}

	@Test
	void generatedPairsAgainstBothRoutedReferencesEvaluateEveryRungOfTheLadder() throws IOException {

		Path output = run("random-routed", "--od-pairs", "RANDOM",
			"--references", FREE_SPEED_ROUTING + "," + CONGESTED_ROUTING,
			"--origins", "3", "--departure-hours", "6,8", "--distance-bands", "0,10000", "--write-queries");

		assertTrue(Files.exists(output.resolve("errors.csv")));
		assertTrue(Files.exists(output.resolve("od_queries.csv")));
		assertTrue(Files.exists(output.resolve("congestion_exposure.csv")),
			"the diagnostic that says whether these pairs carry any congestion at all");
		assertTrue(Files.exists(output.resolve("congestion_index.csv")),
			"the index is copied next to the results it produced");
		assertTrue(Files.exists(output.resolve("free_speed_matrix_cell1000_nb5000.bin")),
			"the base matrix is cached under the thresholds it was built with");

		// the routing rungs are in the table because a router was used, and every rung is reported
		// against both references
		for (String reference : List.of(FREE_SPEED_ROUTING, CONGESTED_ROUTING)) {
			assertVariants(output, reference, MatrixEvaluation.CONGESTED_ROUTING, MatrixEvaluation.FIXED_PATH,
				FREE_SPEED, GLOBAL_INDEX);
		}

		// check 1: the same router on both sides of the subtraction, so the error is the rounding of a
		// travel time to whole seconds and nothing else
		String[] selfCheck = overallRow(output, CONGESTED_ROUTING, MatrixEvaluation.CONGESTED_ROUTING);
		assertTrue(Math.abs(Double.parseDouble(selfCheck[7])) <= 0.5, "bias of the self-check: " + selfCheck[7]);

		// check 2: free speed on both sides, so the free-speed matrix can only be wrong about its own
		// zone system - whatever that error is, it is not a congestion error
		String[] zones = overallRow(output, FREE_SPEED_ROUTING, FREE_SPEED);
		assertTrue(Long.parseLong(zones[4]) > 0, "the zone check was evaluated over some queries");

		List<String> queries = Files.readAllLines(output.resolve("queries.csv"));
		assertEquals("from_node,to_node,departure_time,distance,tt_ref_free_speed_routing,"
			+ "tt_ref_congested_routing,variant,tt_estimate", queries.get(0),
			"a reference is a column of the query table, so one row still covers every reference");
		assertTrue(queries.size() > 1);
		assertEquals(0, (queries.size() - 1) % 4, "every one of the four variants answered every query");
	}

	@Test
	void theFreeSpeedCheckNeedsNoEventsAtAll() throws IOException {

		// what the zone check is on its own: a property of the network and the thresholds, with no run
		// involved. So it must not demand the events of one.
		Path output = Path.of(utils.getOutputDirectory(), "free-speed-only");
		new RunEvaluateTravelTimeMatrix().execute(
			"--network", network(),
			"--congestion-index", this.congestionIndex.toString(),
			"--output-directory", output.toString(),
			"--od-pairs", "RANDOM", "--references", FREE_SPEED_ROUTING,
			"--origins", "3", "--departure-hours", "8", "--distance-bands", "0,10000", "--threads", "2");

		List<String> errors = Files.readAllLines(output.resolve("errors.csv"));
		assertTrue(errors.stream().skip(1).allMatch(line -> line.startsWith(FREE_SPEED_ROUTING + ",")),
			"only the reference that was asked for");
		assertTrue(errors.stream().noneMatch(line -> line.contains("," + MatrixEvaluation.FIXED_PATH + ",")),
			"nothing was routed under congestion, so the rungs that need it are not in the table");
		assertVariants(output, FREE_SPEED_ROUTING, FREE_SPEED, GLOBAL_INDEX);
	}

	/**
	 * The two defaults, and they are one decision: the questions are the trips the run really made, and
	 * the references are then all three, because all three are possible on such a query and the events
	 * have been read either way.
	 * <p>
	 * Why the default is not generated pairs is the finding of the second round of experiments: a
	 * generated pair is mostly made of links the run never put a vehicle on, which were therefore never
	 * slowed down and carry no excess travel time in any reference measured on that run - while the
	 * congestion index, measured where the traffic was, still says the trip should be several percent
	 * slower. See {@link CongestionExposure}, and the row this test reads out of its table.
	 */
	@Test
	void theDefaultQuestionsAreTheTripsTheRunReallyMadeAndTheyAreJudgedByEverything() throws IOException {

		Path output = run("defaults", "--max-trips", "20", "--distance-bands", "0,10000");

		// no --od-pairs and no --references: the trips, against all three
		for (String reference : List.of(FREE_SPEED_ROUTING, CONGESTED_ROUTING, EXPERIENCED)) {
			assertVariants(output, reference, MatrixEvaluation.CONGESTED_ROUTING, MatrixEvaluation.FIXED_PATH,
				FREE_SPEED, GLOBAL_INDEX);
		}

		// and EXPERIENCED being in the table at all is the proof that the pairs were the trips: it is
		// refused outright for generated ones
		assertTrue(Long.parseLong(overallRow(output, EXPERIENCED, GLOBAL_INDEX)[4]) > 0);
		assertTrue(Files.readAllLines(output.resolve("od_queries.csv")).get(1).split(",", -1)[3].length() > 0,
			"every query carries what the trip really took");
	}

	/**
	 * The diagnostic next to the error table: how much congestion the queries carry at all, against how
	 * much the index applies to them. It is the table that says whether a bad row is the congestion
	 * model's fault or the sample's, and on {@code equil} what is asserted is the sanity row - against a
	 * free-speed reference the realized index has to be zero, there being no congestion on either side.
	 */
	@Test
	void theCongestionExposureSaysHowMuchCongestionTheQueriesCarryAtAll() throws IOException {

		Path output = run("exposure", "--max-trips", "20", "--distance-bands", "0,10000");

		List<String> rows = Files.readAllLines(output.resolve("congestion_exposure.csv"));
		assertEquals("reference,group,group_value,n,share_without_excess,mean_free_speed_travel_time,"
			+ "mean_reference_travel_time,realized_index,applied_index,index_bias,index_ratio", rows.get(0));

		// it joins onto errors.csv on reference, group and group_value, which is the whole point of the
		// two files grouping the same queries the same way
		for (String reference : List.of(FREE_SPEED_ROUTING, CONGESTED_ROUTING, EXPERIENCED)) {
			assertTrue(rows.stream().anyMatch(row -> row.startsWith(reference + ",all,all,")),
				"an overall row against " + reference);
			assertTrue(rows.stream().anyMatch(row -> row.startsWith(reference + ",hour,")));
			assertTrue(rows.stream().anyMatch(row -> row.startsWith(reference + ",distance_band,")));
		}

		String[] freeSpeed = rows.stream()
			.filter(row -> row.startsWith(FREE_SPEED_ROUTING + ",all,all,"))
			.findFirst().orElseThrow()
			.split(",", -1);
		assertEquals(0.0, Double.parseDouble(freeSpeed[7]), 1e-4,
			"a free-speed reference carries no excess travel time by construction: " + freeSpeed[7]);
		assertTrue(Double.parseDouble(freeSpeed[8]) >= 0, "and the matrix applies whatever the index says");
	}

	@Test
	void theTripsOfTheRunJudgedByWhatTheyReallyTook() throws IOException {

		Path output = run("trips-experienced", "--od-pairs", "TRIPS", "--references", EXPERIENCED,
			"--distance-bands", "0,10000");

		// no router ran, so the routing rungs are not in the table
		List<String> errors = Files.readAllLines(output.resolve("errors.csv"));
		assertTrue(errors.stream().noneMatch(line -> line.contains("," + MatrixEvaluation.CONGESTED_ROUTING + ",")),
			"nothing was routed, so there is no routed rung to report");
		assertVariants(output, EXPERIENCED, FREE_SPEED, GLOBAL_INDEX);

		// the reference is a real travel time of the run, so the reported mean is not zero
		String[] overall = overallRow(output, EXPERIENCED, FREE_SPEED);
		assertTrue(Long.parseLong(overall[4]) > 0, "trips were evaluated");
		assertTrue(Double.parseDouble(overall[5]) > 0, "their mean experienced travel time is positive");

		// and the queries carry it, so they can be evaluated again without reading the events
		List<String> written = Files.readAllLines(output.resolve("od_queries.csv"));
		assertTrue(written.get(0).contains("observed_travel_time"));
		assertTrue(written.get(1).split(",", -1)[3].length() > 0, "the experienced travel time is in the file");
	}

	@Test
	void theTripsOfTheRunAgainstEveryReferenceAtOnce() throws IOException {

		// the whole point of the references being a list: the real trips as the questions, and three
		// notions of "right" side by side, which is what attributes the error
		Path output = run("trips-everything", "--od-pairs", "TRIPS",
			"--references", EXPERIENCED + "," + CONGESTED_ROUTING + "," + FREE_SPEED_ROUTING,
			"--max-trips", "20", "--distance-bands", "0,10000", "--with-constant-index");

		for (String reference : List.of(EXPERIENCED, CONGESTED_ROUTING, FREE_SPEED_ROUTING)) {
			assertVariants(output, reference, MatrixEvaluation.CONGESTED_ROUTING, MatrixEvaluation.FIXED_PATH,
				FREE_SPEED, MatrixEvaluation.CONSTANT_INDEX, GLOBAL_INDEX);
		}

		assertTrue(Long.parseLong(overallRow(output, EXPERIENCED, GLOBAL_INDEX)[4]) <= 20,
			"--max-trips caps the trips, and with it the shortest-path trees");

		// the router against reality: the row that says how much of the gap is not the matrix at all
		assertTrue(Double.parseDouble(overallRow(output, EXPERIENCED, MatrixEvaluation.CONGESTED_ROUTING)[5]) > 0);
	}

	/**
	 * The configuration the method is meant to be judged in: an unbounded neighbourhood, so the matrix is
	 * exact and its discretisation error is zero <b>by construction</b>. What is then left against a
	 * congested reference is the congestion model alone, which is the quantity of interest and which a
	 * zonal matrix cannot show separately, its own error being scatter that does not simply add up.
	 */
	@Test
	void anUnboundedNeighborhoodIsExact() throws IOException {

		Path output = run("exact", "--od-pairs", "RANDOM",
			"--references", FREE_SPEED_ROUTING + "," + CONGESTED_ROUTING,
			"--max-neighbor-distance", "inf", "--cell-size", "250",
			"--origins", "3", "--departure-hours", "6,8", "--distance-bands", "0,10000",
			"--with-constant-index");

		assertVariants(output, FREE_SPEED_ROUTING, FREE_SPEED, MatrixEvaluation.CONSTANT_INDEX, GLOBAL_INDEX);

		// the point of the whole configuration: against a free-speed router the exact matrix is right to
		// the second, the only difference being that it truncates a travel time to whole seconds
		String[] zones = overallRow(output, FREE_SPEED_ROUTING, FREE_SPEED);
		assertTrue(Long.parseLong(zones[4]) > 0, "it was evaluated over some queries");
		assertTrue(Double.parseDouble(zones[9]) < 1.0,
			"mean absolute error of an exact matrix, in seconds: " + zones[9]);
		assertEquals(0.0, Double.parseDouble(zones[11]), 0.01, "and so its MAPE is zero: " + zones[11]);

		// nothing to cache: the format of contribs/dvrp starts with the zone system there is none of
		try (java.util.stream.Stream<Path> files = Files.list(output)) {
			assertTrue(files.noneMatch(file -> file.getFileName().toString().endsWith(".bin")),
				"an exact matrix is routed again rather than cached, being a travel time per node pair");
		}
	}

	/**
	 * {@code --cell-size} is ignored when the neighbourhood is unbounded, and the way to show that is
	 * that two cell sizes which would give very different zonal matrices give the same errors.
	 */
	@Test
	void theCellSizeCannotChangeAnExactMatrix() throws IOException {

		Path fine = run("exact-fine", "--od-pairs", "RANDOM", "--references", FREE_SPEED_ROUTING,
			"--max-neighbor-distance", "inf", "--cell-size", "100",
			"--origins", "3", "--departure-hours", "8", "--distance-bands", "0,10000");
		Path coarse = run("exact-coarse", "--od-pairs", "RANDOM", "--references", FREE_SPEED_ROUTING,
			"--max-neighbor-distance", "inf", "--cell-size", "20000",
			"--origins", "3", "--departure-hours", "8", "--distance-bands", "0,10000");

		assertEquals(Files.readAllLines(fine.resolve("errors.csv")),
			Files.readAllLines(coarse.resolve("errors.csv")));
	}

	@Test
	void theConstantIndexIsOffUnlessItIsAskedFor() throws IOException {

		// it is a variation of the free-speed variant with another factor, so it is not reported by
		// default - it says whether the *time variation* earns its keep, which is a separate question
		Path output = run("no-constant", "--od-pairs", "RANDOM", "--references", FREE_SPEED_ROUTING,
			"--origins", "2", "--departure-hours", "8", "--distance-bands", "0,10000");

		assertTrue(Files.readAllLines(output.resolve("errors.csv")).stream()
				.noneMatch(line -> line.contains("," + MatrixEvaluation.CONSTANT_INDEX + ",")),
			"not in the table unless --with-constant-index");
	}

	@Test
	void anEarlierSetOfQueriesCanBeEvaluatedAgain() throws IOException {

		Path first = run("first", "--od-pairs", "TRIPS", "--references", EXPERIENCED, "--max-trips", "10",
			"--distance-bands", "0,10000");

		// the same questions and the same measured travel times, with another cell size - and without
		// reading the events at all, which is what the measured column in the file is for
		Path second = run("second", "--od-pairs", "FILE", "--references", EXPERIENCED,
			"--od-file", first.resolve("od_queries.csv").toString(), "--cell-size", "5000",
			"--distance-bands", "0,10000");

		assertEquals(Files.readAllLines(first.resolve("od_queries.csv")),
			Files.readAllLines(second.resolve("od_queries.csv")),
			"the second experiment answered the questions of the first");
		assertEquals(overallRow(first, EXPERIENCED, FREE_SPEED)[4], overallRow(second, EXPERIENCED, FREE_SPEED)[4],
			"over the same number of queries");
	}

	@Test
	void anExperiencedReferenceForPairsNobodyDroveIsRefusedBeforeAnythingIsRead() {

		// the one impossible combination, and it has to fail in the first moment rather than after the
		// events, the network and the matrix
		// execute(..) rethrows a RuntimeException of the command, so the message can be asserted
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
			() -> new RunEvaluateTravelTimeMatrix().execute(
				"--network", network(),
				"--events", events(),
				"--congestion-index", this.congestionIndex.toString(),
				"--output-directory", Path.of(utils.getOutputDirectory(), "never-written").toString(),
				"--od-pairs", "RANDOM",
				"--references", EXPERIENCED));

		assertTrue(thrown.getMessage().contains("was never driven"), thrown.getMessage());
		assertTrue(Files.notExists(Path.of(utils.getOutputDirectory(), "never-written")),
			"and nothing was computed before it gave up");
	}

	@Test
	void theApiReferenceSaysHowToDoItByHand() {
		assertThrows(UnsupportedOperationException.class, () -> new RunEvaluateTravelTimeMatrix().execute(
			"--network", network(),
			"--events", events(),
			"--congestion-index", this.congestionIndex.toString(),
			"--output-directory", Path.of(utils.getOutputDirectory(), "api").toString(),
			"--od-pairs", "RANDOM",
			"--references", "API",
			"--origins", "2", "--departure-hours", "8", "--distance-bands", "0,10000"));
	}

	private Path run(String name, String... options) {

		Path output = Path.of(utils.getOutputDirectory(), name);
		List<String> arguments = new ArrayList<>(List.of(
			"--network", network(),
			"--events", events(),
			"--congestion-index", this.congestionIndex.toString(),
			"--output-directory", output.toString(),
			"--time-bin-size", "3600",
			"--threads", "2"));
		arguments.addAll(List.of(options));

		new RunEvaluateTravelTimeMatrix().execute(arguments.toArray(String[]::new));
		return output;
	}

	private void assertVariants(Path output, String reference, String... variants) throws IOException {
		List<String> errors = Files.readAllLines(output.resolve("errors.csv"));
		for (String variant : variants) {
			assertTrue(errors.stream().anyMatch(line -> line.startsWith(reference + "," + variant + ",all,all,")),
				"the error table holds an overall row for " + variant + " against " + reference);
		}
	}

	private String[] overallRow(Path output, String reference, String variant) throws IOException {
		return Files.readAllLines(output.resolve("errors.csv")).stream()
			.filter(line -> line.startsWith(reference + "," + variant + ",all,all,"))
			.findFirst()
			.orElseThrow()
			.split(",");
	}

	private String network() {
		return this.runOutput.resolve("output_network.xml.gz").toString();
	}

	private String events() {
		return this.runOutput.resolve("output_events.xml.gz").toString();
	}
}
