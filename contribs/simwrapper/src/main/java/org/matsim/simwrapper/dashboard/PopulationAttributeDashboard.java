package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.population.PopulationAttributeAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Bar;
import org.matsim.simwrapper.viz.PieChart;
import org.matsim.simwrapper.viz.Tile;

/**
 * Shows attributes distribution of the population.
 */
public class PopulationAttributeDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Population Attribute";
		header.description = "Analyze the attributes of the population.";

		layout.row("first")
				.el(Tile.class, (viz, data) -> {
					viz.title = "";
					viz.dataset = data.compute(PopulationAttributeAnalysis.class, "total_agents.csv");
					viz.height = 0.1;
				});

		layout.row("second")
				.el(Bar.class, (viz, data) -> {
					viz.title = "Agents per age group";
					viz.stacked = false;
					viz.dataset = data.compute(PopulationAttributeAnalysis.class, "amount_per_age_group.csv");
					viz.x = "Age";
					viz.xAxisName = "Age (≤)";
					viz.yAxisName = "Amount";
				})
				.el(PieChart.class, (viz, data) -> {
					viz.title = "Agents per sex group";
					viz.dataset = data.compute(PopulationAttributeAnalysis.class, "amount_per_sex_group.csv");
					viz.useLastRow = true;
				});

		layout.row("third")
				.el(Bar.class, (viz, data) -> {
					viz.title = "Average Income per Age Group";
					viz.stacked = false;
					viz.dataset = data.compute(PopulationAttributeAnalysis.class, "average_income_per_age_group.csv");
					viz.x = "Age";
					viz.xAxisName = "Age (≤)";
					viz.yAxisName = "avg. Income";
				});
	}
}
