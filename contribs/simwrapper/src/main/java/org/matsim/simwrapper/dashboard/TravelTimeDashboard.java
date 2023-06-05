package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.traffic.CongestionAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.Line;

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

		layout.row("test_row").el(Line.class, (viz, data) -> {

			viz.title = "Overall network congestion index";
			viz.description = "by time";

			viz.dataset = data.compute(CongestionAnalysis.class, "network_congestion_total.csv");

			viz.title = "Congestion index";
			viz.x = "time";
			viz.columns = new ArrayList<>();
			viz.columns.add("congestion_index");
		});
	}
}
