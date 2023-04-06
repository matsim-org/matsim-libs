package org.matsim.simwrapper.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.analysis.StuckAgentAnalysis;
import org.matsim.simwrapper.viz.Bar;
import org.matsim.simwrapper.viz.PieChart;
import org.matsim.simwrapper.viz.TextBlock;

// TODO: doc
public class StuckAgentDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Stuck Agents Dashboard";
		header.description = "Description for the Stuck Agents Dashboard";


		layout.row("first").el(TextBlock.class, (viz, data) -> {
			viz.title = "";
			viz.file = data.compute(StuckAgentAnalysis.class, "table1.md");
			viz.height = 1.;
		});

		layout.row("second")
				.el(PieChart.class, (viz, data) -> {
					viz.title = "Stuck Agents per Transport Mode";
					viz.dataset = "piechart.csv";
					viz.useLastRow = "true";
				})
				.el(TextBlock.class, (viz, data) -> {
					viz.title = "";
					viz.file = "table2.md";
				});

		layout.row("third")
				.el(Bar.class, (viz, data) -> {
					viz.title = "Stuck Agents per Hour";
					viz.stacked = "true";
					viz.dataset = "stuckAgentsPerHour.csv";
					viz.x = "hour";
					viz.xAxisName = "Hour";
					viz.yAxisName = "# Stuck";
				})
				.el(TextBlock.class, (viz, data) -> {
					viz.title = "";
					viz.file = "table3.md";
				});

		layout.row("four").el(TextBlock.class, (viz, data) -> {
			viz.title = "Stuck Agents per Link (Top 20)";
			viz.file = "table4.md";
		});
	}
}
