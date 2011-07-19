package playground.wrashid.parkingChoice;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.wrashid.parkingChoice.infrastructure.ActInfo;

public class ParkingChoiceLib {

	public static boolean isTestCaseRun=false;
	
	// TODO: use parameter from org.matsim.core.config.groups.PlansCalcRouteConfigGroup for this
	static public double getWalkingSpeed(){
		return 3.0 / 3.6 * 1.3;
	}
	
	
	
	static ActInfo getLastActivityInfo(Plan plan) {
		int indexOfLastLeg=-1;
		
		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof LegImpl) {
				LegImpl leg = (LegImpl) plan.getPlanElements().get(i);
				
				indexOfLastLeg=i;
			}
		}
		
		ActInfo firstActivityAfterIndex = getFirstActivityAfterIndex(plan, indexOfLastLeg);
		
		return firstActivityAfterIndex;
	}

	private static ActInfo getFirstActivityAfterIndex(Plan plan, int indexOfLastLeg) {
		for (int i = indexOfLastLeg+1; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) plan.getPlanElements().get(i);
				
				return new ActInfo(activity.getFacilityId(), activity.getType());
			}
		}
		return null;
	}
	
	public static ActInfo getLastActivityInfoPreceededByCarLeg(Plan plan){
		int indexOfLastCarLeg =	getIndexOfLastCarLeg(plan);
		
		if (indexOfLastCarLeg>0){
			return getFirstActivityAfterIndex(plan, indexOfLastCarLeg);
		}
		
		return null;
	}

	private static int getIndexOfLastCarLeg(Plan plan) {
		int indexOfLastCarLeg=-1;
		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof LegImpl) {
				LegImpl leg = (LegImpl) plan.getPlanElements().get(i);
				
				if (TransportMode.car.equalsIgnoreCase(leg.getMode())){
					indexOfLastCarLeg=i;
				}
			}
		}
		
		return indexOfLastCarLeg;
	}
	
	static boolean containsCarLeg(Plan plan){
		return getIndexOfLastCarLeg(plan)>0;
	}
	
}
