package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.traffic.CountComparisonAnalysis;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Links;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Scatter;


/**
 * Creates a dashboard for comparison of simulated and observed traffic volumes.
 */
public class TrafficCountsDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Traffic Counts";
		header.description = "Comparison of observed and simulated daily traffic volumes";

		layout.row("map")
				.el(MapPlot.class, (viz, data) -> {
					viz.title = "Relative traffic volumes";
					viz.height = 8.0;

					viz.center = data.context().getCenter();
					viz.shapes = data.compute(CreateGeoJsonNetwork.class, "network.geojson");

					viz.datasets.counts = data.compute(CountComparisonAnalysis.class, "count_comparison_total.csv");

					viz.display.lineWidth.dataset = data.compute(CountComparisonAnalysis.class, "count_comparison_total.csv");
					viz.display.lineWidth.columnName = "rel_error";
					viz.display.lineWidth.join = "link_id";
					viz.display.lineWidth.scaleFactor = 1000;

					viz.display.fill.dataset = data.compute(CountComparisonAnalysis.class, "count_comparison_total.csv");
					viz.display.fill.columnName = "rel_error";
					viz.display.fill.colorRamp.steps = 3;
					viz.display.fill.colorRamp.ramp = "Viridis";

				});

		layout.row("scatterplot")
				.el(Scatter.class, (viz, data) -> {
					viz.dataset = data.compute(CountComparisonAnalysis.class, "count_comparison_total.csv");

					viz.title = "Observed and simulated daily traffic volumes";

					viz.legendName = "Road type";

					viz.x = "observed_traffic_volume";
					viz.y = "simulated_traffic_volume";

					viz.xAxisName = "Observed traffic volume";
					viz.yAxisName = "Simulated traffic volume";
				});

	}
}
