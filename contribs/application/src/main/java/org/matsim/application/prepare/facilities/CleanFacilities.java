package org.matsim.application.prepare.facilities;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

@CommandLine.Command(name = "clean-facilities", description = "Remove information from facilities file.", mixinStandardHelpOptions = true, showDefaultValues = true)
public class CleanFacilities implements MATSimAppCommand {
	@CommandLine.Option(names = "--facilities", description = "Input facilities file", required = true)
	private String facilities;

	@CommandLine.Option(names = "--remove-link-ids", description = "Remove link ids from facilities", defaultValue = "false")
	private boolean rmLinkIds;

	@CommandLine.Option(names = "--output", description = "Output file name", required = true)
	private Path output;

	static void main(String[] args) {
		System.exit(new CommandLine(new CleanFacilities()).execute(args));
	}

	public CleanFacilities() {
	}

	public CleanFacilities(boolean rmLinkIds) {
		this.rmLinkIds = rmLinkIds;
	}

	@Override
	public Integer call() throws Exception {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimFacilitiesReader(scenario).readFile(facilities);
		clean(scenario.getActivityFacilities());

		if (output.getParent() != null) {
			Files.createDirectories(output.getParent());
		}

		new FacilitiesWriter(scenario.getActivityFacilities()).write(output.toString());

		return 0;
	}

	public void clean(ActivityFacilities facilities) {
		if (rmLinkIds) {
			facilities.getFacilities().values().forEach(facility -> facility.setLinkId(null));
		}
	}
}
