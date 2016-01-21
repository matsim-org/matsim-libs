package org.matsim.contrib.accessibility.interfaces;

import java.util.Map;

import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.facilities.ActivityFacility;

public interface FacilityDataExchangeInterface {
	
	void setFacilityAccessibilities(ActivityFacility measurePoint, Double timeOfDay, Map<Modes4Accessibility, Double> accessibilities);

	void finish();
	
}
