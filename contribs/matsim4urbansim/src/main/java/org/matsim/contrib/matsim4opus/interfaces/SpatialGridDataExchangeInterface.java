package org.matsim.contrib.matsim4opus.interfaces;

import org.matsim.contrib.matsim4opus.gis.SpatialGrid;

public interface SpatialGridDataExchangeInterface {
	public void getAndProcessSpatialGrids(SpatialGrid freeSpeedGrid, SpatialGrid carGrid, SpatialGrid bikeGrid, SpatialGrid walkGrid, SpatialGrid ptGrid);
}
