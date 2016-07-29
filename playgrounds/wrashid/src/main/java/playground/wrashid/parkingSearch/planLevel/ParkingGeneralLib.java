package playground.wrashid.parkingSearch.planLevel;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingArrivalDepartureLog;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingAttribute;
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
			if (pe.get(i) instanceof Activity) {
				Activity parkingActivity = (Activity) pe.get(i);
				if (parkingActivity.getType().equalsIgnoreCase("parking")) {
					Coord coordParking=facilities.getFacilities().get(parkingActivity.getFacilityId()).getCoord();
					Leg nextLeg = (Leg) pe.get(i + 1);
					Leg prevLeg = (Leg) pe.get(i - 1);
					if (nextLeg.getMode().equalsIgnoreCase("walk")) {
						Activity nextAct = (Activity) pe.get(i+2);
						
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
						Activity prevAct = (Activity) pe.get(i-2);
						
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
	public static Id<ActivityFacility> getFirstParkingFacilityId(Plan plan) {

		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof Activity) {
				Activity activity = (Activity) plan.getPlanElements().get(i);

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
	public static LinkedList<Id<ActivityFacility>> getAllParkingFacilityIds(Plan plan) {
		LinkedList<Id<ActivityFacility>> parkingFacilityIds = new LinkedList<>();

		// recognize parking arrival patterns (this means, there is car leg
		// after which there is
		// a parking activity).
		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof Activity) {
				Activity activity = (Activity) plan.getPlanElements().get(i);

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
		LinkedList<Id<ActivityFacility>> allParkingFacilityIds = getAllParkingFacilityIds(plan);

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
	public static LinkedList<Activity> getParkingTargetActivities(Plan plan) {
		LinkedList<Activity> list = new LinkedList<Activity>();

		for (int i = 0; i < plan.getPlanElements().size(); i++) {
			if (plan.getPlanElements().get(i) instanceof Activity) {
				Activity activity = (Activity) plan.getPlanElements().get(i);

				if (activity.getType().equalsIgnoreCase("parking")) {
					Leg leg = (Leg) plan.getPlanElements().get(i - 1);

					if (leg.getMode().equalsIgnoreCase("car")) {
						// parking arrival pattern recognized.

						Activity targetActivity = (Activity) plan.getPlanElements().get(i + 2);
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
	public static ParkingTimeInfo getParkingTimeInfo(Plan plan, Activity activity, ParkingArrivalDepartureLog parkingArrivalDepartureLog) {
		Activity arrivalParkingAct = getArrivalParkingAct(plan, activity);
		Activity departureParkingAct = getDepartureParkingAct(plan, activity);

		int parkingArrivalIndex=getParkingArrivalIndex(plan,arrivalParkingAct);
		
		ParkingTimeInfo parkingTimeInfo=parkingArrivalDepartureLog.getParkingArrivalDepartureList().get(parkingArrivalIndex);
		
		if (arrivalParkingAct.getFacilityId()!=parkingTimeInfo.getParkingFacilityId()){
			throw new Error("facility Ids inconsistent:" + arrivalParkingAct.getFacilityId()+"!="+parkingTimeInfo.getParkingFacilityId());
		}
		
		return parkingTimeInfo;
	}

	public static Activity getDepartureParkingAct(Plan plan, Activity activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int indexOfDepartingParkingAct = getDepartureParkingActIndex(plan, activity);

		Activity departureParkingAct = (Activity) pe.get(indexOfDepartingParkingAct);
		return departureParkingAct;
	}

	public static int getDepartureParkingActIndex(Plan plan, Activity activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int activityIndex = pe.indexOf(activity);
		int indexOfDepartingParkingAct = -1;

		for (int i = activityIndex; i < pe.size(); i++) {
			if (pe.get(i) instanceof Activity) {
				Activity parkingAct = (Activity) plan.getPlanElements().get(i);
				if (parkingAct.getType().equalsIgnoreCase("parking")) {
					indexOfDepartingParkingAct = i;
					break;
				}
			}
		}

		// if home parking
		if (indexOfDepartingParkingAct == -1) {
			for (int i = 0; i < pe.size(); i++) {
				if (pe.get(i) instanceof Activity) {
					Activity parkingAct = (Activity) plan.getPlanElements().get(i);
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
	public static Activity getArrivalParkingAct(Plan plan, Activity activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int indexOfArrivalParkingAct = getArrivalParkingActIndex(plan, activity);

		Activity arrivalParkingAct = (Activity) pe.get(indexOfArrivalParkingAct);
		return arrivalParkingAct;
	}

	public static int getArrivalParkingActIndex(Plan plan, Activity activity) {
		List<PlanElement> pe = plan.getPlanElements();
		int activityIndex = pe.indexOf(activity);
		int indexOfArrivalParkingAct = -1;

		for (int i = activityIndex; 0 < i; i--) {
			if (pe.get(i) instanceof Activity) {
				Activity parkingAct = (Activity) plan.getPlanElements().get(i);
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
	public static int getParkingArrivalIndex(Plan plan, Activity parkingArrivalAct) {
		List<PlanElement> pe = plan.getPlanElements();
		int parkingPlanElementIndex = pe.indexOf(parkingArrivalAct);
		int index = -1;

		for (int i = 0; i <= parkingPlanElementIndex; i++) {
			if (pe.get(i) instanceof Activity) {
				Activity activity = (Activity) plan.getPlanElements().get(i);

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

	public static boolean containsParkingAttribute(LinkedList<ParkingAttribute> parkingFacilityAttributeList, ParkingAttribute parkingAttribute){
		for (ParkingAttribute parkingAttrbiuteInList:parkingFacilityAttributeList){
			if (parkingAttrbiuteInList==parkingAttribute){
				return true;
			}
		}
		return false;
	}
	
}
