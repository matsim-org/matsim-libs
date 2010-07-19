package playground.wrashid.parkingSearch.planLevel.ranking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.parkingSearch.planLevel.ParkingGeneralLib;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.occupancy.ParkingCapacityFullLogger;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.IncomeRelevantForParking;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping;
import playground.wrashid.parkingSearch.planLevel.scoring.OrderedFacility;
import playground.wrashid.parkingSearch.planLevel.scoring.ParkingTimeInfo;

public class Ranking {

	private ParkingPriceMapping parkingPriceMapping;
	private IncomeRelevantForParking incomeRelevantForParking;
	private ActivityFacilitiesImpl parkingFacilities;

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

		if (plan.getPerson().getId().toString().equalsIgnoreCase("3")){
			System.out.println();
		}
		
		ActivityImpl arrivalParkingAct = ParkingGeneralLib.getArrivalParkingAct(plan, targetActivity);
		ActivityImpl departureParkingAct = ParkingGeneralLib.getDepartureParkingAct(plan, targetActivity);
		int indexOfCurrentParking=ParkingGeneralLib.getParkingArrivalIndex(plan, arrivalParkingAct);
		if (indexOfCurrentParking==-1){
			System.out.println();
		}
		
		double parkingArrivalTime=ParkingRoot.getParkingOccupancyMaintainer().getParkingArrivalLog().get(plan.getPerson().getId()).getParkingArrivalInfoList().get(indexOfCurrentParking).getArrivalTime();
		
		
		int numberOfParkingsInSet=100;
		
		closestParkings = ParkingRoot.getClosestParkingMatrix().getClosestParkings(targetActivity.getCoord(), numberOfParkingsInSet, numberOfParkingsInSet);

		
		// check, if there is at least one parking in parking set which is free at the time of arrival (and the given delta interval)
		// if that is not the case, enlarge the parking set.
		double delta = 15 * 60;
		while (!someParkingFromSetIsFreeAtArrivalTime(closestParkings,parkingArrivalTime,delta)){
			numberOfParkingsInSet*=2;
			closestParkings = ParkingRoot.getClosestParkingMatrix().getClosestParkings(targetActivity.getCoord(), numberOfParkingsInSet, numberOfParkingsInSet);
		}

		// score the given parkings
		
		for (int i = 0; i < closestParkings.size(); i++) {
			ActivityFacilityImpl curParking = closestParkings.get(i);
			ParkingTimeInfo parkingTimeInfo = new ParkingTimeInfo(parkingArrivalTime,
					departureParkingAct.getEndTime());
			double score = getScore(targetActivity, curParking.getId(), parkingTimeInfo, plan.getPerson().getId(),
					arrivalParkingAct.getDuration(), departureParkingAct.getDuration(), plan, delta);
			OrderedFacility orderedFacility = new OrderedFacility(curParking, score);
			prio.add(orderedFacility);
		}

		
		// because priority list is sorted from low to high score, we need to flip it
		while (prio.size() > 0) {
			ActivityFacilityImpl parkingFacility=prio.poll().getFacility();
			resultList.add(0,parkingFacility);
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
			
			if (isParkingNotFullDuringIntervall(parkings.get(i).getId(),arrivalTime, delta)){
				return true;
			}
			
		}

