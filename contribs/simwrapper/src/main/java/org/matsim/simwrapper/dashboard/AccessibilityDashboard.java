package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.accessibility.AccessibilityAnalysis;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.simwrapper.*;
import org.matsim.simwrapper.viz.GridMap;

import java.util.List;

/**
 * Shows emission in the scenario.
 */
public class AccessibilityDashboard implements Dashboard {


	private final List<String> pois;
	private final String coordinateSystem;
	private final List<Modes4Accessibility> modes;

	/**
	 * Best provide the crs from {@link org.matsim.core.config.groups.GlobalConfigGroup}
	 *
	 * @param coordinateSystem
	 */
	public AccessibilityDashboard(String coordinateSystem, List<String> pois, List<Modes4Accessibility> modes) {

		this.coordinateSystem = coordinateSystem;
		this.pois = pois;
		this.modes = modes;

	}

	@Override
	public void configure(Header header, Layout layout) {


		header.title = "Accessibility";
		header.description = "Shows accessibility for different modes. Note: 10 utils are added to all values, since negative values can't get be rendered in GridMap";

		for (String poi : pois) {

			for(Modes4Accessibility mode : modes) {
				layout.row(mode.name() + "-" + poi)
					.el(GridMap.class, (viz, data) -> {
						accessibilityDataGridMap(mode.name(), mode.name() + "_accessibility", poi, viz, data);
					});

				if (modes.contains(Modes4Accessibility.walk)) {
					layout.row(mode.name() + "-" + poi)
						.el(GridMap.class, (viz, data) -> {
							accessibilityDataGridMap(mode.name() + " - Walk", mode.name() + "_accessibility_diff", poi, viz, data);
						});
				}


				layout.tab(poi).add(mode.name() + "-" + poi);
			}
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
//		viz.width = 0.5;
	}
}
