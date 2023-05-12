package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.traffic.CountComparisonAnalysis;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Bar;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Scatter;


/**
 * Creates a dashboard for comparison of simulated and observed traffic volumes.
 */
public class TrafficCountsDashboard implements Dashboard {

	private final String optionLowerLimit;
	private String optionUpperLimit;

	public TrafficCountsDashboard() {
		optionLowerLimit = "--lower-limit=" + 0.8;
		optionUpperLimit = "--upper-limit=" + 1.2;
	}

	public TrafficCountsDashboard(double lowerLimit, double upperLimit) {
		optionLowerLimit = "--lower-limit=" + lowerLimit;
		optionUpperLimit = "--upper-limit=" + upperLimit;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Traffic Counts";
		header.description = "Comparison of observed and simulated daily traffic volumes";

		layout.row("map")
				.el(MapPlot.class, (viz, data) -> {
					viz.title = "Relative traffic volumes";
					viz.height = 8.0;

					viz.center = data.context().getCenter();
					viz.shapes = data.compute(CreateGeoJsonNetwork.class, "network.geojson", "--with-properties");

					viz.datasets.counts = data.compute(CountComparisonAnalysis.class, "count_comparison_total.csv", optionUpperLimit, optionLowerLimit);

					viz.display.fill.dataset = data.compute(CountComparisonAnalysis.class, "count_comparison_total.csv");
					viz.display.fill.columnName = "quality";
					viz.display.fill.colorRamp.steps = 5;
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
				})
				.el(Bar.class, (viz, data) -> {
					viz.dataset = data.compute(CountComparisonAnalysis.class, "estimation_quality.csv");

					viz.title = "Model estimation quality";

					viz.x = "quality";
					viz.stacked = false;

					viz.yAxisName = "Number";
				});
	}
}
