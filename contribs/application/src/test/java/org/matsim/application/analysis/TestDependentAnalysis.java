package org.matsim.application.analysis;

import org.matsim.application.CommandSpec;
import org.matsim.application.Dependency;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

@CommandSpec(
	produces = "processed.csv",
	dependsOn = {@Dependency(value = TestAnalysis.class, files = {"out.xml"}, required = true)}
)
public class TestDependentAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(TestDependentAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(TestDependentAnalysis.class);

	@Override
	public Integer call() throws Exception {

		String s = Files.readString(Path.of(input.getPath("out.xml")));

		if (!s.equals("Fixed Content"))
			throw new IllegalStateException("Content in out.xml not as expected.");

		return 0;
	}
}
