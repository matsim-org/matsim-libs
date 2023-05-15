package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.population.StuckAgentAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;

import java.util.List;

// TODO: doc
public class StuckAgentDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Stuck Agents";
		header.description = "Analyze agents that are 'stuck' i.e. could not finish their daily plan.";

		layout.row("first").el(Tile.class, (viz, data) -> {
			viz.title = "";
			viz.dataset = data.compute(StuckAgentAnalysis.class, "stuck_agents.csv");
			viz.height = 0.1;
		});

		layout.row("second")
			// FIXME: rewrite using plotly plugin
			.el(PieChart.class, (viz, data) -> {
				viz.title = "Stuck Agents per Transport Mode";
				viz.dataset = data.compute(StuckAgentAnalysis.class, "stuckAgentsPerModePieChart.csv");
				viz.useLastRow = true;
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Stuck Agents per Mode";
				viz.dataset = data.compute(StuckAgentAnalysis.class, "stuck_agents_per_mode.csv");
			});

		layout.row("third")
				.el(Bar.class, (viz, data) -> {
					viz.title = "Stuck Agents per Hour";
					viz.stacked = true;
					viz.dataset = data.compute(StuckAgentAnalysis.class, "stuck_agents_per_hour.csv");
					viz.x = "hour";
					viz.xAxisName = "Hour";
					viz.yAxisName = "# Stuck";
					viz.ignoreColumns = List.of("Total");
				})
				.el(Table.class, (viz, data) -> {
					viz.title = "Stuck Agents per Hour";
					viz.dataset = data.compute(StuckAgentAnalysis.class, "stuck_agents_per_hour.csv");
				});

		layout.row("four").el(Table.class, (viz, data) -> {
			viz.title = "Stuck Agents per Link (Top 20)";
			viz.dataset = data.compute(StuckAgentAnalysis.class, "stuck_agents_per_link.csv");
		});
	}

	@Override
	public double priority() {
		return -1;
	}
}
