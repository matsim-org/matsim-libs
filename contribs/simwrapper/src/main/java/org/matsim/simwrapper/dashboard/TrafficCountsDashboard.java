package org.matsim.simwrapper.dashboard;

import org.apache.commons.lang.StringUtils;
import org.matsim.application.analysis.traffic.CountComparisonAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Plotly;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Creates a dashboard for comparison of simulated and observed traffic volumes.
 */
public class TrafficCountsDashboard implements Dashboard {


	private final Map<String, Set<String>> networkModes = new LinkedHashMap<>();

	private List<Double> limits = List.of(0.6, 0.8, 1.2, 1.4);
	private List<String> labels = List.of("major under", "under", "ok", "over", "major over");

	private String countsPath = null;

	/**
	 * Constructor with default arguments. Will analyze all modes
	 */
	public TrafficCountsDashboard() {
	}

	/**
	 * Registered modes to be compared separately. This will overwrite the default behaviour and result in one tab per node,
	 */
	public TrafficCountsDashboard withModes(String name, Set<String> modes) {
		this.networkModes.put(name, modes);
		return this;
	}

	/**
	 * Overwrite the default counts file. Otherwise, output counts are used.
	 */
	public TrafficCountsDashboard withCountsPath(String countsPath) {
		this.countsPath = countsPath;
		return this;
	}

	/**
	 * Set the quality thresholds and labels.
	 *
	 * @param limits thresholds for labels
	 * @param labels text representation of labels
	 * @return same instance
	 */
	public TrafficCountsDashboard withQualityLabels(List<Double> limits, List<String> labels) {
		this.limits = limits;
		this.labels = labels;
		return this;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Traffic Counts";
		header.description = "Comparison of observed and simulated daily traffic volumes. Reported volumes are scaled-up according to simulated sample size. \nError metrics based on relative error: ";

		for (int i = 0; i < labels.size(); i++) {
			if (i == 0)
				header.description += String.format(Locale.US, "%s: < %.2f; ", labels.get(i), limits.get(i));
			else if (i == labels.size() - 1)
				header.description += String.format(Locale.US, "%s: > %.2f", labels.get(i), limits.get(i - 1));
			else
				header.description += String.format(Locale.US, "%s: %.2f - %.2f; ", labels.get(i), limits.get(i - 1), limits.get(i));
		}

		List<String> argList = new ArrayList<>(List.of(
			"--limits", limits.stream().map(String::valueOf).collect(Collectors.joining(",")),
			"--labels", String.join(",", labels)
		));

		if (countsPath != null)
			argList.addAll(List.of("--counts", countsPath));


		// Default analysis for cars
		if (networkModes.isEmpty())
			createTab(layout, argList, null, null);

		else {

			// One tab for each requested mode
			for (Map.Entry<String, Set<String>> e : networkModes.entrySet()) {
				createTab(layout, argList, e.getKey(), e.getValue());
			}
		}
	}

	private void createTab(Layout layout, List<String> argList, String tabName, Set<String> modes) {

		// Copy list to avoid side effects
		argList = new ArrayList<>(argList);

		if (modes != null)
			argList.addAll(List.of("--network-mode", String.join(",", modes)));

		String[] args = argList.toArray(new String[0]);

		String suffix = tabName == null ? "" : ("_" + tabName.toLowerCase());
		String context = tabName == null ? null : context() + tabName.toLowerCase();

		// Name with capital letters
		tabName = tabName == null ? null : StringUtils.capitalize(tabName);

		layout.row("overview" + suffix, tabName)
			.el(context, Plotly.class, (viz, data) -> {

				viz.title = "Count estimation quality";
				viz.description = "over all count stations";

				Plotly.DataSet ds = viz.addDataset(data.compute(CountComparisonAnalysis.class, "count_comparison_quality.csv", args))
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
					.name("quality", ColorScheme.RdYlBu)
				);
			}).el(context, Plotly.class, (viz, data) -> {

				viz.title = "Count estimation quality";
				viz.description = "by road type";

				Plotly.DataSet ds = viz.addDataset(data.compute(CountComparisonAnalysis.class, "count_comparison_quality.csv", args));

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.yAxis(Axis.builder().title("Share").build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(), ds.mapping()
					.x("road_type")
					.y("share")
					.name("quality", ColorScheme.RdYlBu)
				);

			});

