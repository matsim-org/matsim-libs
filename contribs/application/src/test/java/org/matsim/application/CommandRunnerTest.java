package org.matsim.application;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.analysis.TestDependentAnalysis;
import org.matsim.application.analysis.TestOtherAnalysis;
import org.matsim.application.analysis.TestOtherDependentAnalysis;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;

public class CommandRunnerTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void runner() {

		Path path = Path.of(utils.getOutputDirectory());

		CommandRunner runner = new CommandRunner().setOutput(path);
		runner.add(TestDependentAnalysis.class);
		runner.add(TestOtherAnalysis.class, "--option", "1");
		runner.add(TestOtherDependentAnalysis.class);

		runner.run(Path.of(utils.getInputDirectory()));

		// Results will go into analysis subdirectory because of the commands package names

		Assertions.assertThat(path.resolve("analysis"))
				.isDirectoryContaining(p -> p.getFileName().toString().equals("out.xml"))
				.isDirectoryContaining(p -> p.getFileName().toString().equals("processed.csv"));
	}
}
