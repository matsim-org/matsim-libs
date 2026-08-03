package org.matsim.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.options.SampleOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests core command-line and run behavior of {@link MATSimApplication}.
 */
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

		assertThat(ret).isEqualTo(0);
	}

	@Test
	void run_noConfig() {
		Assertions.assertThrows(NullPointerException.class, () -> MATSimApplication.execute(TestScenario.class));
	}

	/**
	 * Test scenario used by application tests and contrib analysis tests.
	 */
	public static final class TestScenario extends MATSimApplication {

		@CommandLine.Mixin
		private SampleOptions sample = new SampleOptions(1, 10, 25);

		public TestScenario(Config config) {
			super(config);
		}

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
