package org.matsim.contrib.accessibility;

import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.facilities.ActivityFacility;

import java.util.HashMap;
import java.util.Map;

@Deprecated
public class SpatialGridAggregator implements FacilityDataExchangeInterface {

	private Map<String, SpatialGrid> accessibilityGrids = new HashMap<>() ;

	@Override
	public void setFacilityAccessibilities(ActivityFacility origin, Double timeOfDay, String mode, double accessibility) {
		accessibilityGrids.get(mode).setValue(accessibility, origin.getCoord().getX(), origin.getCoord().getY());
	}

	@Override
	public void finish() {
	}

	public Map<String, SpatialGrid> getAccessibilityGrids() {
		return accessibilityGrids;
	}
}