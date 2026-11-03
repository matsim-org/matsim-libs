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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the validation against the car legs of a real run, so that the step from events to an error table
 * is covered end to end: the leg bookkeeping, the matrix, the comparison and the csv.
 * <p>
 * {@code equil} is a handful of links with hardly any congestion, so what is asserted is that every
 * variant was validated on the legs that were found and that the table says so. What the errors are is a
 * property of a scenario.
 */
class RunValidateTravelTimeMatrixTest {

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
			"--network", this.runOutput.resolve("output_network.xml.gz").toString(),
			"--events", this.runOutput.resolve("output_events.xml.gz").toString(),
			"--time-bin-size", "3600",
			"--output", this.congestionIndex.toString());
	}

	@Test
	void itValidatesEveryVariantOnTheLegsOfTheRun() throws IOException {

		Path output = Path.of(utils.getOutputDirectory(), "validation");

		new RunValidateTravelTimeMatrix().execute(
			"--network", this.runOutput.resolve("output_network.xml.gz").toString(),
			"--events", this.runOutput.resolve("output_events.xml.gz").toString(),
			"--congestion-index", this.congestionIndex.toString(),
			"--output-directory", output.toString(),
			"--time-bin-size", "3600",
			"--distance-bands", "0,10000",
			"--write-legs");

		assertTrue(Files.exists(output.resolve("errors.csv")));
		assertTrue(Files.exists(output.resolve("legs.csv")));
		assertTrue(Files.exists(output.resolve("congestion_index.csv")));

		List<String> errors = Files.readAllLines(output.resolve("errors.csv"));
		for (String variant : List.of(TravelTimeMatrixConfigGroup.Variant.BY_ROAD_CLASS.name(),
			TravelTimeMatrixConfigGroup.Variant.GLOBAL_INDEX.name(), MatrixEvaluation.CONSTANT_INDEX,
			TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name())) {
			assertTrue(errors.stream().anyMatch(line -> line.startsWith(variant + ",all,all,")),
				"the error table holds an overall row for " + variant);
		}

		// the experienced travel time is the reference, so the reported mean reference is a real travel
		// time of the run and not zero
		String overall = errors.stream()
			.filter(line -> line.startsWith(TravelTimeMatrixConfigGroup.Variant.FREE_SPEED.name() + ",all,all,"))
			.findFirst()
			.orElseThrow();
		String[] columns = overall.split(",");
		assertTrue(Long.parseLong(columns[3]) > 0, "legs were validated");
		assertTrue(Double.parseDouble(columns[4]) > 0, "their mean experienced travel time is positive");

		List<String> legs = Files.readAllLines(output.resolve("legs.csv"));
		assertTrue(legs.get(0).startsWith("from_node,to_node,departure_time"));
		assertTrue(legs.stream().anyMatch(line -> line.contains("FREE_SPEED")));
	}
}
