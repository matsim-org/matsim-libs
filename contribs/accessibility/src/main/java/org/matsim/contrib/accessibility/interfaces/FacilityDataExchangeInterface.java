package org.matsim.contrib.accessibility.interfaces;

import java.util.Map;
import org.matsim.facilities.ActivityFacility;

public interface FacilityDataExchangeInterface {
	
	void setFacilityAccessibilities(ActivityFacility measurePoint, Double timeOfDay, Map<String, Double> accessibilities);

	void finish();
}