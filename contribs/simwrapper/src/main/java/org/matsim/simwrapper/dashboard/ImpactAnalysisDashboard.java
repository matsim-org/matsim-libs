package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.impact.ImpactAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Table;

/**
 * Dashboard with general overview.
 */
public class ImpactAnalysisDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Impact Analysis";
		header.description = "Impact overview of the MATSim run.";

		layout.row("links")
			.el(Table.class, (viz, data) -> {
				viz.title = "Kenngrößen";
				viz.dataset = data.compute(ImpactAnalysis.class, "data.csv");
				viz.enableFilter = false;
				viz.showAllRows = true;
				viz.width = 1d;

			});
	}

}
