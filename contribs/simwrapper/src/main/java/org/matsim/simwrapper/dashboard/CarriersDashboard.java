package org.matsim.simwrapper.dashboard;

import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;

import java.util.ArrayList;
import java.util.List;

public class CarriersDashboard implements Dashboard {
	private final String basePath;

	CarriersDashboard(String basePath) {
		if (!basePath.endsWith("/")) {
			basePath += "/";
		}
		this.basePath = basePath;
	}
/**
*
 * @param header
 * @param layout
*/
	@Override
	public void configure(Header header, Layout layout) {
		header.title = "Carriers Analyse";
		header.description = "Shows statistics about the carriers in the scenario.";

		String[] args = new ArrayList<>(List.of("--base-path", basePath)).toArray(new String[0]);

	}
}
