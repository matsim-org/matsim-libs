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
 * the run or generated pairs, judged by what the trips really took or by a time-dependent router.
 * <p>
 * {@code equil} has fifteen nodes and hardly any congestion, so what is asserted is that the pipeline
 * holds together - every variant is evaluated, every query is answered, the files are written - and not
 * what the errors are. What the errors are is a property of a scenario, not of this code.
 */
class RunEvaluateTravelTimeMatrixTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

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
	void generatedPairsJudgedByARouterEvaluateEveryRungOfTheLadder() throws IOException {

		Path output = run("random-routed", "--od-pairs", "RANDOM", "--reference", "ROUTED",
			"--origins", "3", "--departure-hours", "6,8", "--distance-bands", "0,10000", "--write-queries");

		assertTrue(Files.exists(output.resolve("errors.csv")));
		assertTrue(Files.exists(output.resolve("od_queries.csv")));
		assertTrue(Files.exists(output.resolve("congestion_index.csv")),
			"the index is copied next to the results it produced");
		assertTrue(Files.exists(output.resolve("free_speed_matrix_cell1000_nb5000.bin")),
			"the base matrix is cached under the thresholds it was built with");

		// the routing rungs are in the table because a router was used
		assertVariants(output, MatrixEvaluation.REFERENCE, MatrixEvaluation.FIXED_PATH,
			TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS.name(),
			TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX.name(), MatrixEvaluation.CONSTANT_INDEX,
			TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name());

		List<String> queries = Files.readAllLines(output.resolve("queries.csv"));
		assertTrue(queries.size() > 1);
		assertEquals(0, (queries.size() - 1) % 6, "every one of the six variants answered every query");
	}

	@Test
	void theTripsOfTheRunJudgedByWhatTheyReallyTook() throws IOException {

		Path output = run("trips-experienced", "--od-pairs", "TRIPS", "--reference", "EXPERIENCED",
			"--distance-bands", "0,10000");

		// no router ran, so the routing rungs are not in the table
		List<String> errors = Files.readAllLines(output.resolve("errors.csv"));
		assertTrue(errors.stream().noneMatch(line -> line.startsWith(MatrixEvaluation.REFERENCE + ",")),
			"nothing was routed, so there is no routed reference to report");
		assertVariants(output, TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS.name(),
			TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX.name(), MatrixEvaluation.CONSTANT_INDEX,
			TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name());

		// the reference is a real travel time of the run, so the reported mean is not zero
		String[] overall = overallRow(output, TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name());
		assertTrue(Long.parseLong(overall[3]) > 0, "trips were evaluated");
		assertTrue(Double.parseDouble(overall[4]) > 0, "their mean experienced travel time is positive");

		// and the queries carry it, so they can be evaluated again without reading the events
		List<String> written = Files.readAllLines(output.resolve("od_queries.csv"));
		assertTrue(written.get(0).contains("reference_travel_time"));
		assertTrue(written.get(1).split(",", -1)[3].length() > 0, "the experienced travel time is in the file");
	}

	@Test
	void theTripsOfTheRunJudgedByARouterAsWell() throws IOException {

		// the combination the merge of the two commands makes possible: the real trips as the questions,
		// what they really took as the reference, and the router as one more variant - which is what
		// separates the error of the matrix from the error the simulation's route choice adds
		Path output = run("trips-both", "--od-pairs", "TRIPS", "--reference", "EXPERIENCED", "--also-route",
			"--max-trips", "20", "--distance-bands", "0,10000");

		assertVariants(output, MatrixEvaluation.REFERENCE, MatrixEvaluation.FIXED_PATH,
			TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX.name());

		assertTrue(Long.parseLong(overallRow(output, MatrixEvaluation.REFERENCE)[3]) <= 20,
			"--max-trips caps the trips, and with it the shortest-path trees");
	}

	@Test
	void anEarlierSetOfQueriesCanBeEvaluatedAgain() throws IOException {

		Path first = run("first", "--od-pairs", "TRIPS", "--reference", "EXPERIENCED", "--max-trips", "10",
			"--distance-bands", "0,10000");

		// the same questions and the same reference travel times, with another cell size - and without
		// reading the events at all, which is what the reference column in the file is for
		Path second = run("second", "--od-pairs", "FILE", "--reference", "EXPERIENCED",
			"--od-file", first.resolve("od_queries.csv").toString(), "--cell-size", "5000",
			"--distance-bands", "0,10000");

		assertEquals(Files.readAllLines(first.resolve("od_queries.csv")),
			Files.readAllLines(second.resolve("od_queries.csv")),
			"the second experiment answered the questions of the first");
		assertEquals(overallRow(first, TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name())[3],
			overallRow(second, TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name())[3],
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
				"--reference", "EXPERIENCED"));

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
			"--reference", "API",
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

	private void assertVariants(Path output, String... variants) throws IOException {
		List<String> errors = Files.readAllLines(output.resolve("errors.csv"));
		for (String variant : variants) {
			assertTrue(errors.stream().anyMatch(line -> line.startsWith(variant + ",all,all,")),
				"the error table holds an overall row for " + variant);
		}
	}

	private String[] overallRow(Path output, String variant) throws IOException {
		return Files.readAllLines(output.resolve("errors.csv")).stream()
			.filter(line -> line.startsWith(variant + ",all,all,"))
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
