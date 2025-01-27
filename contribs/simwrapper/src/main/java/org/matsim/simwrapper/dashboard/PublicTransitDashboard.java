package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.pt.PublicTransitAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.Plotly;
import org.matsim.simwrapper.viz.Table;
import org.matsim.simwrapper.viz.TransitViewer;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

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

			// Include a network that has not been filtered
			viz.network = data.withContext("all").compute(CreateAvroNetwork.class, "network.avro",
				"--mode-filter", "", "--shp", "none");

			viz.transitSchedule = data.output("(*.)?output_transitSchedule.xml.gz");
			viz.ptStop2stopFile = data.compute(PublicTransitAnalysis.class, "pt_pax_volumes.csv.gz");

			if (!customRouteTypes.isEmpty())
				viz.customRouteTypes = customRouteTypes;
		});

		layout.row("ptSupplyStatistics").el(Table.class, (viz, data) -> {
			viz.title = "Number of scheduled pt services per mode";
			viz.description = "Stops without field stopAreaId are handled as 1 additional stop area.";
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

		layout.row("ptSupplyStatisticsByHour").el(Plotly.class, (viz, data) -> {

				viz.title = "Share of active transit lines per mode and hour";
				viz.description = "transit lines with >= 1 departure at any stop inside the shape file per hour";

				Plotly.DataSet ds = viz.addDataset(data.compute(PtSupplyStatistics.class, "pt_active_transit_lines_per_hour_per_mode_per_area.csv"));

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.yAxis(Axis.builder().title("Share").build())
					.xAxis(Axis.builder().title("Hour").build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.OVERLAY)
					.build();

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).mode(ScatterTrace.Mode.LINE).build(), ds.mapping()
					.x("departureHour")
					.y("share")
					.name("transportMode", ColorScheme.Spectral)
				);
			}).el(Plotly.class, (viz, data) -> {

			viz.title = "Share of active transit stop areas per mode and hour";
			viz.description = "Stop areas with >= 1 departure in hour / stop areas in shape file with >= 1 departure per day.";

			Plotly.DataSet ds = viz.addDataset(data.compute(PtSupplyStatistics.class, "pt_active_transit_stops_per_hour_per_mode_per_area.csv"));

			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.yAxis(Axis.builder().title("Share").build())
				.xAxis(Axis.builder().title("Hour").build())
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.OVERLAY)
				.build();

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).mode(ScatterTrace.Mode.LINE).build(), ds.mapping()
				.x("departureHour")
				.y("share")
				.name("transportMode", ColorScheme.Spectral)
			);
		});

		layout.row("ptHeadwaysByHour").el(Plotly.class, (viz, data) -> {

			viz.title = "Median Headway per line by hour";
//			viz.description = "by hour and purpose";
			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.xAxis(Axis.builder().title("Hour").build())
				.yAxis(Axis.builder().title("Count").build())
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.STACK)
				.build();

//			viz.interactive = Plotly.Interactive.dropdown;

			viz.addTrace(BarTrace.builder(Plotly.OBJ_INPUT, Plotly.INPUT).build(),
				viz.addDataset(data.compute(PtSupplyStatistics.class, "pt_headway_group_per_mode_and_hour.csv")).mapping()
					.name("headwayGroup", ColorScheme.Spectral)
					.x("departureHour")
					.y("count")
			);

		}).el(Plotly.class, (viz, data) -> {

			viz.title = "Share of active transit stop areas per mode and hour";
			viz.description = "Stop areas with >= 1 departure in hour / stop areas in shape file with >= 1 departure per day.";

			Plotly.DataSet ds = viz.addDataset(data.compute(PtSupplyStatistics.class, "pt_headway_per_mode_and_hour.csv"));

			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
				.yAxis(Axis.builder().title("Share").build())
				.xAxis(Axis.builder().title("Hour").build())
				.barMode(tech.tablesaw.plotly.components.Layout.BarMode.OVERLAY)
				.build();

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).mode(ScatterTrace.Mode.LINE).build(), ds.mapping()
				.x("departureHour")
				.y("medianPerModeOfMedianPerLineOfMedianHeadwayPerStopPair")
				.name("transportMode", ColorScheme.Spectral)
			);
		});
	}
}