		layout.row("scatter" + suffix, tabName)
			.el(context, Plotly.class, (viz, data) -> {

				Plotly.DataSet ds = viz.addDataset(data.compute(CountComparisonAnalysis.class, "count_comparison_by_hour.csv", args));

				viz.title = "Traffic volumes by hour";
				viz.description = "simulated vs. observed";
				viz.fixedRatio = true;
				viz.interactive = Plotly.Interactive.slider;
				viz.height = 8.0;


				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Observed traffic count").build())
					.yAxis(Axis.builder().title("Simulated traffic count").build())
					.build();

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).build(), ds.mapping()
					.x("observed_traffic_volume")
					.y("simulated_traffic_volume")
					.text("name")
					.name("hour")
				);

			})
			.el(context, Plotly.class, (viz, data) -> {

				Plotly.DataSet ds = viz.addDataset(data.compute(CountComparisonAnalysis.class, "count_comparison_daily.csv", args));

				viz.title = "Daily traffic volumes";
				viz.description = "simulated vs. observed";
				viz.fixedRatio = true;
				viz.height = 8.0;

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

		layout.row("map" + suffix, tabName)
			.el(context, MapPlot.class, (viz, data) -> {
				viz.title = "Relative traffic volumes";
				viz.height = 8.0;

				viz.setShape(data.withDefaultContext().compute(CreateAvroNetwork.class, "network.avro", "--with-properties"), "id");
				viz.addDataset("counts", data.compute(CountComparisonAnalysis.class, "count_comparison_daily.csv", args));

				viz.center = data.withDefaultContext().context().getCenter();
				viz.zoom = data.withDefaultContext().context().mapZoomLevel;

				viz.display.lineColor.dataset = "counts";
				viz.display.lineColor.columnName = "quality";
				viz.display.lineColor.join = "link_id";
				viz.display.lineColor.setColorRamp(ColorScheme.RdYlBu, labels.size(), false);

				// 8px
				viz.display.lineWidth.dataset = "@8";
			})
			.el(context, Plotly.class, (viz, data) -> {

				viz.title = "Avg. error / bias";

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Hour").build())
					.yAxis(Axis.builder().title("Mean rel. error [%]").build())
					.yAxis2(Axis.builder().title("Mean (abs.) error [veh/h]")
						.side(Axis.Side.right)
						.overlaying(ScatterTrace.YAxis.Y)
						.build())
					.build();

				Plotly.DataSet ds = viz.addDataset(data.compute(CountComparisonAnalysis.class, "count_error_by_hour.csv", args));

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).mode(ScatterTrace.Mode.LINE)
					.name("Mean rel. error")
					.build(), ds.mapping()
					.x("hour")
					.y("mean_rel_error"));

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.mode(ScatterTrace.Mode.LINE).yAxis(ScatterTrace.YAxis.Y2)
					.name("Mean abs. error")
					.build(), ds.mapping()
					.x("hour")
					.y("mean_abs_error"));

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT)
					.mode(ScatterTrace.Mode.LINE).yAxis(ScatterTrace.YAxis.Y2)
					.name("Mean bias")
					.build(), ds.mapping()
					.x("hour")
					.y("mean_bias"));

			});

		layout.row("details" + suffix, tabName)
			.el(context, Plotly.class, (viz, data) -> {

				viz.title = "Count stations";
				viz.description = "hourly comparison";
				viz.interactive = Plotly.Interactive.dropdown;

				Plotly.DataSet ds = viz.addDataset(data.compute(CountComparisonAnalysis.class, "count_comparison_by_hour.csv", args));

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Simulated").build(), ds.mapping()
					.x("hour")
					.y("simulated_traffic_volume")
					.name("name")
				);

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).name("Observed").build(), ds.mapping()
					.x("hour")
					.y("observed_traffic_volume")
					.name("name")
				);

			});

	}

}
