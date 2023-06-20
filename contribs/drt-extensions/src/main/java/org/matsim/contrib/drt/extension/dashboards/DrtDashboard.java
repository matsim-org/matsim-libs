package org.matsim.contrib.drt.extension.dashboards;


import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;

import java.net.URL;

/**
 * General DRT dashboard for one specific service.
 */
public class DrtDashboard implements Dashboard {

	private final String mode;
	private final URL transitStopFile;

	public DrtDashboard(String mode, URL transitStopFile) {
		this.mode = mode;
		this.transitStopFile = transitStopFile;
	}

	/**
	 * Auto generate title.
	 */
	public String getTitle() {
		String name = context();
		return name.isBlank() ? "DRT" : "DRT - " + name;
	}

	@Override
	public String context() {
		return mode.replace("drt", "").replace("Drt", "");
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = getTitle();

	}
}
