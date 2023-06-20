package org.matsim.contrib.drt.extension.dashboards;

import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import picocli.CommandLine;

@CommandLine.Command(
	name = "drt-post-process",
	description = "Creates additional files for drt dashboards."
)
@CommandSpec(
	requireRunDirectory = true,
	produces = "",
	group = "drt"
)
public class DrtAnalysisPostProcessing implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(DrtAnalysisPostProcessing.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(DrtAnalysisPostProcessing.class);

	@CommandLine.Option(names = "--drt-mode", required = true, description = "Name of the drt mode to analyze.")
	private String drtMode;

	@Override
	public Integer call() throws Exception {
		return 0;
	}
}
