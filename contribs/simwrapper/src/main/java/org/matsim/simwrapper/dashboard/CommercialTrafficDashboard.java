package org.matsim.simwrapper.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.TextBlock;

/**
 *  Dashboard to show commercial traffic statistics.
 */
public class CommercialTrafficDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {
		header.title = "Commercial Traffic";
		header.description = "Commercial Traffic related analyses.";

		layout.row("info").el(TextBlock.class, (viz, data) -> {
			viz.backgroundColor = "transparent";
			viz.content = """
				### Notes
				- The speed performance index is the ratio of average travel speed and the maximum permissible road speed.
				A performance index of 0.5, means that the average speed is half of the maximum permissible speed. A road with a performance index below 0.5 is considered to be in a congested state.
				- The congestion index is the ratio of time a road is in an uncongested state. 0.5 means that a road is congested half of the time. A road with 1.0 is always uncongested.

				cf. *A Traffic Congestion Assessment Method for Urban Road Networks Based on Speed Performance Index* by Feifei He, Xuedong Yan*, Yang Liu, Lu Ma.
				""";
		});
	}
}
