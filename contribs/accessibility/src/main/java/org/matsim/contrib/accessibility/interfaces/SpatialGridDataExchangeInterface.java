package org.matsim.contrib.accessibility.interfaces;

import java.util.Map;

import org.matsim.contrib.accessibility.AccessibilityControlerListenerImpl.Modes4Accessibility;
import org.matsim.contrib.accessibility.gis.SpatialGrid;

public interface SpatialGridDataExchangeInterface {
	public void getAndProcessSpatialGrids( Map<Modes4Accessibility,SpatialGrid> spatialGrids ) ;
}
