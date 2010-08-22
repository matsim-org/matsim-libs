package playground.wrashid.parkingSearch.planLevel.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.ScoringFunction;

import playground.wrashid.PSF.parking.ParkingInfo;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.GlobalRegistry;
import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.IncomeRelevantForParking;
import playground.wrashid.parkingSearch.planLevel.parkingPrice.ParkingPriceMapping;

public class ParkingDefaultScoringFunction extends ParkingScoringFunction {

	public static double oneValueForNormalizationInSeconds=20*60;
	
	public ParkingDefaultScoringFunction(ParkingPriceMapping parkingPriceMapping, IncomeRelevantForParking income,
			ActivityFacilitiesImpl parkingFacilities) {
		super(parkingPriceMapping, income, parkingFacilities);
	}
	
	public ParkingDefaultScoringFunction(ParkingPriceMapping parkingPriceMapping, IncomeRelevantForParking income) {
		this.parkingPriceMapping=parkingPriceMapping;
		this.incomeRelevantForParking=income;
	}

	public double getScore(ActivityImpl targetActivity, Id parkingFacilityId, ParkingTimeInfo parkingTimeInfo, Id personId,
			double parkingArrivalDuration, double parkingDepartureDuration, Plan plan, double delta, boolean forRanking) {
		
		

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

		// return walkingPenalty;

		if (!forRanking) {
			//TODO: remove forRanking variable...

		}

	
		// in order to get a sense for the scoring function range, it is assumed that the agent performs the first activity
		// for almost the whole day. 
		// then we divide this by the number of act-legs. Although we could make more checks (e.g. how many parkings are there, etc.)
		// we just want to get a general since for the score, so that the penalty fits into the whole scoring scheme.
		
//		ScoringFunction scoringFunction = GlobalRegistry.controler.getScoringFunctionFactory().createNewScoringFunction(plan);
//		scoringFunction.startActivity(0.0, (Activity) plan.getPlanElements().get(0));
//		scoringFunction.endActivity(23*3600);
//		scoringFunction.finish();
		double penaltyScoreNorm=5;
//		Plan lastSelectedPlan=ParkingRoot.getParkingOccupancyMaintainer().getLastSelectedPlan().get(personId);
//		if (lastSelectedPlan.getScore()==null){
//			// during first iteration, when it is not initialized
//			penaltyScoreNorm = 1000;
//		}else {
//			penaltyScoreNorm = 4* lastSelectedPlan.getScore() /plan.getPlanElements().size();
//		}
		
		
		

		double parkingPriceScore = getParkingPriceScore(parkingFacilityId, parkingTimeInfo, personId);
		double parkingParkingCapacityViolationPenalty = getParkingCapacityViolationPenalty(parkingFacilityId, parkingTimeInfo, plan);
		double parkingWalkingPenalty=getWalkingExplicitScorePart(targetActivity, parkingFacilityId);
		double parkingActivityDurationPenalty=getParkingActivityDurationPenalty(parkingArrivalDuration, parkingDepartureDuration);
		
		// TODO: add more sums here!!!!
		double weightedScore=0;
		weightedScore+=parkingPriceScore;
		weightedScore+=parkingWalkingPenalty;
		weightedScore+=parkingActivityDurationPenalty;
		weightedScore+=10*parkingParkingCapacityViolationPenalty;
		weightedScore/=13.0; // for normailization
	
		
		// this is extremly important for reduing the number of parking slot violations, because:
		// for the same target if there is no individualization of the parameters for the agents, all see the 
		// same score map for the parkings. Therefore there is lots of oszilation.
		// - even if this parameter is chosen extremly small, still there are only few violations.
		// - an agent, which would have a better score at a place, because he could reach that place first, will
		// still get a chance to do that later.
		// - pakrings, which have similar scores get selected at random!!! (e.g. within range of 10%)
		
		int iterationNumber=GlobalRegistry.controler.getIterationNumber();
		int startingPhaseIterations=5;
		if (iterationNumber<startingPhaseIterations){
			//weightedScore=weightedScore*MatsimRandom.getRandom().nextDouble()*(1-(iterationNumber)/(startingPhaseIterations+1));
		}
		
		return weightedScore*penaltyScoreNorm;
	}

	
	private double getWalkingExplicitScorePart(ActivityImpl targetActivity, Id parkingFacilityId){
		double zeroValueForNormalization=0;
		double oneValueForNormalization=maxWalkingDistance; // in meters
		
		
		double walkingPenalty = -1.0
		* GeneralLib.getDistance(targetActivity.getCoord(), parkingFacilities.getFacilities().get(parkingFacilityId)
				.getCoord());
		return (walkingPenalty-zeroValueForNormalization)/(oneValueForNormalization);
		
	}
	
	
	
