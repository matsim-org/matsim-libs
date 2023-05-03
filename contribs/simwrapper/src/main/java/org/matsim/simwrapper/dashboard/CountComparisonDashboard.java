package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.traffic.CountComparisonAnalysis;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Links;
import org.matsim.simwrapper.viz.Scatter;


/**
 * Creates a dashboard for comparison of simulated and observed traffic volumes.
 */
public class CountComparisonDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Count Comparsion Dashboard";
		header.description = "Comparsion of observed and simulated daily traffic volumes";

		layout.row("map")
			.el(Links.class, (viz, data) -> {

				viz.title = "Relative traffic volumes";
				viz.height = 8d;

				viz.network = data.compute(CreateGeoJsonNetwork.class, "network.geojson");

				viz.datasets.csvFile = data.compute(CountComparisonAnalysis.class, "count_comparison_total.csv");
			});

		layout.row("scatterplot").el(Scatter.class, ((viz, data) -> {
					viz.dataset = data.compute(CountComparisonAnalysis.class, "count_comparison_total.csv");

					viz.title = "Observed and simulated daily traffic volumes";

					viz.legendName = "Road type";

					viz.x = "observed_traffic_volume";
					viz.y = "simulated_traffic_volume";

					viz.xAxisName = "Observed traffic volume";
					viz.yAxisName = "Simulated traffic volume";
				}));

	}
}
