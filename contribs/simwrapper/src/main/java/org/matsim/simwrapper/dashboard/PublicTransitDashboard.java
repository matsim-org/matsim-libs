package org.matsim.simwrapper.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.TransitViewer;

/**
 * Standard dashboard for public transit.
 */
public class PublicTransitDashboard implements Dashboard {

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Public Transit";
		header.tab = "PT";
		header.triggerPattern = "*output_transitSchedule*xml*";

		layout.row("viewer").el(TransitViewer.class, (viz, data) -> {
			viz.title = "Transit Viewer";
			viz.height = 12d;
			viz.description = "Visualize the transit schedule.";
			viz.network = "*output_network.xml.gz";
			viz.transitSchedule = data.output("*output_transitSchedule.xml.gz");
		});
	}
}
