package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardUtils;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Table;

/**
 * Shows emission in the scenario.
 */
public class AccessibilityDashboard implements Dashboard {

	private final String coordinateSystem;

	/**
	 * Best provide the crs from {@link org.matsim.core.config.groups.GlobalConfigGroup}
	 * @param coordinateSystem
	 */
	public AccessibilityDashboard(String coordinateSystem) {
		this.coordinateSystem = coordinateSystem;
	}

	@Override
	public void configure(Header header, Layout layout) {

		header.title = "Accessibility";
		header.description = "Shows the air pollution footprint and its spatial distribution. Shown values are already upscaled from simulated sample size.";

		layout.row("second")
			.el(GridMap.class, (viz, data) -> {
				viz.title = "Accessibility to X";
				viz.unit = "Utils";
				viz.description = "at 10:00:00";
				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
				viz.file = data.computeWithPlaceholder(AirPollutionAnalysis.class, "emissions_grid_per_day.%s", "avro");
			});

	}
}
