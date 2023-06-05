package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.traffic.CongestionAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Line;
import org.matsim.simwrapper.viz.Plotly;
import org.matsim.simwrapper.viz.Table;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;

import java.util.ArrayList;

public class TravelTimeDashboard implements Dashboard {

	private final int startTime;
	private final int maxTime;
	private final int timeSlice;

	public TravelTimeDashboard(){
		this.startTime = 0;
		this.maxTime = 86400;
		this.timeSlice = 900;
	}

	public TravelTimeDashboard(int startTime, int maxTime, int timeSlice){

		this.startTime = startTime;
		this.maxTime = maxTime;
		this.timeSlice = timeSlice;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Travel times and congestion";
		header.description = "Analysis of super important stuff";

		layout.row("index_by_hour").el(Plotly.class, (viz, data) -> {

			viz.title = "Network congestion index";

			viz.description = "by time";

			Plotly.DataSet ds = viz.addDataset(data.compute(CongestionAnalysis.class, "traffic_stats_by_road_type_and_hour.csv"));

			viz.layout = tech.tablesaw.plotly.components.Layout.builder()
					.yAxis(Axis.builder().title("Index").build())
					.xAxis(Axis.builder().title("Time in seconds").build())
					.barMode(tech.tablesaw.plotly.components.Layout.BarMode.OVERLAY)
					.build();

			viz.addTrace(ScatterTrace.builder(Plotly.INPUT, Plotly.INPUT).mode(ScatterTrace.Mode.LINE).build(), ds.mapping()
					.x("time")
					.y("congestion_index")
					.name("road_type", Plotly.ColorScheme.RdYlBu)
			);
		})
				.el(Table.class, ((viz, data) -> {

					viz.title = "Traffic stats per road type";

					viz.dataset = data.compute(CongestionAnalysis.class, "traffic_stats_by_road_type_daily.csv");

					viz.showAllRows = true;
					viz.enableFilter = false;
				}));
	}
}
