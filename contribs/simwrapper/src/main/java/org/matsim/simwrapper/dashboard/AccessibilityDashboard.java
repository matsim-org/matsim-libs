package org.matsim.simwrapper.dashboard;

import org.matsim.application.analysis.accessibility.AccessibilityAnalysis;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.simwrapper.*;
import org.matsim.simwrapper.viz.ColorScheme;
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
		header.description = "Shows accessibility for different modes of transport to different points of interest.";

		for (String poi : pois) {

			for(Modes4Accessibility mode : modes) {
				layout.row(mode.name() + "-" + poi)
					.el(GridMap.class, (viz, data) -> {
						accessibilityDataGridMap(mode.name(), mode.name() + "_accessibility", poi, viz, data, false);
					});

				if (modes.contains(Modes4Accessibility.walk)) {
					layout.row(mode.name() + "-" + poi)
						.el(GridMap.class, (viz, data) -> {
							accessibilityDataGridMap(mode.name(), mode.name() + "_accessibility_diff", poi, viz, data, true);

						});
				}


				layout.tab(poi).add(mode.name() + "-" + poi);
			}
		}

	}

	private void accessibilityDataGridMap(String modeName, String columnName, String poi, GridMap viz, Data data, boolean isDiff) {
		viz.title = modeName + (isDiff ? " - walk" :"") + " accessibility to " + poi;
		viz.unit = "Utils";
		viz.description = isDiff ? "white: walk and " + modeName + " are equivalent; green: " + modeName + " is advantageous" : "yellow: high accessibility; purple: low accessibility";
		viz.setColorRamp(isDiff ? "Greens" : ColorScheme.Viridis);
		viz.cellSize = 250;
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
