package org.matsim.contrib.drt.extension.dashboards;


import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;

import java.net.URL;

/**
 * Dashboard for detailed insight into DRT service.
 */
public class DrtDetailedDashboard implements Dashboard {

	private final String mode;
	private final URL transitStopFile;

	public DrtDetailedDashboard(String mode, URL transitStopFile) {
		this.mode = mode;
		this.transitStopFile = transitStopFile;
	}

	/**
	 * Auto generate title.
	 */
	public String getTitle() {
		String name = context();
		return name.isBlank() ? "DRT (Detailed)" : name + " (Detailed)";

	}

	@Override
	public String context() {
		return mode.replace("drt", "").replace("Drt", "");
	}

	@Override
	public void configure(Header header, Layout layout) {
		header.title = getTitle();
	}

	@Override
	public double priority() {
		return -0.5;
	}
}
