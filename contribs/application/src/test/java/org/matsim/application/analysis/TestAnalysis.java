package org.matsim.application.analysis;

import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.ShpOptions;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test command that writes content to file.
 */
@CommandSpec(requires = {"test.csv", "stats.csv"}, requireNetwork = true, produces = "out.xml")
public class TestAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(TestAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(TestAnalysis.class);

	@CommandLine.Mixin
	private final ShpOptions shp = new ShpOptions();

	@Override
	public Integer call() throws Exception {

		if (!Files.exists(Path.of(input.getPath("test.csv"))))
			return 2;

		if (!Files.exists(Path.of(input.getPath("stats.csv"))))
			return 2;

		Files.writeString(output.getPath(), "Fixed Content");

		return 0;
	}
}
