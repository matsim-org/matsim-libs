package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.traffic.CountComparisonAnalysis;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Plotly;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Creates a dashboard for comparison of simulated and observed traffic volumes.
 */
public class TrafficCountsDashboard implements Dashboard {

	private String[] args;

	/**
	 * Constructor with default arguments.
	 */
	public TrafficCountsDashboard() {
		args = new String[0];
	}

	/**
	 * Constructor with custom limits and categories.
	 */
	public TrafficCountsDashboard(Collection<Double> limits, Collection<String> labels) {

		args = new String[]{
			"--limits", limits.stream().map(String::valueOf).collect(Collectors.joining(",")),
			"--labels", String.join(",", labels)
		};
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Traffic Counts";
		header.description = "Comparison of observed and simulated daily traffic volumes";

		// TODO: generate description for labels and limits

		layout.row("overview")
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Count estimation quality";
				viz.description = "over all count stations";

				Plotly.DataSet ds = viz.addDataset(data.compute(CountComparisonAnalysis.class, "count_comparison_quality.csv"))
					.aggregate(List.of("quality"), "n", Plotly.AggrFunc.SUM)
					// No need to show axis label
					.constant("source", "");

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.xAxis(Axis.builder().title("Number").build())
					.build();

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).orientation(BarTrace.Orientation.HORIZONTAL).build(), ds.mapping()
					.x("n")
					.y("source")
					.name("quality", Plotly.ColorScheme.RdYlBu)
				);
			}).el(Plotly.class, (viz, data) -> {

				viz.title = "Count estimation quality";
				viz.description = "by road type";

				Plotly.DataSet ds = viz.addDataset(data.compute(CountComparisonAnalysis.class, "count_comparison_quality.csv"));

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.yAxis(Axis.builder().title("Share").build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(), ds.mapping()
					.x("road_type")
					.y("share")
					.name("quality", Plotly.ColorScheme.RdYlBu)
				);

			});

		layout.row("map")
			.el(MapPlot.class, (viz, data) -> {
				viz.title = "Relative traffic volumes";
				viz.height = 8.0;

				viz.center = data.context().getCenter();
				viz.shapes = data.compute(CreateGeoJsonNetwork.class, "network.geojson", "--with-properties");

				viz.datasets.counts = data.compute(CountComparisonAnalysis.class, "count_comparison_daily.csv", args);

				viz.display.fill.dataset = data.compute(CountComparisonAnalysis.class, "count_comparison_daily.csv");
				viz.display.fill.columnName = "quality";
				viz.display.fill.colorRamp.steps = 5;
				viz.display.fill.colorRamp.ramp = "Viridis";

			})
			.el(Plotly.class, (viz, data) -> {

				Plotly.DataSet ds = viz.addDataset(data.compute(CountComparisonAnalysis.class, "count_comparison_daily.csv"));

				viz.title = "Daily traffic volumes";
				viz.description = "simulated vs. observed";

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Observed traffic count").build())
					.yAxis(Axis.builder().title("Simulated traffic count").build())
					.build();

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).build(), ds.mapping()
					.x("observed_traffic_volume")
					.y("simulated_traffic_volume")
					.text("name")
					.name("road_type")
				);

			});
	}
}
