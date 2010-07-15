package playground.wrashid.parkingSearch.planLevel;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;

public class ParkingGeneralLib {

	/**
	 * reurns null, if no parking activity found else id of first parking
	 * facility
	 * 
	 * @param plan
	 * @return
	 */
	public static Id getFirstParkingFacilityId(Plan plan) {

		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) plan.getPlanElements().get(i);

				if (activity.getType().equalsIgnoreCase("parking")) {
					return activity.getFacilityId();
				}

			}
		}

		return null;
	}
}
