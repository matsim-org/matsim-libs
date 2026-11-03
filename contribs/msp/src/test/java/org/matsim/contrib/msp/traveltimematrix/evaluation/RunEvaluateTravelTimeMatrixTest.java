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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the whole experiment on a real run: a scenario, the congestion index of its events, the matrix,
 * the routing reference and the error tables.
 * <p>
 * {@code equil} has fifteen nodes and hardly any congestion, so this test asserts that the pipeline
 * holds together - every variant is evaluated, every query is answered, the files are written and the
 * ladder comes out in the right order - and not what the errors are. What the errors are is a property
 * of a scenario, not of this code.
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
			"--network", network().toString(),
			"--events", events().toString(),
			"--time-bin-size", "3600",
			"--output", this.congestionIndex.toString());
	}

	@Test
	void itEvaluatesEveryRungOfTheLadderAndWritesTheTables() throws IOException {

		Path output = Path.of(utils.getOutputDirectory(), "evaluation");

		new RunEvaluateTravelTimeMatrix().execute(
			"--network", network().toString(),
			"--events", events().toString(),
			"--congestion-index", this.congestionIndex.toString(),
			"--output-directory", output.toString(),
			"--origins", "3",
			"--departure-hours", "6,8",
			"--distance-bands", "0,10000",
			"--time-bin-size", "3600",
			"--threads", "2");

		assertTrue(Files.exists(output.resolve("od_sample.csv")));
		assertTrue(Files.exists(output.resolve("queries.csv")));
		assertTrue(Files.exists(output.resolve("errors.csv")));
		assertTrue(Files.exists(output.resolve("congestion_index.csv")),
			"the index is copied next to the results it produced");
		assertTrue(Files.exists(output.resolve("free_speed_matrix.bin")),
			"the base matrix is cached, so a second experiment does not compute it again");

		List<String> errors = Files.readAllLines(output.resolve("errors.csv"));
		for (String variant : List.of(MatrixEvaluation.REFERENCE, MatrixEvaluation.FIXED_PATH,
			MatrixEvaluation.CONSTANT_INDEX, TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS.name(),
			TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX.name(),
			TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name())) {
			assertTrue(errors.stream().anyMatch(line -> line.startsWith(variant + ",all,all,")),
				"the error table holds an overall row for " + variant);
		}

		List<String> queries = Files.readAllLines(output.resolve("queries.csv"));
		// a header, plus one row per query and variant. Three origins give at most two destinations each
		// - the second distance band may hold no node for an origin at the edge of the network.
		assertTrue(queries.size() > 1);
		assertEquals(0, (queries.size() - 1) % (2 * 6),
			"every variant answered every query: " + (queries.size() - 1) + " rows over two departure times and "
				+ "six variants");
	}

	@Test
	void itEvaluatesTheQueriesOfAGivenSampleFile() throws IOException {

		Path first = Path.of(utils.getOutputDirectory(), "first");
		new RunEvaluateTravelTimeMatrix().execute(
			"--network", network().toString(),
			"--events", events().toString(),
			"--congestion-index", this.congestionIndex.toString(),
			"--output-directory", first.toString(),
			"--origins", "2",
			"--departure-hours", "8",
			"--distance-bands", "0,10000",
			"--time-bin-size", "3600");

		// the point of writing the sample: a second experiment, with other matrix parameters, on exactly
		// the same queries
		Path second = Path.of(utils.getOutputDirectory(), "second");
		new RunEvaluateTravelTimeMatrix().execute(
			"--network", network().toString(),
			"--events", events().toString(),
			"--congestion-index", this.congestionIndex.toString(),
			"--output-directory", second.toString(),
			"--od-sample", first.resolve("od_sample.csv").toString(),
			"--cell-size", "5000",
			"--time-bin-size", "3600");

		assertEquals(Files.readAllLines(first.resolve("od_sample.csv")),
			Files.readAllLines(second.resolve("od_sample.csv")),
			"the second experiment answered the queries of the first");
	}

	private Path network() {
		return this.runOutput.resolve("output_network.xml.gz");
	}

	private Path events() {
		return this.runOutput.resolve("output_events.xml.gz");
	}
}
