package org.matsim.contrib.accessibility.interfaces;

import java.util.Map;

import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.facilities.ActivityFacility;

public interface ZoneDataExchangeInterface {
	
	void setZoneAccessibilities(ActivityFacility measurePoint, Node fromNode, Map<Modes4Accessibility, Double> accessibilities) ;
	
}
