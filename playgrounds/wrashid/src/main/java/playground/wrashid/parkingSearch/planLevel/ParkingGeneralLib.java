package playground.wrashid.parkingSearch.planLevel;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingTimeInfo;

public class ParkingGeneralLib {

	
	/**
	 * get parking related walking distance of whole day - average per leg
	 * @param plan
	 * @param facilities
	 * @return
	 */
	public static double getParkingRelatedWalkingDistanceOfWholeDayAveragePerLeg(Plan plan, ActivityFacilities facilities) {
		double travelDistance=0;
		int numberOfLegs=0;

		List<PlanElement> pe = plan.getPlanElements();

		for (int i = 0; i < pe.size(); i++) {
			if (pe.get(i) instanceof ActivityImpl) {
				ActivityImpl parkingActivity = (ActivityImpl) pe.get(i);
				if (parkingActivity.getType().equalsIgnoreCase("parking")) {
					Coord coordParking=facilities.getFacilities().get(parkingActivity.getFacilityId()).getCoord();
					Leg nextLeg = (Leg) pe.get(i + 1);
					Leg prevLeg = (Leg) pe.get(i - 1);
					if (nextLeg.getMode().equalsIgnoreCase("walk")) {
						ActivityImpl nextAct = (ActivityImpl) pe.get(i+2);
						
						if (nextAct.getFacilityId()!=null){
							Coord nextActFacilityCoord=facilities.getFacilities().get(nextAct.getFacilityId()).getCoord();
							travelDistance+=GeneralLib.getDistance(coordParking, nextActFacilityCoord);
						} else {
							Coord nextActLinkCoord=nextAct.getCoord();
							travelDistance+=GeneralLib.getDistance(coordParking, nextActLinkCoord);
						}
						numberOfLegs++;
					}
					if (prevLeg.getMode().equalsIgnoreCase("walk")) {
						ActivityImpl prevAct = (ActivityImpl) pe.get(i-2);
						
						if (prevAct.getFacilityId()!=null){
							Coord prevActFacilityCoord=facilities.getFacilities().get(prevAct.getFacilityId()).getCoord();
							travelDistance+=GeneralLib.getDistance(coordParking, prevActFacilityCoord);
						} else {
							Coord prevActLinkCoord=prevAct.getCoord();
							travelDistance+=GeneralLib.getDistance(coordParking, prevActLinkCoord);
						}
						numberOfLegs++;
					}

				}
			}
		}

		return travelDistance/numberOfLegs;
	}

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
	 * The home/first parking comes last (because it is the last parking arrival
	 * of the day).
	 * 
	 * @param plan
	 * @return
	 */
	public static LinkedList<Id> getAllParkingFacilityIds(Plan plan) {
		LinkedList<Id> parkingFacilityIds = new LinkedList<Id>();

		// recognize parking arrival patterns (this means, there is car leg
		// after which there is
		// a parking activity).
		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) plan.getPlanElements().get(i);

