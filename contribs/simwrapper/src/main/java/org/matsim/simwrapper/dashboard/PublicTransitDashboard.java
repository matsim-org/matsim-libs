package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.pt.PublicTransitAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.FlowMap;
import org.matsim.simwrapper.viz.TransitViewer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

			// Include a network that has not been filtered
			viz.network = data.withContext("all").compute(CreateAvroNetwork.class, "network.avro",
				"--mode-filter", "", "--shp", "none");

			viz.transitSchedule = data.output("(*.)?output_transitSchedule.xml.gz");
			viz.demand = data.compute(PublicTransitAnalysis.class, "pt_pax_volumes.csv.gz");

			if (!customRouteTypes.isEmpty())
				viz.customRouteTypes = customRouteTypes;
		});

		layout.row("flowmap").el(FlowMap.class, (viz, data) -> {
			viz.title = "Flow Map";
			viz.description = "Visualize the flows of different metrics";
			FlowMap.Metrics metrics = new FlowMap.Metrics();
			metrics.setZoom(9.5);
			metrics.setLabel("headway metric");
			metrics.setDataset("analysis/pt/pt_headway_per_stop_area_pair_and_hour.csv");
			metrics.setOrigin("stopAreaOrStop");
			metrics.setDestination("stopAreaOrStopNext");
			metrics.setFlow("meanHeadway");
			metrics.setColorScheme("BurgYl");
			metrics.setValueTransform(FlowMap.Metrics.ValueTransform.INVERSE);
			viz.metrics.add(metrics);
		});
	}
}
