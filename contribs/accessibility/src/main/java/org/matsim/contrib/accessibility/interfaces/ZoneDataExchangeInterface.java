package org.matsim.contrib.accessibility.interfaces;

import java.util.Map;

import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.facilities.ActivityFacility;

public interface ZoneDataExchangeInterface {
	
	public void setZoneAccessibilities(ActivityFacility measurePoint, Map<Modes4Accessibility,Double> accessibilities ) ;
	
}
