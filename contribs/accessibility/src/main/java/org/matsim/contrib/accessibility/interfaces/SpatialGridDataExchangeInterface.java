package org.matsim.contrib.accessibility.interfaces;

import java.util.Map;

import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.SpatialGrid;

@Deprecated // pls talk to kai if you want to use this
public interface SpatialGridDataExchangeInterface {
	@Deprecated // pls talk to kai if you want to use this
	public void setAndProcessSpatialGrids( Map<String ,SpatialGrid> spatialGrids ) ;
}
