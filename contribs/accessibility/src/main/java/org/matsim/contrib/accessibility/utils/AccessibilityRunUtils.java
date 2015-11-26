package org.matsim.contrib.accessibility.utils;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesUtils;

public class AccessibilityRunUtils {
	
	public static ActivityFacilities collectActivityFacilitiesOfType(Scenario scenario, String activityFacilityType) {
		ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities(activityFacilityType) ;
		for (ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()) {
			for (ActivityOption option : fac.getActivityOptions().values()) {
				if ( option.getType().equals(activityFacilityType) ) {
					activityFacilities.addActivityFacility(fac);
				}
			}
		}
		return activityFacilities;
	}

	
	public static List<String> collectAllFacilityTypes(Scenario scenario) {
		List<String> activityTypes = new ArrayList<String>() ;
		for (ActivityFacility fac : scenario.getActivityFacilities().getFacilities().values()) {
			for (ActivityOption option : fac.getActivityOptions().values()) {
				// collect all activity types that are contained within the provided facilities file
				if ( !activityTypes.contains(option.getType()) ) {
					activityTypes.add( option.getType() ) ;
				}
			}
		}
		return activityTypes;
	}
}
