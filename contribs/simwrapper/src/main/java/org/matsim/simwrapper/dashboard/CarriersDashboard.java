package org.matsim.simwrapper.dashboard;

import org.matsim.freight.carriers.analysis.CarriersAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.*;

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

		layout.row("first")
			.el(Tile.class, (viz, data) -> {
				viz.dataset = data.compute(CarriersAnalysis.class, "Carriers_stats.tsv", args); //Kann ich hier nicht auch einfach ne berechneten Wert nehmen?
				viz.height = 0.1;
			});

	}
}
