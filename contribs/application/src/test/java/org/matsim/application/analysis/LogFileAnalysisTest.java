package org.matsim.application.analysis;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimApplication;
import org.matsim.application.MATSimApplicationTest;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LogFileAnalysisTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void output() throws IOException {

		Config config = ConfigUtils.createConfig();

		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setLastIteration(2);

		int execute = MATSimApplication.execute(MATSimApplicationTest.TestScenario.class, config);

		Assertions.assertThat(execute)
			.isEqualTo(0);

		Path out = Path.of(config.controller().getOutputDirectory());
		new LogFileAnalysis().execute(
			"--input", ApplicationUtils.matchInput("logfile.log", out).toString(),
			"--output-memory-stats", out.resolve("mem_stats.csv").toString(),
			"--output-run-info", out.resolve("run_info.csv").toString(),
			"--output-runtime-stats", out.resolve("runtime_stats.csv").toString()
		);


		Assertions.assertThat(Files.readAllLines(out.resolve("mem_stats.csv")))
			.hasSizeGreaterThan(1);

		Assertions.assertThat(Files.readAllLines(out.resolve("runtime_stats.csv")))
			.hasSize(4);

		Assertions.assertThat(Files.readAllLines(out.resolve("run_info.csv")))
			.hasSizeGreaterThan(8);

	}
}
