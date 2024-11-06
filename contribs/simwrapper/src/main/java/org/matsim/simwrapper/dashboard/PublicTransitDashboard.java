package org.matsim.simwrapper.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.TransitViewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard dashboard for public transit.
 */
public class PublicTransitDashboard implements Dashboard {

	private List<TransitViewer.CustomRouteType> customRouteTypes = new ArrayList<>();

	/**
	 * Add custom route types to the transit viewer.
	 */
	public PublicTransitDashboard withCustomRouteTypes(TransitViewer.CustomRouteType... custom) {
		customRouteTypes.addAll(List.of(custom));
		return this;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Public Transit";
		header.tab = "PT";
		header.triggerPattern = "(*.)?output_transitSchedule*xml*";

		layout.row("viewer").el(TransitViewer.class, (viz, data) -> {
			viz.title = "Transit Viewer";
			viz.height = 12d;
			viz.description = "Visualize the transit schedule.";
			viz.network = "(*.)?output_network.xml.gz";
			viz.transitSchedule = data.output("(*.)?output_transitSchedule.xml.gz");

			if (!customRouteTypes.isEmpty())
				viz.customRouteTypes = customRouteTypes;
		});
	}
}