				if (activity.getType().equalsIgnoreCase("parking")) {
					Leg leg = (Leg) plan.getPlanElements().get(i - 1);

					if (leg.getMode().equalsIgnoreCase("car")) {
						parkingFacilityIds.add(activity.getFacilityId());
					}
				}
			}
		}

		return parkingFacilityIds;
	}

	public static void printAllParkingFacilityIds(Plan plan) {
		LinkedList<Id> allParkingFacilityIds = getAllParkingFacilityIds(plan);

		System.out.println(plan.getPerson().getId());

		for (int i = 0; i < allParkingFacilityIds.size(); i++) {
			System.out.print(allParkingFacilityIds.get(i) + " - ");
		}

		System.out.println();
	}

	/**
	 * get the first activity after each arrival at a parking.
	 * 
	 * 
	 * @param plan
	 * @return
	 */
	public static LinkedList<ActivityImpl> getParkingTargetActivities(Plan plan) {
		LinkedList<ActivityImpl> list = new LinkedList<ActivityImpl>();

		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) plan.getPlanElements().get(i);

				if (activity.getType().equalsIgnoreCase("parking")) {
					Leg leg = (Leg) plan.getPlanElements().get(i - 1);

					if (leg.getMode().equalsIgnoreCase("car")) {
						// parking arrival pattern recognized.

						ActivityImpl targetActivity = (ActivityImpl) plan.getPlanElements().get(i + 2);
						list.add(targetActivity);
					}
				}
			}
		}

		return list;
	}

	/**
	 * Get the ParkingTimeInfo of the parking related to the given activity
	 * 
	 * @param activity
	 * @return
	 */
	public static ParkingTimeInfo getParkingTimeInfo(Plan plan, ActivityImpl activity) {
		ActivityImpl arrivalParkingAct = getArrivalParkingAct(plan, activity);
		ActivityImpl departureParkingAct = getDepartureParkingAct(plan, activity);

		return new ParkingTimeInfo(arrivalParkingAct.getStartTime(), departureParkingAct.getEndTime());
	}

	public static ActivityImpl getDepartureParkingAct(Plan plan, ActivityImpl activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int indexOfDepartingParkingAct = getDepartureParkingActIndex(plan, activity);

		ActivityImpl departureParkingAct = (ActivityImpl) pe.get(indexOfDepartingParkingAct);
		return departureParkingAct;
	}

	public static int getDepartureParkingActIndex(Plan plan, ActivityImpl activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int activityIndex = pe.indexOf(activity);
		int indexOfDepartingParkingAct = -1;

		for (int i = activityIndex; i < pe.size(); i++) {
			if (pe.get(i) instanceof ActivityImpl) {
				ActivityImpl parkingAct = (ActivityImpl) plan.getPlanElements().get(i);
				if (parkingAct.getType().equalsIgnoreCase("parking")) {
					indexOfDepartingParkingAct = i;
					break;
				}
			}
		}

		// if home parking
		if (indexOfDepartingParkingAct == -1) {
			for (int i = 0; i < pe.size(); i++) {
				if (pe.get(i) instanceof ActivityImpl) {
					ActivityImpl parkingAct = (ActivityImpl) plan.getPlanElements().get(i);
					if (parkingAct.getType().equalsIgnoreCase("parking")) {
						indexOfDepartingParkingAct = i;
						break;
					}
				}
			}
		}

		if (indexOfDepartingParkingAct == -1) {
			throw new Error("plan wrong: no parking in the whole plan");
		}

		return indexOfDepartingParkingAct;
	}

	/**
	 * Get the arrival parking responding to this activity.
	 * 
	 * @return
	 */
	public static ActivityImpl getArrivalParkingAct(Plan plan, ActivityImpl activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int indexOfArrivalParkingAct = getArrivalParkingActIndex(plan, activity);

		ActivityImpl arrivalParkingAct = (ActivityImpl) pe.get(indexOfArrivalParkingAct);
		return arrivalParkingAct;
	}

	public static int getArrivalParkingActIndex(Plan plan, ActivityImpl activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int activityIndex = pe.indexOf(activity);
		int indexOfArrivalParkingAct = -1;

		for (int i = activityIndex; 0 < i; i--) {
			if (pe.get(i) instanceof ActivityImpl) {
				ActivityImpl parkingAct = (ActivityImpl) plan.getPlanElements().get(i);
				if (parkingAct.getType().equalsIgnoreCase("parking")) {
					indexOfArrivalParkingAct = i;
					break;
				}
			}
		}

		if (indexOfArrivalParkingAct == -1) {
			throw new Error("no parking arrival activity found - something is wrong with the plan");
		}

		return indexOfArrivalParkingAct;
	}

	/**
	 * TODO: add test. If the specified arrival is the i-th arrival of the day,
	 * return i (with i starting at 0).
	 * 
	 * @param plan
	 * @param parkingArrivalAct
	 * @return
	 */
	public static int getParkingArrivalIndex(Plan plan, ActivityImpl parkingArrivalAct) {
		List<PlanElement> pe = plan.getPlanElements();
		int parkingPlanElementIndex = pe.indexOf(parkingArrivalAct);
		int index = -1;

		for (int i = 0; i <= parkingPlanElementIndex; i++) {
			if (pe.get(i) instanceof ActivityImpl) {
				ActivityImpl activity = (ActivityImpl) plan.getPlanElements().get(i);

				if (activity.getType().equalsIgnoreCase("parking")) {
					Leg leg = (Leg) plan.getPlanElements().get(i - 1);

					if (leg.getMode().equalsIgnoreCase("car")) {
						index++;
					}
				}
			}
		}

		return index;
	}

}