	/**
	 * For each agent, it is assessed if he violates the parking capacity based on his time of arrival.
	 * 
	 * @param parkingFacilityId
	 * @param parkingTimeInfo
	 * @param plan 
	 * @return
	 */
	private double getParkingCapacityViolationPenalty(Id parkingFacilityId, ParkingTimeInfo parkingTimeInfo, Plan plan) {
		double parkingCapacityViolationPenalty=0;
		
		
		
		if (ParkingRoot.getParkingScoringFunction()
				.isParkingFullAtTime(parkingFacilityId, parkingTimeInfo.getStartTime() - delta/10.0)) {
			// if parking full before arrival => give full penalty
			parkingCapacityViolationPenalty = -1.0;
		} else if (!ParkingRoot.getParkingScoringFunction()
				.isParkingFullAtTime(parkingFacilityId, parkingTimeInfo.getStartTime() - delta/10.0) && ParkingRoot.getParkingScoringFunction().isParkingFullAtTime(parkingFacilityId,
				parkingTimeInfo.getStartTime() + delta)) {
			// if parking not full just before arrival but full after arrival, then still give a small penalty, because we caused the parking
			// to get full and this might cause some problem in the future
			parkingCapacityViolationPenalty = -0.1;
		} else {
			// if parking not full before and after our arrival => no problem.
			parkingCapacityViolationPenalty = 0;
		}

		return parkingCapacityViolationPenalty;
	}

	private boolean isParkingFull(Id parkingFacilityId, ParkingTimeInfo parkingTimeInfo) {
		return !ParkingRoot.getParkingScoringFunction().isParkingNotFullDuringIntervall(parkingFacilityId,
				parkingTimeInfo.getStartTime(), delta);
	}

	private double getParkingPriceScore(Id parkingFacilityId, ParkingTimeInfo parkingTimeInfo, Id personId) {
		double lowestIncome=3000;
		double zeroValueForNormalization=0;
		double oneValueForNormalization=parkingPriceMapping.getParkingPrice(parkingFacilityId).getPrice(0,
				12*3600)/lowestIncome; // in seconds
		
		double parkingPriceScore = -1.0
				* ParkingRoot.getPriceScoreScalingFactor()
				* parkingPriceMapping.getParkingPrice(parkingFacilityId).getPrice(parkingTimeInfo.getStartTime(),
						parkingTimeInfo.getEndTime());
		double income = incomeRelevantForParking.getIncome(personId);

		// TODO: instead of a linear impact, this could form a different
		// function than the priceScore function (wip).
		// TODO: this value should be given back by some other function!!!!
		parkingPriceScore = parkingPriceScore / income;

		return (parkingPriceScore-zeroValueForNormalization)/(oneValueForNormalization);
	}

	private double getParkingActivityDurationPenalty(double parkingArrivalDuration, double parkingDepartureDuration) {
		double zeroValueForNormalization=0;

		
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

		return (parkingActivityDurationPenalty-zeroValueForNormalization)/(oneValueForNormalizationInSeconds);
	}

}
