package org.matsim.contrib.accessibility.interfaces;

import org.matsim.contrib.accessibility.gis.SpatialGrid;

public interface SpatialGridDataExchangeInterface {
	public void getAndProcessSpatialGrids(SpatialGrid freeSpeedGrid, SpatialGrid carGrid, SpatialGrid bikeGrid, SpatialGrid walkGrid, SpatialGrid ptGrid);
}
