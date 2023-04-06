package org.matsim.simwrapper;

import org.matsim.application.MATSimAppCommand;
import picocli.CommandLine;

@CommandLine.Command(
		name = "simwrapper",
		description = "Run SimWrapper on existing folders and generate dashboard files"
)
public class SimWrapperRunner implements MATSimAppCommand {

	@Override
	public Integer call() throws Exception {
		// TODO: implementation

		// TODO: configuration of dashboards

		//

		return 0;
	}

}
