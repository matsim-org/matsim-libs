package org.matsim.application.analysis;

import org.matsim.application.CommandSpec;
import org.matsim.application.Dependency;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import picocli.CommandLine;

import java.nio.file.Files;

@CommandSpec(
	produces = "processed.csv",
	dependsOn = @Dependency(value = TestAnalysis.class, files = {"out.xml"}, required = true)
)
public class TestOtherDependentAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(TestOtherDependentAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(TestOtherDependentAnalysis.class);

	@Override
	public Integer call() throws Exception {

		Files.writeString(output.getPath(), "something");

		return 0;
	}
}
