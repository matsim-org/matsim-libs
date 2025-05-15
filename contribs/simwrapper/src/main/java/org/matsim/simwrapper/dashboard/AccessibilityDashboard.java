package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.accessibility.AccessibilityAnalysis;
import org.matsim.simwrapper.*;
import org.matsim.simwrapper.viz.GridMap;

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
//			layout.row("freespeed-" + poi)
//				.el(GridMap.class, (viz, data) -> {
//					accessibilityDataGridMap("Freespeed", "freespeed_accessibility", poi, viz, data);
//				}).el(GridMap.class, (viz, data) -> {
//					accessibilityDataGridMap("Freespeed - Walk", "freespeed_accessibility_diff", poi, viz, data);
//				});
			layout.row("car-" + poi)
				.el(GridMap.class, (viz, data) -> {
					accessibilityDataGridMap("Car", "car_accessibility", poi, viz, data);
				}).el(GridMap.class, (viz, data) -> {
					accessibilityDataGridMap("Car - Walk", "car_accessibility_diff", poi, viz, data);
				});

			layout.row("pt-" + poi)
				.el(GridMap.class, (viz, data) -> {
					accessibilityDataGridMap("PT", "pt_accessibility", poi, viz, data);
				}).el(GridMap.class, (viz, data) -> {
					accessibilityDataGridMap("PT - Walk", "pt_accessibility_diff", poi, viz, data);
				});
			layout.row("drt-" + poi)
			.el(GridMap.class, (viz, data) -> {
					accessibilityDataGridMap("DRT", "estimatedDrt_accessibility", poi, viz, data);
				}).el(GridMap.class, (viz, data) -> {
					accessibilityDataGridMap("DRT - Walk", "estimatedDrt_accessibility_diff", poi, viz, data);
				});;

			layout.row("walk-" + poi)
				.el(GridMap.class, (viz, data) -> {
					accessibilityDataGridMap("Walk", "walk_accessibility", poi, viz, data);
				}).el(GridMap.class, (viz, data) -> {
					accessibilityDataGridMap("Walk - Walk", "walk_accessibility_diff", poi, viz, data);
				});
//			layout.row("bike-" + poi)
//				.el(GridMap.class, (viz, data) -> {
//					accessibilityDataGridMap("Bike", "bike_accessibility", poi, viz, data);
//				}).el(GridMap.class, (viz, data) -> {
//					accessibilityDataGridMap("Bike - Walk", "bike_accessibility_diff", poi, viz, data);
//				});
//			layout.tab(poi).add("freespeed-" + poi).add("car-" + poi).add("pt-"+poi).add("drt-"+poi).add("walk-"+poi).add("bike-"+poi);
			layout.tab(poi).add("car-" + poi).add("pt-" + poi).add("drt-"+poi).add("walk-" + poi);
		}

	}

	private void accessibilityDataGridMap(String modeName, String columnName, String poi, GridMap viz, Data data) {
		viz.title = modeName + " Accessibility to " + poi;
		viz.unit = "Utils";
		viz.description = "at 10:00:00";
		viz.cellSize = 300;
		viz.opacity = 1.0;
		viz.maxHeight = 1;

		viz.projection = this.coordinateSystem;
		viz.center = data.context().getCenter();
		viz.zoom = data.context().mapZoomLevel;
		viz.file = data.computeWithPlaceholder(AccessibilityAnalysis.class, "%s/accessibilities_simwrapper.csv", poi);
		viz.valueColumn = columnName;
		viz.height = 12.;
		viz.width = 0.5;
	}
}
