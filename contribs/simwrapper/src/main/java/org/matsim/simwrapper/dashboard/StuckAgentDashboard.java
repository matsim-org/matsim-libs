package org.matsim.simwrapper.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.analysis.StuckAgentAnalysis;
import org.matsim.simwrapper.viz.Bar;
import org.matsim.simwrapper.viz.PieChart;
import org.matsim.simwrapper.viz.Table;
import org.matsim.simwrapper.viz.TextBlock;

// TODO: doc
public class StuckAgentDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Stuck Agents";
		header.description = "Description for the Stuck Agents Dashboard";


		layout.row("first").el(TextBlock.class, (viz, data) -> {
			viz.title = "";
			viz.file = data.compute(StuckAgentAnalysis.class, "header.md");
			viz.height = 1.;
		});

		layout.row("second")
				.el(PieChart.class, (viz, data) -> {
					viz.title = "Stuck Agents per Transport Mode";
					viz.dataset = data.compute(StuckAgentAnalysis.class, "stuckAgentsPerModePieChart.csv");
					viz.useLastRow = "true";
				})
				.el(Table.class, (viz, data) -> {
					viz.title = "Stuck Agents per Mode";
					viz.dataset = data.compute(StuckAgentAnalysis.class, "stuckAgentsPerMode.csv");
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
				.el(Table.class, (viz, data) -> {
					viz.title = "Stuck Agents per Hour";
					viz.dataset = data.compute(StuckAgentAnalysis.class, "stuckAgentsPerHour.csv");
				});

		layout.row("four").el(Table.class, (viz, data) -> {
			viz.title = "Stuck Agents per Link (Top 20)";
			viz.dataset = data.compute(StuckAgentAnalysis.class, "stuckAgentsPerLink.csv");
		});
	}
}
