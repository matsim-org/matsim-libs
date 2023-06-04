package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.LogFileAnalysis;
import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;

import java.util.List;

public class EmissionsDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Emissions Dashbaord";
		header.description = "Shows the emissions on different maps";

		layout.row("links")
			.el(Links.class, (viz, data) -> {
				viz.title = "Emissions per Link per Meter";
				viz.description =  "Displays the emissions for each link per meter.";
				viz.height = 12.;
				viz.datasets.csvFile = data.output("analysis/emissions/emissions_per_link_per_m.csv");
				viz.network = data.output("kelheim-mini.output_network.xml.gz");
				viz.display.color.columnName ="CO2_TOTAL [g/m]";
				viz.display.color.dataset = "csvFile";
				data.compute(AirPollutionAnalysis.class, "emissions_per_link_per_m.csv");
				viz.display.width.scaleFactor = 1;
				viz.display.width.columnName = "CO2_TOTAL [g/m]";
				viz.display.width.dataset = "csvFile";
			});

		layout.row("links")
			.el(XYTime.class, (viz, data) -> {
				viz.title = "Emissions over Time";
				viz.description =  "Displays the emissions over time.";
				viz.height = 12.;
				data.compute(AirPollutionAnalysis.class, "emissions_grid_per_day.xyt.csv");
				viz.file = data.output("analysis/emissions/emissions_grid_per_day.xyt.csv");
			});

	}
}
