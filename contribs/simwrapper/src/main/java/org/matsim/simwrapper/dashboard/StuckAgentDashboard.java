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

		header.title = "Stuck Agents";
		header.description = "Analyze agents that are 'stuck' i.e. could not finish their daily plan.";

		layout.row("first").el(TextBlock.class, (viz, data) -> {
			viz.title = "";
			viz.file = "table1.md";
			viz.height = 1.;
		});

		layout.row("second")
				.el(PieChart.class, (viz, data) -> {
					viz.title = "Stuck Agents per Transport Mode";
					viz.dataset = data.compute(StuckAgentAnalysis.class, "stuckAgentsPerMode.csv");
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
					viz.dataset = data.compute(StuckAgentAnalysis.class, "stuckAgentsPerHour.csv");
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
