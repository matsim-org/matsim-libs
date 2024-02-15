package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.noise.NoiseAnalysis;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.Table;

/**
 * Shows emission in the scenario.
 */
public class NoiseDashboard implements Dashboard {
	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Noise";
		header.description = "Shows the noise footprint and spatial distribution.";


		layout.row("links")
			.el(GridMap.class, (viz, data) -> {

				viz.title = "Noise Immissions";

				viz.description = "Noise Immissions per hour";
				viz.height = 12.0;
				viz.projection = "EPSG:25832";
				viz.cellSize = 250;
				viz.opacity = 0.2;
				viz.maxHeight = 20;
				viz.valueColumn = "immission";
				viz.setColorRamp("greenRed", 10, false);
				viz.file = data.compute(NoiseAnalysis.class, "noise-analysisimmission_consideredAgentUnits_damages_receiverPoint_merged_xyt.csv.gz", "--input-crs", "EPSG:25832");

			});

	}
}
