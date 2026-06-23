package org.matsim.simwrapper.dashboard;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.analysis.traffic.TrafficAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.*;
import org.matsim.simwrapper.viz.*;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.util.Set;

/**
 * Dashboard to show traffic statistics.
 */
public class TrafficDashboard implements Dashboard {

	private final Set<String> modes;

	public TrafficDashboard() {
		this(Set.of(TransportMode.car));
	}

	public TrafficDashboard(Set<String> modes) {
		this.modes = modes;
	}

	@Override
	public void configure(Header header, Layout layout, SimWrapperConfigGroup configGroup) {

		String[] args = new String[]{"--transport-modes", String.join(",", this.modes)};
		if (modes.size() == 1)
			header.title = modes.stream().findFirst().get() + " Traffic";
		else
			header.title = "Network Traffic";
		header.description = "Traffic related analyses for the modes " + modes + ". Volumes for PT are not shown in this dashboard.";

		layout.row("index_by_hour")
			.el(Plotly.class, (viz, data) -> {

				viz.title = "Network excess travel time index";
				viz.description = "by hour";

				Plotly.DataSet ds = viz.addDataset(data.compute(TrafficAnalysis.class, "traffic_stats_by_road_type_and_hour.csv", args));

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.yAxis(Axis.builder().title("Index").build())
					.xAxis(Axis.builder().title("Hour").build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.OVERLAY)
					.build();

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).mode(ScatterTrace.Mode.LINE).build(), ds.mapping()
					.x("hour")
					.y("excess_travel_time_index")
					.name("road_type", ColorScheme.Spectral)
				);
			})
//			.el(Plotly.class, (viz, data) -> {
//
//				viz.title = "Network congestion index (deprecated)";
//				viz.description = "by hour";
//
//				Plotly.DataSet ds = viz.addDataset(data.compute(TrafficAnalysis.class, "traffic_stats_by_road_type_and_hour.csv", args));
//
//				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
//					.yAxis(Axis.builder().title("Index").build())
//					.xAxis(Axis.builder().title("Hour").build())
//					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.OVERLAY)
//					.build();
//
//				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).mode(ScatterTrace.Mode.LINE).build(), ds.mapping()
//					.x("hour")
//					.y("congestion_index")
//					.name("road_type", ColorScheme.Spectral)
//				);
//			})
			.el(Table.class, ((viz, data) -> {

				viz.title = "Traffic stats per road type";
				viz.description = "daily";

				viz.dataset = data.compute(TrafficAnalysis.class, "traffic_stats_by_road_type_daily.csv", args);

				viz.showAllRows = true;
				viz.enableFilter = false;
			}));

//		// TODO: Could be done per mode, by using the tab feature

		layout.row("map").el(MapPlot.class, (viz, data) -> {

			viz.title = "Traffic statistics";
			viz.description = DashboardUtils.adjustDescriptionBasedOnSampling("Volume for the modes " + modes + " (value: simulated_traffic_volume). For the different modes the volumes can set in the config in the plot.", data, true);
			viz.center = data.context().getCenter();
			viz.zoom = data.context().getMapZoomLevel();

			viz.setShape(data.compute(CreateAvroNetwork.class, "network.avro"), "id");

			viz.addDataset("traffic", data.compute(TrafficAnalysis.class, "traffic_stats_by_link_daily.csv"));

			viz.display.lineColor.dataset = "traffic";
			viz.display.lineColor.columnName = "avg_speed";
			viz.display.lineColor.join = "link_id";
			viz.display.lineColor.setColorRamp(ColorScheme.RdYlBu, 5, false);

			viz.display.lineWidth.dataset = "traffic";
			viz.display.lineWidth.columnName = "simulated_traffic_volume";
			viz.display.lineWidth.scaleFactor = 20000d;
			viz.display.lineWidth.join = "link_id";

			viz.height = 12d;
		});


		layout.row("info").el(TextBlock.class, (viz, data) -> {
			viz.backgroundColor = "transparent";
			viz.content = """
				### Notes
				- The excess travel time index of a link is the ratio of the expected extra travel time on a link during the given period of time. This value is normalized to the free speed travel time of the link.\s
				When it comes to the network index, the absolute excess travel time for each link are first summed up, and then normalized to the summed free speed travel time. The traffic volume is considered when summing up. \s
				The idea is based on the TomTom travel time index. For example, an excess travel time index of 0.2 means 20% of extra travel time is expected, compared to the free flow condition.\s
			\t
				- Note: The "congestion index" used in previous versions is not recommended by VSP, it is therefore replaced by the "excess travel time index" described above.
			\t""";
		});

		// reference for the "congestion index" used in previous versions: Feifei He, Xuedong Yan, Yang Liu, Lu Ma, 2016,
		// A Traffic Congestion Assessment Method for Urban Road Networks Based on Speed Performance Index,
		//Procedia Engineering, Volume 137, Pages 425-433, ISSN 1877-7058, https://doi.org/10.1016/j.proeng.2016.01.277.
	}
}
