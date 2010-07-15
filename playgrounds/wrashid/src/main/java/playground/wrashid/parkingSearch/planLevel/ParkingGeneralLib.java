package playground.wrashid.parkingSearch.planLevel;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
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
	
	/**
	 * The home/first parking comes last (because it is the last parking arrival of the day).
	 * 
	 * @param plan
	 * @return
	 */
	public static LinkedList<Id> getAllParkingFacilityIds(Plan plan) {
		LinkedList<Id> parkingFacilityIds=new LinkedList<Id>();
		
		// recognize parking arrival patterns (this means, there is car leg after which there is 
		// a parking activity).
		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) plan.getPlanElements().get(i);

				if (activity.getType().equalsIgnoreCase("parking")) {
					Leg leg=(Leg) plan.getPlanElements().get(i-1);
					
					if (leg.getMode().equalsIgnoreCase("car")){
						parkingFacilityIds.add(activity.getFacilityId());
					}
				}
			}
		}

		return parkingFacilityIds;
	}
	
	/**
	 * get the first activity after each arrival at a parking.
	 *
	 * 
	 * @param plan
	 * @return
	 */
	public static LinkedList<ActivityImpl> getParkingTargetActivities(Plan plan){
		LinkedList<ActivityImpl> list=new LinkedList<ActivityImpl>();
		
		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) plan.getPlanElements().get(i);

				if (activity.getType().equalsIgnoreCase("parking")) {
					Leg leg=(Leg) plan.getPlanElements().get(i-1);
					
					if (leg.getMode().equalsIgnoreCase("car")){
						// parking arrival pattern recognized.
						
						ActivityImpl targetActivity = (ActivityImpl) plan.getPlanElements().get(i+2);
						list.add(targetActivity);
					}
				}
			}
		}
		
		return list;
	}
	
}
