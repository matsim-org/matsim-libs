package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.traffic.CountComparisonAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Scatter;

public class CountCamparisonDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Count Comparsion Dashboard";
		header.description = "Comparsion of observed and simulated daily traffic volumes";

		layout.row("scatterplot").el(Scatter.class, ((viz, data) -> {
			viz.dataset = data.compute(CountComparisonAnalysis.class, "count_comparison.csv");

			viz.title = "Observed and simulated daily traffic volumes";

			viz.legendName = "Road type";

			viz.x = "observed_traffic_volume";
			viz.y = "simulated_traffic_volume";

			viz.xAxisName = "Observed traffic volume";
			viz.yAxisName = "Simulated traffic volume";
		}));
	}
}
