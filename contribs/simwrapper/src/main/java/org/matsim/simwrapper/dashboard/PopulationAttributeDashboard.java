package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.population.PopulationAttributeAnalysis;
import org.matsim.application.analysis.population.StuckAgentAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Bar;
import org.matsim.simwrapper.viz.Table;

public class PopulationAttributeDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Population Attribute";
		header.description = "Analyze the attributes of the population.";

		layout.row("first")
				.el(Bar.class, (viz, data) -> {
					viz.title = "Stuck Agents per Hour";
					viz.stacked = false;
					viz.dataset = data.compute(PopulationAttributeAnalysis.class, "amount_per_age_group.csv");
					viz.x = "Age";
					viz.xAxisName = "Age";
					viz.yAxisName = "Amount";
				});



	}
}
