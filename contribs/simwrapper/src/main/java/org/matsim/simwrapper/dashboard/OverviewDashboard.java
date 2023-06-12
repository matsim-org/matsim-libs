package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.LogFileAnalysis;
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

		layout.row("first")
			.el(Table.class, (viz, data) -> {
				viz.title = "Run Info";
				viz.showAllRows = true;
				viz.dataset = data.compute(LogFileAnalysis.class, "run_info.csv");
				viz.width = 1d;
			})
			.el(Line.class, (viz, data) -> {

				viz.title = "Score";
				viz.dataset = data.output("*.scorestats.txt");
				viz.description = "per Iteration";
				viz.x = "ITERATION";
				viz.columns = List.of("avg. EXECUTED", "avg. WORST", "avg. BEST");
				viz.xAxisName = "Iteration";
				viz.yAxisName = "Score";
				viz.width = 2d;

			});

		layout.row("warnings")
			.el(TextBlock.class, (viz, data) -> {
				viz.file = data.compute(LogFileAnalysis.class, "status.md");
				// Force minimal height
				viz.height = 0.1d;
			});

		/*
		Not yet finalized in SimWrapper
		layout.row("config")
			.el(XML.class, (viz, data) -> {
				viz.file = data.output("*.output_config.xml");
				viz.height = 5d;
			});
		 */

		// modeshare + mode progression
		layout.row("second")
			.el(PieChart.class, (viz, data) -> {
				viz.title = "Mode Share";
				viz.description = "at final Iteration";
				viz.dataset = data.output("*.modestats.txt");
				viz.ignoreColumns = List.of("Iteration");
				viz.useLastRow = true;
				viz.width = 1d;

			})
			.el(Area.class, (viz, data) -> {
				viz.title = "Mode Share Progression";
				viz.description = "per Iteration";
				viz.dataset = data.output("*.modestats.txt");
				viz.x = "Iteration";
				viz.xAxisName = "Iteration";
				viz.yAxisName = "Share";
				viz.width = 2d;
			});

		layout.row("third")
			.el(Bar.class, (viz, data) -> {
				viz.title = "Runtime";
				viz.x = "Iteration";
				viz.xAxisName = "Iteration";
				viz.yAxisName = "Runtime [s]";
				viz.columns = List.of("seconds");
				viz.dataset = data.compute(LogFileAnalysis.class, "runtime_stats.csv");

			})
			.el(Plotly.class, (viz, data) -> {
				viz.title = "Memory Usage";

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.xAxis(Axis.builder().title("Time").build())
					.yAxis(Axis.builder().title("MB").build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
					.build();

				viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
					viz.addDataset(data.compute(LogFileAnalysis.class, "memory_stats.csv"))
						.pivot(List.of("time"), "names", "values")
						.mapping()
						.name("names")
						.x("time")
						.y("values")
				);
			});
	}

	@Override
	public double priority() {
		return 1;
	}
}
