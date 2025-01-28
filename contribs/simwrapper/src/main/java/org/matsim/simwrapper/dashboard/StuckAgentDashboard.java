package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.population.StuckAgentAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Bar;
import org.matsim.simwrapper.viz.Plotly;
import org.matsim.simwrapper.viz.Table;
import org.matsim.simwrapper.viz.Tile;
import tech.tablesaw.plotly.traces.PieTrace;

import java.util.List;

/**
 * Dashboard for stuck agents.
 */
public class StuckAgentDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Stuck Agents";
		header.description = "Analyze agents that are 'stuck' i.e. could not finish their daily plan.";

		layout.row("first").el(Tile.class, (viz, data) -> {
			viz.dataset = data.compute(StuckAgentAnalysis.class, "stuck_agents.csv");
			viz.height = 0.1;
		});

		layout.row("second")
			.el(Plotly.class, (viz, data) -> {
				viz.title = "Stuck Agents";
				viz.description = "per Mode";

				Plotly.DataSet ds = viz.addDataset(data.compute(StuckAgentAnalysis.class, "stuck_agents_per_mode.csv"));
				viz.addTrace(PieTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(), ds.mapping()
					.text("Mode")
					.x("Agents")
				);

			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Stuck Agents";
				viz.description = "per Mode";

				viz.dataset = data.compute(StuckAgentAnalysis.class, "stuck_agents_per_mode.csv");
			});

		layout.row("third")
			.el(Bar.class, (viz, data) -> {
				viz.title = "Stuck Agents";
				viz.description = "per hour";

				viz.stacked = true;
				viz.dataset = data.compute(StuckAgentAnalysis.class, "stuck_agents_per_hour.csv");
				viz.x = "hour";
				viz.xAxisName = "Hour";
				viz.yAxisName = "# Stuck";
				viz.columns = List.of("pt","walk");
			})
			.el(Table.class, (viz, data) -> {
				viz.title = "Stuck Agents";
				viz.description = "per hour";
				viz.dataset = data.compute(StuckAgentAnalysis.class, "stuck_agents_per_hour.csv");
			});

		layout.row("four").el(Table.class, (viz, data) -> {
			viz.title = "Stuck Agents";
			viz.description = "per Link (Top 20)";
			viz.dataset = data.compute(StuckAgentAnalysis.class, "stuck_agents_per_link.csv");
		});
	}

	@Override
	public double priority() {
		return -10;
	}
}