		return false;
	}
	
	private boolean isParkingNotFullDuringIntervall(Id parkingFacilityId, double arrivalTime, double delta){
		HashMap<Id, ParkingCapacityFullLogger> parkingCapacityFullTimes = ParkingRoot.getParkingOccupancyMaintainer()
		.getParkingCapacityFullTimes();
		
		double startTime = GeneralLib.projectTimeWithin24Hours(arrivalTime - delta);
		double endTime = GeneralLib.projectTimeWithin24Hours(arrivalTime + delta);
		ParkingCapacityFullLogger parkingCapFullLogger=parkingCapacityFullTimes.get(parkingFacilityId);
		// null means, that parking is free during the whole day (assumption: parking capacity greater than 0).
		if (parkingCapFullLogger==null || !parkingCapFullLogger.doesParkingGetFullInInterval(startTime, endTime) ) {
			return true;
		}
		return false;
	}
	
	
	

	public Ranking(ParkingPriceMapping parkingPriceMapping, IncomeRelevantForParking income,
			ActivityFacilitiesImpl parkingFacilities) {
		this.parkingPriceMapping = parkingPriceMapping;
		this.incomeRelevantForParking = income;
		this.parkingFacilities = parkingFacilities;
	}

	public double getScore(ActivityImpl targetActivity, Id parkingFacilityId, ParkingTimeInfo parkingTimeInfo, Id personId,
			double parkingArrivalDuration, double parkingDepartureDuration, Plan plan, double delta) {
		// TODO: must ensure through the score, that parkings which are full at
		// the time of arrival get a bad score.

		double parkingPriceScore = -1.0
				* ParkingRoot.getPriceScoreScalingFactor()
				* parkingPriceMapping.getParkingPrice(parkingFacilityId).getPrice(parkingTimeInfo.getStartTime(),
						parkingTimeInfo.getEndTime());
		double income = incomeRelevantForParking.getIncome(personId);

		// TODO: instead of a linear impact, this could form a different
		// function than the priceScore function (wip).
		// TODO: this value should be given back by some other function!!!!
		parkingPriceScore = parkingPriceScore / income;

		// impact of parking and un-parking durations
		double parkingActivityDurationPenalty = -1.0 * ParkingRoot.getParkingActivityDurationPenaltyScalingFactor()
				* (parkingArrivalDuration + parkingDepartureDuration);

		// TODO: the penalty could increase more than linear with the
		// duration!!!! (wip).
		// make general concept for this! => provide a function for each, which
		// can be used!
		// TODO: really important! => just change shape of main function to
		// change the impact of whole system!!!!

		// impact of walking between parking and destination

		// TODO: allow defining general function, for which the parameter can be
		// changed by individuals!
		// e.g. negative quadratic function (positiv utility in beginning, which
		// can decrease with time).

		// e.g. if we have a model, that elderly or disabled people would like
		// to walk much less, we should be able
		// to make that factor really important by changing the function
		// parameters.
		// TODO: add here the aspect of the individual function!!!!!!
		// wip: propose a function, but say that others could be used.
		// wip: what one can do now, is to estimate a model, e.g. for walking
		// distance and the other parts
		// after that one can tell the system, what the actual parking occupancy
		// in an area is and then
		// let the system calibrate itself, what scaling would render best
		// results.
		double walkingPenalty = -1.0
				* ClosestParkingMatrix.getDistance(targetActivity.getCoord(), parkingFacilities.getFacilities().get(parkingFacilityId)
						.getCoord());

		// TODO: question: should we have one scaling factor to scaling the
		// whole parking thing with the other scores?
		// is this required per Person or in general???????
		//
		// TODO: question: should we scale the different aspects of parking to
		// each other per person or in general?
		// I think the importance between the different parts of the parking
		// should come from the function.
		// therefore such a factor would not be required.

		// I think, we could avoid having all these parameters, if we would just
		// change the original function
		// parameters of the people.

		// this means, scaling in this case is not part of the simulation.
		// but this would make it difficult.

		// conclution: the scaling within the parking score should be done
		// directly over the individual function paramters.
		// the scaling between parking and the rest should be done again on an
		// individual basis. That parameter can again
		// be based on the whole situation of the person.
		// but we could for the beginning set this parameter on a central basis
		// as default, which could be changed,
		// as models and parameters have been estiamted for that part.
		// =scalingFactorParking*(individual parts of parking defined as
		// parameters for income, walking, etc. functions)

		// CAPACITY CONSTRAINTS (how full is a parking).
		// TODO: get data about constraint violations in last iteration here.
		// TODO: in the beginning, we can just set the capacityConstraint score
		// we could try: that the score of a parking used and the score given
		// here are the same in the sense,
		// that usage of full parking should give a high negative
		// capacityConstraintScore!!!!
		// we could use a general function, which gives a score as a function of
		// how congested a parking is.
		// the parkings, which are really full at the arrival time, should
		// automatically come at the bottom of the ranking
		// this means, that the capacity constraint is a quite heavy penalty
		// (bigger than all the other scores together)
		// when the parking is almost full, it gets trickier, because the
		// parking should

		// TODO: perhaps we should for each parking know, when it got full (time
		// and when there was parking available again)
		// this could help in the decision making process.
		// #######################################
		// because with this I can decide, if I want to try a parking or not
		// (estimate also travel by foot).
		// it is important, that people are encouraged to switch, if they would
		// be first at a parking!!!!
		// define: startTimeFull and endTimeFull as a list for the parkings,
		// which get full.
		// this could be just an additional statistics to the bins (which can
		// also be used for decision taking).
		// => IMPLEMENT THIS

		// only try to switch, if you are almost sure, you will get the
		// space!!!!!

		// avoid having infinite loop (where you select the same parking as
		// before even if you came late to the parking).
		// => hope, that this will be avoided automatically.

		// this function could be parameterized on individual level only if
		// needed (e.g. how averse are people belonging
		// to a certain category to full parkings)...
		double capacityConstraintScore;

		// wip: discussion, it is difficult to get the data for some areas, but
		// in some cities such data is available.
		// the private parking data is a difficult part. but that can also be
		// estimated (e.g. based on surveys).

		// wip: we can verfiy through different experiments these 4 components
		// of the parking.
		// wip: what is missing: parking search. but this can be seen as an
		// additional layer in the parking selection
		// process, because the four 4 factors we look here at, play a central
		// role to parking any way.

		// wip: future work: parking search (add an individual penaltiy for
		// driving there).
		// threre we try to create routes with maximal scores (parking utility
		// and that of driving between them should be lowest)
		// because at each parking, which is full, we know about the parkings
		// arround us (assumption for every day traffic) and
		// and the utility in terms of cost, travel distance, how much
		// congestion is at a parking (although that needs to be clowded), so
		// that
		// the agent has no perfect knowledge on the number of parkings before
		// he gets to the street.
		// etc. is clear. so, we can select where to drive next.
		//

		// wip/TODO: eigentlich müsste man files angeben können für
		// parkplatzpreise (tageszeit abhängig), parkplatz preferenzen der
		// agenten,
		// etc. => momentan einfach so machen. später beim aufräumen auslagern.

		// wip: (motivation/ what we want) es gibt viele ansätze für parkplatz
		// suche etc.
		// wir problieren ein model zu machen, dass es erlaubt modelle aus
		// umfragen zu benutzen um modell zu calbrieren und dann
		// ein system zur verfügung zu haben, wo man sowohl mit preis, parkplatz
		// infrastruktur, demand, supply spielen kann um
		// policy makeren zu helfen. e.g. wo hätte ein parkplatz mit gewisser
		// kapazität die beste chance, wirkung unter annahme
		// von kosten, etc.

		//return walkingPenalty;
		
		if (parkingFacilityId.toString().equalsIgnoreCase("36")){
			System.out.println();
		}
		
		if (!isParkingNotFullDuringIntervall(parkingFacilityId,parkingTimeInfo.getStartTime(),delta)){
			return Double.parseDouble(parkingFacilityId.toString())/2;
		}
		
		return Double.parseDouble(parkingFacilityId.toString());
	}
}
