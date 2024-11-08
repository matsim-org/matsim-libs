package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.pt.PtSupplyStatistics;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.Plotly;
import org.matsim.simwrapper.viz.Table;
import org.matsim.simwrapper.viz.TransitViewer;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;

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

		layout.row("ptSupplyStatistics").el(Table.class, (viz, data) -> {
			viz.title = "Number of scheduled pt services";
			viz.description = "per mode";
			viz.showAllRows = true;
			viz.dataset = data.compute(PtSupplyStatistics.class, "pt_count_unique_ids_per_mode.csv");
			viz.width = 1d;
		}).el(Plotly.class, (viz, data) -> {

			viz.title = "Departures at stops";
			viz.description = "by hour and transit mode";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Hour").build())
				.yAxis(Axis.builder().title("Departures at stops").build())
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
				viz.addDataset(data.compute(PtSupplyStatistics.class, "pt_departures_at_stops_per_hour_per_mode.csv")).mapping()
					.name("transportMode", ColorScheme.Spectral)
					.x("departureHour")
					.y("Count")
			);

		});
	}
}
