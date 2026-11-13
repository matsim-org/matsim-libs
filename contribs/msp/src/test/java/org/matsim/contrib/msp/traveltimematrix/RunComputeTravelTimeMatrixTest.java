package org.matsim.contrib.msp.traveltimematrix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.msp.run.RunMspScenario;
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
 * Runs the matrix command on the output of a real run: that the matrix is built, that the base matrix is
 * cached, and that the summary csv a threshold sweep is read off gets one row per run of the command.
 */
class RunComputeTravelTimeMatrixTest {

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
	void itCachesTheBaseMatrixAndReportsWhatItCost() throws IOException {

		Path output = Path.of(utils.getOutputDirectory(), "matrix");

		new RunComputeTravelTimeMatrix().execute(
			"--network", this.runOutput.resolve("output_network.xml.gz").toString(),
			"--congestion-index", this.congestionIndex.toString(),
			"--output", output.toString(),
			"--cell-size", "5000",
			"--benchmark-queries", "1000");

		// the thresholds are in the name, so that a sweep cannot read back the cache of another cell size
		Path cache = output.resolve("free_speed_matrix_cell5000_nb5000.bin");
		assertTrue(Files.exists(cache), "the expensive part is cached for every later run");
		assertTrue(Files.size(cache) > 0);

		List<String> summary = Files.readAllLines(output.resolve("matrix_summary.csv"));
		assertEquals(2, summary.size(), "a header and one row");
		assertTrue(summary.get(0).startsWith("variant,cell_size,max_neighbor_distance,links,nodes,zones,"));
		assertTrue(summary.get(1).startsWith("GLOBAL_INDEX,5000,"));
	}

	@Test
	void aSweepOverTheThresholdsAppendsToTheSameSummary() throws IOException {

		Path output = Path.of(utils.getOutputDirectory(), "sweep");

		for (String cellSize : List.of("2000", "5000", "10000")) {
			new RunComputeTravelTimeMatrix().execute(
				"--network", this.runOutput.resolve("output_network.xml.gz").toString(),
				"--congestion-index", this.congestionIndex.toString(),
				"--output", output.toString(),
				"--variant", "GLOBAL_INDEX",
				"--cell-size", cellSize,
				"--benchmark-queries", "0");
		}

		List<String> summary = Files.readAllLines(output.resolve("matrix_summary.csv"));
		assertEquals(4, summary.size(), "a header and one row per cell size");
		assertTrue(summary.get(1).startsWith("GLOBAL_INDEX,2000,"));
		assertTrue(summary.get(3).startsWith("GLOBAL_INDEX,10000,"));
		assertTrue(summary.get(1).endsWith(","), "no query time was measured with --benchmark-queries 0");

		// one cache per setting, which is what keeps the sweep from reading back the wrong matrix
		for (String cellSize : List.of("2000", "5000", "10000")) {
			assertTrue(Files.exists(output.resolve("free_speed_matrix_cell" + cellSize + "_nb5000.bin")),
				"the cache of cell size " + cellSize);
		}
	}

	@Test
	void itBuildsTheFreeSpeedVariantWithoutACongestionIndex() {

		Path output = Path.of(utils.getOutputDirectory(), "free-speed");

		new RunComputeTravelTimeMatrix().execute(
			"--network", this.runOutput.resolve("output_network.xml.gz").toString(),
			"--output", output.toString(),
			"--variant", "FREE_SPEED",
			"--benchmark-queries", "0");

		assertTrue(Files.exists(output.resolve("free_speed_matrix_cell1000_nb5000.bin")));
	}
}
