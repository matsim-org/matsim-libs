package org.matsim.contrib.msp.traveltimematrix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.contrib.msp.run.RunMspScenario;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the congestion index command on the output of a real run, so that the step from events to a
 * profile is covered end to end: the events reader, the travel time and volume handlers, the road
 * classification and the csv.
 * <p>
 * The scenario is {@code equil}, which has no congestion worth speaking of and no OSM highway types on
 * its links, so this test asserts the shape of the result rather than its values - the values are what
 * {@code CongestionIndexCalculatorTest} pins down, on a network built for it.
 */
class RunComputeCongestionIndexTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	private Path runOutput;

	@BeforeEach
	void runAScenario() {

		URL context = ExamplesUtils.getTestScenarioURL("equil");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));

		this.runOutput = Path.of(utils.getOutputDirectory(), "run");
		config.controller().setOutputDirectory(this.runOutput.toString());
		config.controller().setLastIteration(0);
		config.controller().setCompressionType(ControllerConfigGroup.CompressionType.gzip);

		Controler controler = MATSimApplication.prepare(new RunMspScenario(config), config);
		controler.run();
	}

	@Test
	void itWritesAProfileThatCoversTheDay() {

		Path output = Path.of(utils.getOutputDirectory(), "congestion_index.csv");

		new RunComputeCongestionIndex().execute(
			"--network", this.runOutput.resolve("output_network.xml.gz").toString(),
			"--events", this.runOutput.resolve("output_events.xml.gz").toString(),
			"--road-classes", "NONE",
			"--output", output.toString());

		assertTrue(Files.exists(output));

		CongestionIndexProfile profile = CongestionIndexProfile.read(output.toString());
		assertEquals(900, profile.getTimeBinSize());
		assertEquals(96, profile.getNumberOfBins());
		assertEquals(1, profile.getRoadClasses().size(), "NONE measures the network-wide index alone");
		assertTrue(profile.getDailyMean(RoadClassification.ALL) >= 0,
			"the excess travel time index cannot be negative");
	}

	@Test
	void theRoadClassSplitEndsUpInTheSameFile() {

		Path output = Path.of(utils.getOutputDirectory(), "by_road_class.csv");

		new RunComputeCongestionIndex().execute(
			"--network", this.runOutput.resolve("output_network.xml.gz").toString(),
			"--events", this.runOutput.resolve("output_events.xml.gz").toString(),
			"--road-classes", "DEFAULT_GROUPS",
			"--time-bin-size", "3600",
			"--output", output.toString());

		assertTrue(Files.exists(output));

		CongestionIndexProfile profile = CongestionIndexProfile.read(output.toString());
		assertEquals(3600, profile.getTimeBinSize());
		assertEquals(24, profile.getNumberOfBins());
		// the network-wide class plus the six default groups
		assertEquals(7, profile.getRoadClasses().size());
		assertTrue(profile.getRoadClasses().contains(RoadClassification.ALL));
		assertTrue(profile.getRoadClasses().contains("motorway"));
	}
}
