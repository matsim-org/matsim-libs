package playground.wrashid.parkingSearch.planLevel.scoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.tools.kml.Color;
import playground.wrashid.parkingSearch.planLevel.ParkingGeneralLib;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingCapacityFullLogger;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.IncomeRelevantForParking;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping;

public abstract class ParkingScoringFunction {

	protected ParkingPriceMapping parkingPriceMapping;
	protected IncomeRelevantForParking incomeRelevantForParking;
	protected ActivityFacilitiesImpl parkingFacilities;
	double delta = 15 * 60;
	int numberOfParkingsInSet = 10;
	// int maxNumberOfParkingsInSet = 500;
	int maxWalkingDistance = 2000; // im meters

	// public int getNumberOfParkingsInSet() {
	// return numberOfParkingsInSet;
	// }

	// public void setNumberOfParkingsInSet(int numberOfParkingsInSet) {
	// this.numberOfParkingsInSet = numberOfParkingsInSet;
	// }

	public void setMaxWalkingDistance(int maxWalkingDistance) {
		this.maxWalkingDistance = maxWalkingDistance;
	}

	public void setParkingFacilities(ActivityFacilitiesImpl parkingFacilities) {
		this.parkingFacilities = parkingFacilities;
	}

	/**
	 * TODO: test this
	 * 
	 * score all the parkings in question and score them
	 * 
	 * @return
	 */
	public ArrayList<ActivityFacilityImpl> getParkingsOrderedAccordingToScore(ActivityImpl targetActivity, Plan plan) {
		PriorityQueue<OrderedFacility> prio = new PriorityQueue<OrderedFacility>();
		ArrayList<ActivityFacilityImpl> resultList = new ArrayList<ActivityFacilityImpl>();
		ArrayList<ActivityFacilityImpl> closestParkings = null;

		// if (plan.getPerson().getId().toString().equalsIgnoreCase("3")) {
		// System.out.println();
		// }

		ActivityImpl arrivalParkingAct = ParkingGeneralLib.getArrivalParkingAct(plan, targetActivity);
		int indexOfCurrentParking = ParkingGeneralLib.getParkingArrivalIndex(plan, arrivalParkingAct);
		// if (indexOfCurrentParking == -1) {
		// System.out.println();
		// }

		double parkingArrivalTime = ParkingRoot.getParkingOccupancyMaintainer().getParkingArrivalDepartureLog()
				.get(plan.getPerson().getId()).getParkingArrivalDepartureList().get(indexOfCurrentParking).getStartTime();

//		closestParkings = ParkingRoot.getClosestParkingMatrix().getClosestParkings(targetActivity.getCoord(),
//				numberOfParkingsInSet, numberOfParkingsInSet);
		
		// only look at parkings with in maxWalkingDistance
		closestParkings = ParkingRoot.getClosestParkingMatrix().getClosestParkings(targetActivity.getCoord(),
				maxWalkingDistance);
		

		// check, if there is at least one parking in parking set which is free
		// at the time of arrival (and the given delta interval)
		// if that is not the case, enlarge the parking set.

//		if (!someParkingFromSetIsFreeAtArrivalTime(closestParkings, parkingArrivalTime, delta)) {
			// numberOfParkingsInSet *= 2;
			// closestParkings =
			// ParkingRoot.getClosestParkingMatrix().getClosestParkings(targetActivity.getCoord(),
			// numberOfParkingsInSet, numberOfParkingsInSet);

			// if there are no parkings in the walking distance, we have to live
			// with it (and report that supply shortage in the analysis)
			
			
			
			
			// if still no parking is free, log the violation
			//if (!someParkingFromSetIsFreeAtArrivalTime(closestParkings, parkingArrivalTime, delta)) {
			//	ParkingRoot.getParkingLog().add("all parkings in area full: " + targetActivity.getCoord().toString());
				

			//	ParkingRoot.getMapDebugTrace().addPointCoordinate(targetActivity.getCoord(), "targetActivity", Color.RED);
				//ParkingRoot.writeMapDebugTraceToCurrentIterationDirectory();
			//}
			
			// assure, that there are at least some parkings in the selection set (especially avoid by this, that
			// the selection within the maxDistance radius gives back 0 parkings
			int minimumNumberOfParkings=3;
			if (closestParkings.size()<minimumNumberOfParkings) {			
				closestParkings = ParkingRoot.getClosestParkingMatrix().getClosestParkings(targetActivity.getCoord(),minimumNumberOfParkings,minimumNumberOfParkings);
			}
			
			// if there is no parking, in the set which is free, extend the set also
			while (!someParkingFromSetIsFreeAtArrivalTime(closestParkings, parkingArrivalTime, delta)) {
				minimumNumberOfParkings*=2;
				closestParkings = ParkingRoot.getClosestParkingMatrix().getClosestParkings(targetActivity.getCoord(),minimumNumberOfParkings,minimumNumberOfParkings);
			}
			
//		}

		// score the given parkings

		for (int i = 0; i < closestParkings.size(); i++) {
			ActivityFacilityImpl curParking = closestParkings.get(i);
			double score = getScore(targetActivity, plan, curParking.getId(), true);
			
			
			// add a very small random number to the parking score
			// this is different per agent and therefore solves the following artifacts
			// of discretization:
			// if two parkings have exactly the same score, one of them will come first in the 
			// priority queue. If for all agents the same parking comes first, we have a preference
			// of parking, which is not due to the score. By introducing the random factor, some agents
			// will get he one parking first in the queue than others.
			
			// note: this adding of a small scores has to be small compared to the score of parking
			// therefore a multiplication with that score is needed.
			
			// this is probably only need for experiments with artificial test scenarios.
			score+= MatsimRandom.getRandom().nextDouble()*score/1000;

			
			
			OrderedFacility orderedFacility = new OrderedFacility(curParking, score);
			prio.add(orderedFacility);
		}

		// because priority list is sorted from low to high score, we need to
		// flip it
		while (prio.size() > 0) {
			ActivityFacilityImpl parkingFacility = prio.poll().getFacility();
			resultList.add(0, parkingFacility);
		}

		return resultList;
	}

