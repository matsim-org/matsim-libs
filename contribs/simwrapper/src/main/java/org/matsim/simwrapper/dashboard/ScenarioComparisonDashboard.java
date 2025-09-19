package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.scenarioComparison.ScenarioComparisonAnalysis;
import org.matsim.simwrapper.ComparisonDashboard;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Tile;

public class ScenarioComparisonDashboard implements ComparisonDashboard {

	private String pathToBaseCase;

	@Override
	public String getPathToBaseCase() {
		return pathToBaseCase;
	}

	@Override
	public void setPathToBaseCase(String path) {
		this.pathToBaseCase = path;
	}

	private String constructorBasePath;

	public ScenarioComparisonDashboard(String basePath) {
		constructorBasePath = basePath;
	}

	public void configure(Header header, Layout layout) {

		header.title = "Scenario Comparison: Policy to Base Case";
		header.description = "Shows the differences in a variety of metrics between the policy and base case.";

		layout.row("trip stats")
			.el(Tile.class, (viz, data) -> {
				viz.title = "Difference in Time & Distance Traveled per Mode (KMs and Hours)";
				viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "difference_trips.csv", "--input-base-path=" + constructorBasePath);
				viz.height = 0.1;
			});

		layout.row("emission stats")
			.el(Tile.class, (viz, data) -> {
				viz.title = "Difference in Emissions (KGs)";
				viz.dataset = data.compute(ScenarioComparisonAnalysis.class, "difference_emissions.csv", "--input-base-path=" + constructorBasePath);
				viz.height = 0.1;
			});

	}

	//	priority is set to a lower number in order to force this class to be executed after population and emissions folders are already generated
	@Override
	public double priority() {
		return -20;
	}

}
