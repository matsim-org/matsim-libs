package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.accessibility.AccessibilityAnalysis;
import org.matsim.application.analysis.emissions.AirPollutionAnalysis;
import org.matsim.application.prepare.network.CreateAvroNetwork;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardUtils;
import org.matsim.simwrapper.Header;
import org.matsim.simwrapper.Layout;
import org.matsim.simwrapper.viz.ColorScheme;
import org.matsim.simwrapper.viz.GridMap;
import org.matsim.simwrapper.viz.MapPlot;
import org.matsim.simwrapper.viz.Table;

import java.util.Arrays;
import java.util.List;

/**
 * Shows emission in the scenario.
 */
public class AccessibilityDashboard implements Dashboard {


	private final List<String> pois;
	private final String coordinateSystem;

	/**
	 * Best provide the crs from {@link org.matsim.core.config.groups.GlobalConfigGroup}
	 * @param coordinateSystem
	 */
	public AccessibilityDashboard(String coordinateSystem, List<String> pois) {
		this.coordinateSystem = coordinateSystem;
		this.pois = pois;

	}

	@Override
	public void configure(Header header, Layout layout) {


		header.title = "Accessibility";
		header.description = "Shows accessibility for different modes. Note: 10 utils are added to all values, since negative values can't get be rendered in GridMap";

		for (String poi : pois) {
			layout.row("row1-" + poi)
				.el(GridMap.class, (viz, data) -> {
					viz.title = "Freespeed Accessibility to " + poi;
					viz.unit = "Utils";
					viz.description = "at 10:00:00";
					viz.height = 12.0;
					viz.cellSize = 90;
					viz.opacity = 0.1;
					viz.maxHeight = 1;
					viz.projection = this.coordinateSystem;
					viz.center = data.context().getCenter();
					viz.zoom = data.context().mapZoomLevel;
					viz.file = data.computeWithPlaceholder(AccessibilityAnalysis.class, "%s/accessibilities_simwrapper.csv", poi);
					viz.valueColumn = "freespeed_accessibility";
					viz.width = 0.5;
				})
				.el(GridMap.class, (viz, data) -> {
					viz.title = "Car Accessibility to " + poi;
					viz.unit = "Utils";
					viz.description = "at 10:00:00";
					viz.height = 12.0;
					viz.cellSize = 90;
					viz.opacity = 0.1;
					viz.maxHeight = 1;
					viz.projection = this.coordinateSystem;
					viz.center = data.context().getCenter();
					viz.zoom = data.context().mapZoomLevel;
					viz.file = data.computeWithPlaceholder(AccessibilityAnalysis.class, "%s/accessibilities_simwrapper.csv", poi);
					viz.valueColumn = "car_accessibility";
					viz.width = 0.5;
				});

			layout.row("pt_drt-" + poi)
				.el(GridMap.class, (viz, data) -> {
					viz.title = "PT Accessibility  to " + poi;
					viz.unit = "Utils";
					viz.description = "at 10:00:00";
					viz.height = 12.0;
					viz.cellSize = 90;
					viz.opacity = 0.1;
					viz.maxHeight = 1;
					viz.projection = this.coordinateSystem;
					viz.center = data.context().getCenter();
					viz.zoom = data.context().mapZoomLevel;
					viz.file = data.computeWithPlaceholder(AccessibilityAnalysis.class, "%s/accessibilities_simwrapper.csv", poi);
					viz.valueColumn = "pt_accessibility";
					viz.width = 0.5;

				}).el(GridMap.class, (viz, data) -> {
					viz.title = "DRT Accessibility to " + poi;
					viz.unit = "Utils";
					viz.description = "at 10:00:00";
					viz.height = 12.0;
					viz.cellSize = 90;
					viz.opacity = 0.1;
					viz.maxHeight = 1;
					viz.projection = this.coordinateSystem;
					viz.center = data.context().getCenter();
					viz.zoom = data.context().mapZoomLevel;
					viz.file = data.computeWithPlaceholder(AccessibilityAnalysis.class, "%s/accessibilities_simwrapper.csv", poi);
					viz.valueColumn = "estimatedDrt_accessibility";
					viz.width = 0.5;
				});

			layout.tab(poi).add("row1-" + poi).add("pt_drt-"+poi);
		}


//		layout.row("Car")
//			.el(GridMap.class, (viz, data) -> {
//				viz.title = "Car Accessibility to X";
//				viz.unit = "Utils";
//				viz.description = "at 10:00:00";
//				viz.projection = this.coordinateSystem;
////				DashboardUtils.setGridMapStandards(viz, data, this.coordinateSystem);
////				viz.file = data.computeWithPlaceholder(AccessibilityAnalysis.class, "%s/accessibilities.csv", "supermarket");
//				viz.file = data.compute(AccessibilityAnalysis.class, "supermarket_freespeed.csv");
//				viz.valueColumn = "car_accessibility";
////				viz.setColorRamp(new double[]{-5., -2.5, 0.0, 2.5, 5}, new String[]{DARK_BLUE, LIGHT_BLUE, YELLOW, SAND, ORANGE, RED});
////				viz.height = 0.0;
////				viz.maxHeight = 0;
//			});

	}
}
