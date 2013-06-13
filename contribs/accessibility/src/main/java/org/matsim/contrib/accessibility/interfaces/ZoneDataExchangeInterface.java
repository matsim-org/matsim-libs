package org.matsim.contrib.accessibility.interfaces;

import org.matsim.core.api.experimental.facilities.ActivityFacility;

public interface ZoneDataExchangeInterface {
	
	public void getZoneAccessibilities(ActivityFacility measurePoint, double freeSpeedAccessibility,
			double carAccessibility, double bikeAccessibility, double walkAccessibility, double ptAccessibility);
	
	public boolean endReached();
	
}
