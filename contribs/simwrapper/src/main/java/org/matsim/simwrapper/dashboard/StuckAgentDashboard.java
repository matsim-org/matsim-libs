package org.matsim.simwrapper.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.TextBlock;


// TODO: doc
public class StuckAgentDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Stuck Agents Dashboard";
		header.description = "Description for the Stuck Agents Dashboard";

		layout.row("first", TextBlock.class, (viz, data) -> {
			viz.title = "Stats";
			viz.file = "stats.md";

		});
	}
}
