package org.matsim.simwrapper;

import org.matsim.simwrapper.viz.GridMap;

public class DashboardUtils {
	static final String DARK_BLUE = "#1175b3";
	static final String LIGHT_BLUE = "#95c7df";
	static final String ORANGE = "#f4a986";
	static final String RED = "#cc0c27";
	static final String SAND = "#dfb095";
	static final String YELLOW = "#dfdb95";

	public static void setGridMapStandards(GridMap viz, Data data, String crs) {
		viz.height = 12.0;
		viz.cellSize = 100;
		viz.opacity = 0.1;
		viz.maxHeight = 15;
		viz.projection = crs;
		viz.center = data.context().getCenter();
		viz.zoom = data.context().mapZoomLevel;
		viz.setColorRamp(new double[]{30, 40, 50, 60, 70}, new String[]{DARK_BLUE, LIGHT_BLUE, YELLOW, SAND, ORANGE, RED});
	}
}
