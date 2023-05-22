package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.LogFileAnalysis;
import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Line;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Table;

import java.util.List;

public class EmissionsDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Emissions Dashbaord";
		header.description = "Shows the emissions on different maps";

		layout.row("map")
				.el(MapPlot.class, (viz, data) -> {
					viz.title = "Run Info";
//					viz.showAllRows = true;
					data.compute(AirPollutionAnalysis.class, "emissions_grid_per_day.csv");
					viz.width = 1d;
				});

	}
}