	/**
	 * check, that at least some of the parking is free at the arrival time.
	 * 
	 * @param parkings
	 * @param arrivalTime
	 * @return
	 */
	public boolean someParkingFromSetIsFreeAtArrivalTime(ArrayList<ActivityFacilityImpl> parkings, double arrivalTime,
			double delta) {

		for (int i = 0; i < parkings.size(); i++) {
			// only if the parking is fully free within the given interval, it
			// is considered free
			// the reason for specifying such an interval is, that the arrival
			// time at the other parking can be different
			// the that for the old parking for several reasons (e.g. the
			// replanning phase where other agents change their
			// travel behaviour or event the current agent may change his
			// departure time from home, etc.
			// therefore this delta is set big (15 minute at the moment)

			if (isParkingNotFullDuringIntervall(parkings.get(i).getId(), arrivalTime, delta)) {
				return true;
			}

		}

		return false;
	}

	public boolean isParkingFullAtTime(Id parkingFacilityId, double time) {
		HashMap<Id, ParkingCapacityFullLogger> parkingCapacityFullTimes = ParkingRoot.getParkingOccupancyMaintainer()
				.getParkingCapacityFullTimes();

		time = GeneralLib.projectTimeWithin24Hours(time);
		ParkingCapacityFullLogger parkingCapFullLogger = parkingCapacityFullTimes.get(parkingFacilityId);

		if (parkingCapFullLogger != null && parkingCapFullLogger.isParkingFullAtTime(time)) {
			return true;
		}
		return false;
	}

	public boolean isParkingNotFullDuringIntervall(Id parkingFacilityId, double arrivalTime, double delta) {
		HashMap<Id, ParkingCapacityFullLogger> parkingCapacityFullTimes = ParkingRoot.getParkingOccupancyMaintainer()
				.getParkingCapacityFullTimes();

		double startTime = GeneralLib.projectTimeWithin24Hours(arrivalTime - delta);
		double endTime = GeneralLib.projectTimeWithin24Hours(arrivalTime + delta);
		ParkingCapacityFullLogger parkingCapFullLogger = parkingCapacityFullTimes.get(parkingFacilityId);
		// null means, that parking is free during the whole day (assumption:
		// parking capacity greater than 0).
		if (parkingCapFullLogger == null || !parkingCapFullLogger.doesParkingGetFullInInterval(startTime, endTime)) {
			return true;
		}
		return false;
	}

	public ParkingScoringFunction(ParkingPriceMapping parkingPriceMapping, IncomeRelevantForParking income,
			ActivityFacilitiesImpl parkingFacilities) {
		this.parkingPriceMapping = parkingPriceMapping;
		this.incomeRelevantForParking = income;
		this.parkingFacilities = parkingFacilities;
	}

	public double getScore(ActivityImpl targetActivity, Plan plan, Id parkingFacilityId, boolean forRanking) {
		ActivityImpl arrivalParkingAct = ParkingGeneralLib.getArrivalParkingAct(plan, targetActivity);
		ActivityImpl departureParkingAct = ParkingGeneralLib.getDepartureParkingAct(plan, targetActivity);

		ParkingTimeInfo parkingTimeInfo = ParkingGeneralLib.getParkingTimeInfo(plan, targetActivity, ParkingRoot
				.getParkingOccupancyMaintainer().getParkingArrivalDepartureLog().get(plan.getPerson().getId()));

		double score = getScore(targetActivity, parkingFacilityId, parkingTimeInfo, plan.getPerson().getId(),
				arrivalParkingAct.getDuration(), departureParkingAct.getDuration(), plan, delta, forRanking);

		return score;
	}

	/**
	 * forRanking=false => only for scoring an actual parking
	 * 
	 * @param targetActivity
	 * @param parkingFacilityId
	 * @param parkingTimeInfo
	 * @param personId
	 * @param parkingArrivalDuration
	 * @param parkingDepartureDuration
	 * @param plan
	 * @param delta
	 * @param forRanking
	 * @return
	 */
	public abstract double getScore(ActivityImpl targetActivity, Id parkingFacilityId, ParkingTimeInfo parkingTimeInfo,
			Id personId, double parkingArrivalDuration, double parkingDepartureDuration, Plan plan, double delta,
			boolean forRanking);
}
