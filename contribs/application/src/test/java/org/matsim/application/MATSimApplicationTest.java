package org.matsim.application;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.prepare.freight.tripExtraction.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.population.GenerateShortDistanceTrips;
import org.matsim.application.prepare.population.MergePopulations;
import org.matsim.application.prepare.population.TrajectoryToPlans;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestUtils;
import picocli.CommandLine;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MATSimApplicationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void help() {

		int ret = MATSimApplication.execute(TestScenario.class, "--help");

		assertEquals(0, ret, "Return code should be 0");
	}

	@Test
	void config() {

		Controler controler = MATSimApplication.prepare(TestScenario.class, ConfigUtils.createConfig(),
				"-c:controler.runId=Test123", "--config:global.numberOfThreads=4", "--config:plans.inputCRS", "EPSG:1234");

		Config config = controler.getConfig();

		assertThat(config.controller().getRunId()).isEqualTo("Test123");
		assertThat(config.global().getNumberOfThreads()).isEqualTo(4);
		assertThat(config.plans().getInputCRS()).isEqualTo("EPSG:1234");

	}

	@Test
	void yaml() {

		Path yml = Path.of(utils.getClassInputDirectory(), "specs.yml");

		Controler controler = MATSimApplication.prepare(TestScenario.class, ConfigUtils.createConfig(), "--yaml", yml.toString());

		assertThat(controler.getConfig().controller().getRunId())
				.isEqualTo("567");

		ScoringConfigGroup score = controler.getConfig().scoring();

		ScoringConfigGroup.ScoringParameterSet params = score.getScoringParameters(null);

		assertThat(params.getOrCreateModeParams("car").getConstant())
				.isEqualTo(-1);

		assertThat(params.getOrCreateModeParams("bike").getConstant())
				.isEqualTo(-2);

	}

	@Test
	void sample() {

		Controler controler = MATSimApplication.prepare(TestScenario.class, ConfigUtils.createConfig(),
				"--10pct");

		assertThat(controler.getConfig().controller().getRunId())
				.isEqualTo("run-10pct");

		controler = MATSimApplication.prepare(TestScenario.class, ConfigUtils.createConfig());

		assertThat(controler.getConfig().controller().getRunId())
				.isEqualTo("run-25pct");

	}

	@Test
	void population() throws MalformedURLException {

		Path input = Path.of(utils.getClassInputDirectory());
		Path output = Path.of(utils.getOutputDirectory());

		assertThat(input.resolve("persons.xml")).exists();

		MATSimApplication.execute(TestScenario.class, "prepare", "trajectory-to-plans",
				"--samples", "0.5", "0.1",
				"--sample-size", "1.0",
				"--name", "test",
				"--population", input.resolve("persons.xml").toString(),
				"--attributes", input.resolve("attributes.xml").toString(),
				"--output", output.toString()
		);

		Path plans = output.resolve("test-100pct.plans.xml.gz");

		assertThat(plans).exists();
		assertThat(output.resolve("test-50pct.plans.xml.gz")).exists();

		MATSimApplication.execute(TestScenario.class, "prepare", "generate-short-distance-trips",
				"--population", plans.toString(),
				"--num-trips", "2"
		);

		var actualContent = IOUtils.getInputStream(
				output.resolve("test-100pct.plans-with-trips.xml.gz").toUri().toURL());
		var expectedContent = IOUtils.getInputStream(
				input.resolve("test-100pct.plans-with-trips.xml.gz").toUri().toURL());

		assertThat(IOUtils.isEqual(actualContent, expectedContent)).isTrue();
	}

	@Test
	@Disabled("Class is deprecated")
	void freight() {

		Path input = Path.of("..", "..", "..", "..",
				"shared-svn", "komodnext", "data", "freight", "original_data").toAbsolutePath().normalize();

		Assumptions.assumeTrue(Files.exists(input));

		Path output = Path.of(utils.getOutputDirectory());

		String network = input.resolve("german-primary-road.network.xml.gz").toString();

		String allFreightTrips = output.resolve("german-wide-freight-trips.xml.gz").toString();
		MATSimApplication.execute(TestScenario.class, "prepare", "generate-german-freight-trips",
				input.toString(),
				"--sample", "0.25",
				"--network", network,
				"--input-crs", "EPSG:5677",
				"--output", allFreightTrips
		);

		String freightTrips = output.resolve("freight-trips.xml.gz").toString();
		MATSimApplication.execute(TestScenario.class, "prepare", "extract-freight-trips",
				allFreightTrips,
				"--network", network,
				"--shp", input.resolve("../DusseldorfBoundary/newDusseldorfBoundary.shp").toString(),
				"--input-crs", "EPSG:5677",
				"--target-crs", "EPSG:25832",
				"--output", freightTrips
		);

	}

	@Test
	void run() {

		Config config = ConfigUtils.createConfig();
		Path out = Path.of(utils.getOutputDirectory()).resolve("out");

		config.controller().setOutputDirectory(out.toString());
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(1);

		int ret = MATSimApplication.execute(TestScenario.class, config);

		// Content defined in the post process section
		assertThat(out.resolve("test.txt"))
				.hasContent("Inhalt");

	}

	@MATSimApplication.Prepare({
			TrajectoryToPlans.class, GenerateShortDistanceTrips.class, ExtractRelevantFreightTrips.class, MergePopulations.class
	})
	public static final class TestScenario extends MATSimApplication {

		@CommandLine.Mixin
		private SampleOptions sample = new SampleOptions(1, 10, 25);

		public TestScenario(Config config) {
			super(config);
		}

		// Public constructor is required to run the class
		public TestScenario() {
		}

		@Override
		protected Config prepareConfig(Config config) {

			config.controller().setRunId(sample.adjustName("run-25pct"));
			return config;
		}


		@Override
		protected List<MATSimAppCommand> preparePostProcessing(Path outputFolder, String runId) {
			return List.of(new TestCommand(outputFolder.resolve("test.txt"), "Inhalt"));
		}
	}

}
