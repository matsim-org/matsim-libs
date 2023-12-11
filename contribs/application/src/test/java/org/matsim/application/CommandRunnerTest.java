package org.matsim.application;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.analysis.TestDependentAnalysis;
import org.matsim.application.analysis.TestOtherAnalysis;
import org.matsim.application.analysis.TestOtherDependentAnalysis;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;

public class CommandRunnerTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void runner() {

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
