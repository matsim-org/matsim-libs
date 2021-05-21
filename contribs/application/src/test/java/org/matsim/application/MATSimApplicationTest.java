package org.matsim.application;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.prepare.freight.ExtractRelevantFreightTrips;
import org.matsim.application.prepare.freight.GenerateGermanWideFreightTrips;
import org.matsim.application.prepare.population.GenerateShortDistanceTrips;
import org.matsim.application.prepare.population.MergePopulations;
import org.matsim.application.prepare.population.TrajectoryToPlans;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class MATSimApplicationTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void help() {

		int ret = MATSimApplication.execute(TestScenario.class, "--help");

		assertEquals("Return code should be 0", 0, ret);
	}

	@Test
	public void config() {

		Controler controler = MATSimApplication.prepare(TestScenario.class, ConfigUtils.createConfig(),
				"-c:controler.runId=Test123", "--config:global.numberOfThreads=4");

		Config config = controler.getConfig();

		assertThat(config.controler().getRunId()).isEqualTo("Test123");
		assertThat(config.global().getNumberOfThreads()).isEqualTo(4);

	}

	@Test
	public void yaml() {

		Path yml = Path.of(utils.getClassInputDirectory(), "specs.yml");

		Controler controler = MATSimApplication.prepare(TestScenario.class, ConfigUtils.createConfig(), "--yaml", yml.toString());

		assertThat(controler.getConfig().controler().getRunId())
				.isEqualTo("567");

		PlanCalcScoreConfigGroup score = controler.getConfig().planCalcScore();

		PlanCalcScoreConfigGroup.ScoringParameterSet params = score.getScoringParameters(null);

		assertThat(params.getOrCreateModeParams("car").getConstant())
				.isEqualTo(-1);

        assertThat(params.getOrCreateModeParams("bike").getConstant())
                .isEqualTo(-2);

	}

	@Test
	public void sample() {

		Controler controler = MATSimApplication.prepare(TestScenario.class, ConfigUtils.createConfig(),
				"--10pct");

		assertThat(controler.getConfig().controler().getRunId())
				.isEqualTo("run-10pct");
	}

	@Test
	public void population() {

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


		Path result = output.resolve("test-100pct.plans-with-trips.xml.gz");

		assertThat(result)
				.exists()
				.hasSameBinaryContentAs(input.resolve("test-100pct.plans-with-trips.xml.gz"));

	}

	@Test
	public void freight() {

		Path input = Path.of("..", "..", "..", "..",
				"shared-svn", "komodnext", "data", "freight", "original_data").toAbsolutePath().normalize();

		Assume.assumeTrue(Files.exists(input));

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

	@MATSimApplication.Prepare({
			TrajectoryToPlans.class, GenerateShortDistanceTrips.class,
			GenerateGermanWideFreightTrips.class, ExtractRelevantFreightTrips.class, MergePopulations.class
	})
	private static final class TestScenario extends MATSimApplication {

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

			config.controler().setRunId(sample.adjustName("run-1pct"));

			return config;
		}
	}

}