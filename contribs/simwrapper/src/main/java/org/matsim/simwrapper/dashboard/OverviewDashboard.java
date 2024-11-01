package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.LogFileAnalysis;
import org.matsim.application.analysis.traffic.TrafficAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;

import java.util.List;

/**
 * Dashboard with general overview.
 */
public class OverviewDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Overview";
		header.description = "General overview of the MATSim run.";

		layout.row("first").el(Table.class, (viz, data) -> {
			viz.title = "Run Info";
			viz.showAllRows = true;
			viz.dataset = data.compute(LogFileAnalysis.class, "run_info.csv");
			viz.width = 1d;
		}).el(MapPlot.class, (viz, data) -> {

			viz.title = "Simulated traffic volume";
			viz.center = data.context().getCenter();
			viz.zoom = data.context().mapZoomLevel;
			viz.height = 7.5;
			viz.width = 2.0;

			viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro", "--with-properties"), "linkId");
			viz.addDataset("traffic", data.compute(TrafficAnalysis.class, "traffic_stats_by_link_daily.csv"));

			viz.display.lineColor.dataset = "traffic";
			viz.display.lineColor.columnName = "simulated_traffic_volume";
			viz.display.lineColor.join = "link_id";
			viz.display.lineColor.setColorRamp(ColorScheme.RdYlBu, 5, true);

			viz.display.lineWidth.dataset = "traffic";
			viz.display.lineWidth.columnName = "simulated_traffic_volume";
			viz.display.lineWidth.scaleFactor = 20000d;
			viz.display.lineWidth.join = "link_id";


		});

		// Info about the status of the run
		layout.row("warnings").el(TextBlock.class, (viz, data) -> {
			viz.file = data.compute(LogFileAnalysis.class, "status.md");
		});

		layout.row("config").el(XML.class, (viz, data) -> {
			viz.file = data.output("(*.)?output_config.xml");
			viz.height = 6d;
			viz.width = 2d;
			viz.unfoldLevel = 1;

		}).el(PieChart.class, (viz, data) -> {
			viz.title = "Mode Share";
			viz.description = "at final Iteration";
			viz.dataset = data.output("(*.)?modestats.csv");
			viz.ignoreColumns = List.of("iteration");
			viz.useLastRow = true;
		});


		layout.row("second").el(Line.class, (viz, data) -> {

			viz.title = "Score";
			viz.dataset = data.output("(*.)?scorestats.csv");
			viz.description = "per Iteration";
			viz.x = "iteration";
			viz.columns = List.of("avg_executed", "avg_worst", "avg_best");
			viz.xAxisName = "Iteration";
			viz.yAxisName = "Score";

		});

		layout.row("third")
			.el(Area.class, (viz, data) -> {
				viz.title = "Mode Share Progression";
				viz.description = "per Iteration";
				viz.dataset = data.output("(*.)?modestats.csv");
				viz.x = "iteration";
				viz.xAxisName = "Iteration";
				viz.yAxisName = "Share";
				viz.width = 2d;
			});

		layout.row("perf").el(Bar.class, (viz, data) -> {
			viz.title = "Runtime";
			viz.x = "Iteration";
			viz.xAxisName = "Iteration";
			viz.yAxisName = "Runtime [s]";
			viz.columns = List.of("seconds");
			viz.dataset = data.compute(LogFileAnalysis.class, "runtime_stats.csv");

		}).el(Plotly.class, (viz, data) -> {
			viz.title = "Memory Usage";

			viz.layout = tech.tablesaw.plotly.components.Layout.builder().xAxis(Axis.builder().title("Time").build()).yAxis(Axis.builder().title("MB").build()).barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK).build();

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(), viz.addDataset(data.compute(LogFileAnalysis.class, "memory_stats.csv")).pivot(List.of("time"), "names", "values").mapping().name("names").x("time").y("values"));
		});
	}

	@Override
	public double priority() {
		return 1;
	}
}
