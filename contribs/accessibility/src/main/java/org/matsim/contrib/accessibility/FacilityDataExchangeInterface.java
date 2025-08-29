package org.matsim.contrib.accessibility;

import java.util.Map;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.ActivityFacility;

public interface FacilityDataExchangeInterface extends DataExchangeInterface {

	void setFacilityAccessibilities(ActivityFacility measurePoint, Double timeOfDay, String mode, double accessibility);

}
