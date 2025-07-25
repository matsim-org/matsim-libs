package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.difference.DifferenceAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Tile;

public class DifferenceDashboard implements Dashboard {

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
	private String constructorPolicyPath;

	public DifferenceDashboard(String basePath, String policyPath) {
		constructorBasePath = basePath;
		constructorPolicyPath = policyPath;
	}

	public void configure(Header header, Layout layout) {

		header.title = "Difference: Policy to Base Case";
		header.description = "Shows the difference car travel times & hours between the policy and base case.";

		layout.row("stats")
			.el(Tile.class, (viz, data) -> {
				viz.dataset = data.compute(DifferenceAnalysis.class, "difference.csv", "--input-base-path=" + constructorBasePath, "--input-policy-path=" + constructorPolicyPath);
				viz.height = 0.1;
			});
	}



}
