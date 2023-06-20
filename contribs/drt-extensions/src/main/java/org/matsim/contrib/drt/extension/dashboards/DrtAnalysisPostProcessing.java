package org.matsim.contrib.drt.extension.dashboards;

import org.matsim.api.core.v01.Scenario;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CrsOptions;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import picocli.CommandLine;

import java.net.URL;
import java.util.Collection;

@CommandLine.Command(
	name = "drt-post-process",
	description = "Creates additional files for drt dashboards."
)
@CommandSpec(
	requireRunDirectory = true,
	produces = {"kpi.csv", "stops.shp", "trips_per_stop.csv", "od.csv"},
	group = "drt"
)
public final class DrtAnalysisPostProcessing implements MATSimAppCommand {

	@CommandLine.Mixin
	private final InputOptions input = InputOptions.ofCommand(DrtAnalysisPostProcessing.class);
	@CommandLine.Mixin
	private final OutputOptions output = OutputOptions.ofCommand(DrtAnalysisPostProcessing.class);

	@CommandLine.Mixin
	private CrsOptions crs;

	@CommandLine.Option(names = "--drt-mode", required = true, description = "Name of the drt mode to analyze.")
	private String drtMode;

	@CommandLine.Option(names = "--stop-file", description = "URL to drt stop file")
	private URL stopFile;

	public static void main(String[] args) {
		new DrtAnalysisPostProcessing().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Collection<TransitStopFacility> stops = readTransitStop(stopFile);

		System.out.println(stops);

		// TODO: create stop .shp file


		return 0;
	}

	private Collection<TransitStopFacility> readTransitStop(URL stopFile) {

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readURL(stopFile);

		return scenario.getTransitSchedule().getFacilities().values();
	}

}
