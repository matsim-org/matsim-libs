package playground.wrashid.lib.tools.facility;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.IntegerValueHashMap;

public class PrintStatisticsAboutFacilities {

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String facilitiesPath = "K:/Projekte/herbie/output/demandCreation/facilitiesWFreight.xml.gz";
		ActivityFacilitiesImpl facilities = GeneralLib.readActivityFacilities(facilitiesPath);

		IntegerValueHashMap<String> actTypes=new IntegerValueHashMap<String>();

		for (Id facilityId : facilities.getFacilities().keySet()) {
			ActivityFacilityImpl facility = (ActivityFacilityImpl) facilities.getFacilities().get(facilityId);

			for (String activityOption : facility.getActivityOptions().keySet()) {
				actTypes.increment(activityOption);
			}

		}
		
		for (String activityType:actTypes.getKeySet()){
			System.out.println(activityType + " => " + actTypes.get(activityType));
		}

	}

}
