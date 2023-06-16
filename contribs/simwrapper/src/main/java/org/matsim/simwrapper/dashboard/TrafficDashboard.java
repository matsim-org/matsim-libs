package org.matsim.simwrapper.dashboard;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.analysis.traffic.TrafficAnalysis;
import org.matsim.application.prepare.network.CreateGeoJsonNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Links;
import org.matsim.simwrapper.viz.Plotly;
import org.matsim.simwrapper.viz.Table;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.util.Set;

/**
 * Dashboard to show traffic statistics.
 */
public class TrafficDashboard implements Dashboard {

	private final Set<String> modes;

	public TrafficDashboard() {
		this(Set.of(TransportMode.car, "freight"));
	}

	public TrafficDashboard(Set<String> modes) {
		this.modes = modes;
	}

	@Override
	public void configure(Header header, Layout layout) {

		String[] args = new String[]{"--transport-modes", String.join(",", this.modes)};

		header.title = "Car Traffic";
		header.description = "Traffic related analyses.";

		layout.row("index_by_hour").el(Plotly.class, (viz, data) -> {

				viz.title = "Network congestion index";
				viz.description = "by hour";

				Plotly.DataSet ds = viz.addDataset(data.compute(TrafficAnalysis.class, "traffic_stats_by_road_type_and_hour.csv", args));

				viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.yAxis(Axis.builder().title("Index").build())
					.xAxis(Axis.builder().title("Hour").build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.OVERLAY)
					.build();

				viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).mode(ScatterTrace.Mode.LINE).build(), ds.mapping()
					.x("hour")
					.y("congestion_index")
					.name("road_type", Plotly.ColorScheme.Spectral)
				);
			})
			.el(Table.class, ((viz, data) -> {

				viz.title = "Traffic stats per road type";
				viz.description = "daily";

				viz.dataset = data.compute(TrafficAnalysis.class, "traffic_stats_by_road_type_daily.csv", args);

				viz.showAllRows = true;
				viz.enableFilter = false;
			}));

		// TODO: not working ideally, should be converted to map viz
		layout.row("map").el(Links.class, (viz, data) -> {

			viz.network = data.compute(CreateGeoJsonNetwork.class, "network.geojson");
			viz.datasets.csvBase = data.compute(TrafficAnalysis.class, "traffic_stats_by_link_daily.csv", args);
			viz.center = data.context().getCenter();

			viz.display.color.columnName = "avg_speed";
			viz.display.width.columnName = "avg_speed";

			viz.height = 12d;
		});

	}
}
