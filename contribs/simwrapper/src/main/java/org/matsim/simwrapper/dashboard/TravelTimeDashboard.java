package org.matsim.simwrapper.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;

public class TravelTimeDashboard implements Dashboard {


	private final int startTime;
	private final int maxTime;
	private final int timeSlice;

	public TravelTimeDashboard(int startTime, int maxTime, int timeSlice){

		this.startTime = startTime;
		this.maxTime = maxTime;
		this.timeSlice = timeSlice;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Travel times and congestion";
		header.description = "Analysis of super important stuff";

		//layout.row();
	}
}
