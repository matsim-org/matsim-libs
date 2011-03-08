package playground.wrashid.lib.tools.facility;

import java.util.HashSet;

import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;

import playground.wrashid.lib.GeneralLib;

public class PrintStatisticsAboutFacilities {

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String facilitiesPath = "H:/data/experiments/ARTEMIS/output/run10/output_facilities.xml.gz";
		ActivityFacilitiesImpl facilities = GeneralLib.readActivityFacilities(facilitiesPath);

		HashSet<String> activityTypes = new HashSet<String>();

		for (Id facilityId : facilities.getFacilities().keySet()) {
			ActivityFacilityImpl facility = (ActivityFacilityImpl) facilities.getFacilities().get(facilityId);

			for (String activityOption : facility.getActivityOptions().keySet()) {
				if (!activityTypes.contains(activityOption)) {
					System.out.println(activityOption);
					activityTypes.add(activityOption);
				}

			}

		}

	}

}
