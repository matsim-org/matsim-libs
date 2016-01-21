package org.matsim.contrib.accessibility;

import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interfaces.FacilityDataExchangeInterface;
import org.matsim.facilities.ActivityFacility;

import java.util.HashMap;
import java.util.Map;

class SpatialGridAggregator implements FacilityDataExchangeInterface {

	private Map<Modes4Accessibility,SpatialGrid> accessibilityGrids = new HashMap<>() ;

	@Override
	public void setFacilityAccessibilities(ActivityFacility origin, Double timeOfDay, Map<Modes4Accessibility, Double> accessibilities) {
		for (Map.Entry<Modes4Accessibility, Double> modes4AccessibilityDoubleEntry : accessibilities.entrySet()) {
			accessibilityGrids.get(modes4AccessibilityDoubleEntry.getKey()).setValue(modes4AccessibilityDoubleEntry.getValue(), origin.getCoord().getX(), origin.getCoord().getY());
		}
	}

	@Override
	public void finish() {

	}

	Map<Modes4Accessibility, SpatialGrid> getAccessibilityGrids() {
		return accessibilityGrids;
	}

}
