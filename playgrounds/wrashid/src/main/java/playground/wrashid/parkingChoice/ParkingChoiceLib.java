package playground.wrashid.parkingChoice;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.wrashid.parkingChoice.infrastructure.ActInfo;

public class ParkingChoiceLib {

	static ActInfo getLastActivityInfo(Plan plan) {
		int indexOfLastCarLeg=-1;
		
		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof LegImpl) {
				LegImpl leg = (LegImpl) plan.getPlanElements().get(i);
				
				indexOfLastCarLeg=i;

			}
		}
		
		for (int i = indexOfLastCarLeg+1; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) plan.getPlanElements().get(i);
				
				return new ActInfo(activity.getFacilityId(), activity.getType());
			}
		}
		
		return null;
	}
	
}
