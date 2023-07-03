package org.matsim.application.analysis;

import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import picocli.CommandLine;

/**
 * Test command that writes content to file.
 */
@CommandSpec(requires = {"test.csv", "stats.csv"}, requireNetwork = true, produces = "out.xml")
public class TestOtherAnalysis implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(TestOtherAnalysis.class);

	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(TestOtherAnalysis.class);

	@CommandLine.Option(names = "--option", description = "Testing option")
	private int option;

	@Override
	public Integer call() throws Exception {
		return 0;
	}
}
