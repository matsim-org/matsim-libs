/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.wrashid.parkingSearch.withinDay_v_STRC.scoring;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.Pair;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scoring.ScoringFunction;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.parkingSearch.withinDay_v_STRC.WithinDayParkingController;
import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;
import playground.wrashid.parkingSearch.withindayFW.analysis.ParkingAnalysisHandler;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyStats;
import playground.wrashid.parkingSearch.withindayFW.util.GlobalParkingSearchParams;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

public class ParkingScoreManager extends LayerForAddingDataCollectionEventHandlers implements ScoringListener {

	private final ParkingPersonalBetas parkingPersonalBetas;

	private DoubleValueHashMap<Id> parkingIterationScoreSum;
	private LinkedListValueHashMap<Id, Pair<Id, Double>> parkingWalkTimesLog;
	private LinkedListValueHashMap<Id, Pair<Id, Double>> parkingSearchTimesLog;
	private LinkedListValueHashMap<Id, Pair<Id, Double>> parkingCostLog;
	private ParkingOccupancyStats parkingOccupancy;

	private ParkingAnalysisHandler parkingAnalysisHandler;

	public ParkingScoreManager(Scenario scenario, ParkingInfrastructure parkingInfrastructure, double distance,
			WithinDayParkingController controler, ParkingPersonalBetas parkingPersonalBetas) {
		super(scenario, parkingInfrastructure, distance, controler);
		this.parkingPersonalBetas = parkingPersonalBetas;
	}
	
	@Override
	public void reset(int iter) {
		super.reset(iter);
		
		parkingIterationScoreSum=new DoubleValueHashMap<Id>();
		parkingWalkTimesLog=new LinkedListValueHashMap<Id, Pair<Id,Double>>();
		parkingSearchTimesLog=new LinkedListValueHashMap<Id, Pair<Id,Double>>();
		parkingCostLog=new LinkedListValueHashMap<Id, Pair<Id,Double>>();
		parkingOccupancy=new ParkingOccupancyStats();
	}
		
	@Override
	public void handleEvent(ActivityEndEvent event) {
		super.handleEvent(event);
		Id personId = event.getPersonId();

		if (event.getActType().equalsIgnoreCase("parking")) {
			PlanBasedWithinDayAgent planBasedWithinDayAgent = this.agents.get(personId);

			if (isAgentNextDrivingAwayFromParking(planBasedWithinDayAgent)) {
				if (isDuringDayParkingActivity(planBasedWithinDayAgent)) {
					updateParkingScoreDuringDay(event);
				}
			}

		}
	}

	private boolean isDuringDayParkingActivity(PlanBasedWithinDayAgent planBasedWithinDayAgent) {
		return firstParkingActivityPlanElemIndex.get(planBasedWithinDayAgent.getId()) < currentPlanElementIndex
				&& lastParkingActivityPlanElemIndex.get(planBasedWithinDayAgent.getId()) > currentPlanElementIndex;
	}

	private boolean isNextLegFirstCarDepartureOfDay(PlanBasedWithinDayAgent planBasedWithinDayAgent) {
		return firstParkingActivityPlanElemIndex.get(planBasedWithinDayAgent.getId()) == currentPlanElementIndex;
	}

	private boolean isAgentNextDrivingAwayFromParking(PlanBasedWithinDayAgent planBasedWithinDayAgent) {
		Plan plan = planBasedWithinDayAgent.getSelectedPlan();
		// check whether there is a next leg
		if (plan.getPlanElements().size() > currentPlanElementIndex + 1) {
			if (plan.getPlanElements().get(currentPlanElementIndex+1) instanceof ActivityImpl){
				System.out.println();
			}
			
			LegImpl leg = (LegImpl) plan.getPlanElements().get(currentPlanElementIndex+1);
			return leg.getMode().equals(TransportMode.car);			
		} else return false;
	}

	private void updateParkingScoreDuringDay(ActivityEndEvent event) {
		double parkingScore = 0.0;

		Id personId = event.getPersonId();

		PlanElement currentPlanElement = agents.get(personId).getSelectedPlan().getPlanElements().get(currentPlanElementIndex);
		
		if (!this.parkingArrivalTime.containsKey(personId)){
			System.out.println();
		}
		
		double parkingArrivalTime = this.parkingArrivalTime.get(personId);
		double parkingDepartureTime = event.getTime();
		double parkingDuration = GeneralLib.getIntervalDuration(parkingArrivalTime, parkingDepartureTime);
		double activityDuration = GeneralLib.getIntervalDuration(startTimeOfFirstActivityAfterParkingCar.get(personId),
				endTimeOfLastActivityBeforeLeavingWithCar.get(personId));
		Id parkingFacilityId = event.getFacilityId();

		// parking cost scoring
		Double parkingCost = getParkingCost(parkingArrivalTime, parkingDuration, parkingFacilityId);

		if (parkingCost == null) {
			DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		}

		double costScore = getParkingCostScore(personId, parkingCost);
		parkingScore += costScore;

		// parking walk time
		
		double walkingTimeTotalInMinutes = (walkDurationFromParking.get(personId) + walkDurationToParking.get(personId)) / 60.0;
		double walkScore = getWalkScore(personId, activityDuration, walkingTimeTotalInMinutes);
		parkingScore += walkScore;

		// parking search time

		double parkingSearchDurationInMinutes = getParkingSearchDurationInMinutes(personId, parkingArrivalTime);

		double searchTimeScore = getSearchTimeScore(personId, activityDuration, parkingSearchDurationInMinutes);
		parkingScore += searchTimeScore;

		parkingIterationScoreSum.incrementBy(personId, parkingScore);

		Integer previousCarLegPlanElementIndex = getIndexOfPreviousCarLeg(personId);

		// update score of currently selected strategy
		parkingStrategyManager.updateScore(personId, previousCarLegPlanElementIndex, parkingScore);

		// reset search time
		getParkingSearchStartTime().remove(personId);

		parkingWalkTimesLog.put(personId, new Pair<Id, Double>(parkingFacilityId, walkingTimeTotalInMinutes));
		parkingSearchTimesLog.put(personId, new Pair<Id, Double>(parkingFacilityId, parkingSearchDurationInMinutes));

		parkingCostLog.put(personId, new Pair<Id, Double>(parkingFacilityId, parkingCost));

		parkingOccupancy.updateParkingOccupancy(parkingFacilityId, parkingArrivalTime, parkingDepartureTime,
				((ParkingInfrastructure_v2) parkingInfrastructure).getParkingCapacity(parkingFacilityId));
	}

	private Integer getIndexOfPreviousCarLeg(Id personId) {
		PlanBasedWithinDayAgent agent = this.agents.get(personId);
		Plan executedPlan = agent.getSelectedPlan();

		for (int i = currentPlanElementIndex; i > 0; i--) {
			List<PlanElement> planElements = executedPlan.getPlanElements();
			if (planElements.get(i) instanceof Leg) {
				Leg leg = (Leg) planElements.get(i);

				if (leg.getMode().equals(TransportMode.car)) {
					return i;
				}
			}
		}

		DebugLib.stopSystemAndReportInconsistency("this is not allowed to happen - assumption broken");
		return null;
	}

	private Double getParkingCost(double parkingArrivalTime, double parkingDuration, Id facilityId) {
		return parkingInfrastructure.getParkingCostCalculator().getParkingCost(facilityId, null, null, parkingArrivalTime,
				parkingDuration);
	}

	public double getParkingCostScore(Id personId, double parkingArrivalTime, double parkingDuration, Id facilityId) {
		Double parkingCost = getParkingCost(parkingArrivalTime, parkingDuration, facilityId);

		// forSettingBreakPoint(facilityId, parkingCost);

		if (parkingCost == null) {
			DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		}

		return getParkingCostScore(personId, parkingCost);
	}

	public double getParkingCostScore(Id personId, Double parkingCost) {
		if (parkingCost == null) {
			DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		}

		return parkingPersonalBetas.getParkingCostBeta(personId) * parkingCost;
	}

	public double getWalkScore(Id personId, double activityDuration, double walkingTimeTotalInMinutes) {
		return parkingPersonalBetas.getParkingWalkTimeBeta(personId, activityDuration) * walkingTimeTotalInMinutes;
	}

	private double getParkingSearchDurationInMinutes(Id personId, double parkingArrivalTime) {
		double parkingSearchDurationInMinutes = 0;

		parkingSearchDurationInMinutes = GeneralLib.getIntervalDuration(getParkingSearchStartTime().get(personId), parkingArrivalTime) / 60;

		return parkingSearchDurationInMinutes;
	}

	public double getSearchTimeScore(Id personId, double activityDuration, double parkingSearchTimeInMinutes) {
		return parkingPersonalBetas.getParkingSearchTimeBeta(personId, activityDuration) * parkingSearchTimeInMinutes;
	}

	@Override
	//public void notifyAfterMobsim(AfterMobsimEvent event) {
	public void notifyScoring(ScoringEvent event) {
	//public void notifyIterationEnds(IterationEndsEvent event) {
		//super.notifyIterationEnds(event);
		//super.notifyAfterMobsim(event);
		for (Id personId : this.parkingIterationScoreSum.keySet()) {
			processScoreOfLastParking(personId);

			ScoringFunction scoringFunction = event.getControler().getPlansScoring().getScoringFunctionForAgent(personId);

			double amount = GlobalParkingSearchParams.getParkingScoreWeight() * parkingIterationScoreSum.get(personId);
			scoringFunction.addMoney(amount);
		}

		IntegerValueHashMap<Id> facilityCapacities = ((ParkingInfrastructure_v2) parkingInfrastructure)
				.getParkingFacilityCapacities();

		if (getParkingAnalysisHandler() != null) {
			getParkingAnalysisHandler()
					.updateParkingOccupancyStatistics(parkingOccupancy, facilityCapacities, event.getIteration());
			getParkingAnalysisHandler().processParkingWalkTimes(parkingWalkTimesLog, event.getIteration());
			getParkingAnalysisHandler().processParkingSearchTimes(parkingSearchTimesLog, event.getIteration());
			getParkingAnalysisHandler().processParkingCost(parkingCostLog, event.getIteration());
			getParkingAnalysisHandler().printShareOfCarUsers();
		}

		/*
		 * IntegerValueHashMap<ParkingStrategy> numberOfTimesStrategyUser=new
		 * IntegerValueHashMap<ParkingStrategy>(); for (Id
		 * personId:parkingStrategyManager
		 * .getCurrentlySelectedParkingStrategies().getKeySet1()){ for (Integer
		 * index:parkingStrategyManager.getCurrentlySelectedParkingStrategies().
		 * getKeySet2(personId)){
		 * numberOfTimesStrategyUser.increment(parkingStrategyManager
		 * .getCurrentlySelectedParkingStrategies().get(personId, index)); } }
		 * 
		 * //"Strategies used by agents" for (ParkingStrategy
		 * ps:numberOfTimesStrategyUser.getKeySet()){
		 * log.info("parking strategy currently selected (iteration):\t" +
		 * event.getIteration() + "\t" + ps.getIdentifier() + "\t" +
		 * numberOfTimesStrategyUser.get(ps)); }
		 */
	}

	private void processScoreOfLastParking(Id personId) {
		double parkingScore = 0.0;

		Double parkingArrivalTime = lastParkingArrivalTimeOfDay.get(personId);
		
		double lastParkingActivityDurationOfDay = GeneralLib.getIntervalDuration(parkingArrivalTime,
				firstParkingDepartureTimeOfDay.get(personId));

		
		
		// parking cost scoring

		Id lastParkingFacilityIdOfDay = this.lastParkingFacilityIdOfDay.get(personId);

		Double parkingCost = getParkingCost(parkingArrivalTime, lastParkingActivityDurationOfDay, lastParkingFacilityIdOfDay);

		if (parkingCost == null) {
			DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		}

		double costScore = getParkingCostScore(personId, parkingCost);

		parkingScore += costScore;

		// parking walk time

		double walkingTimeTotalInMinutes = (firstParkingWalkTimeOfDay.get(personId) + lastParkingWalkTimeOfDay.get(personId)) / 60.0;
		double walkScore = getWalkScore(personId, lastParkingActivityDurationOfDay, walkingTimeTotalInMinutes);
		parkingScore += walkScore;

		// parking search time

		double parkingSearchTimeInMinutes = getParkingSearchDurationInMinutes(personId, parkingArrivalTime);

		double searchTimeScore = getSearchTimeScore(personId, lastParkingActivityDurationOfDay, parkingSearchTimeInMinutes);
		parkingScore += searchTimeScore;

		if (walkScore > 0 || costScore > 0 || searchTimeScore > 0) {
			DebugLib.stopSystemAndReportInconsistency();
		}

		parkingIterationScoreSum.incrementBy(personId, parkingScore);

		Integer lastCarLegIndexOfDay = lastParkingActivityPlanElemIndex.get(personId) - 1;

		parkingStrategyManager.updateScore(personId, lastCarLegIndexOfDay, parkingScore);

		parkingWalkTimesLog.put(personId, new Pair<Id, Double>(lastParkingFacilityIdOfDay, walkingTimeTotalInMinutes));
		parkingSearchTimesLog.put(personId, new Pair<Id, Double>(lastParkingFacilityIdOfDay, parkingSearchTimeInMinutes));

		parkingCostLog.put(personId, new Pair<Id, Double>(lastParkingFacilityIdOfDay, parkingCost));

		// double
		// firstDepartureTimeOfDay=durationOfLastParkingOfDay.getFirstDepartureTimeOfDay(personId);

		parkingOccupancy.updateParkingOccupancy(lastParkingFacilityIdOfDay, parkingArrivalTime, parkingArrivalTime
				+ lastParkingActivityDurationOfDay,
				((ParkingInfrastructure_v2) parkingInfrastructure).getParkingCapacity(lastParkingFacilityIdOfDay));
	}

	public ParkingAnalysisHandler getParkingAnalysisHandler() {
		return parkingAnalysisHandler;
	}

	public void setParkingAnalysisHandler(ParkingAnalysisHandler parkingAnalysisHandler) {
		this.parkingAnalysisHandler = parkingAnalysisHandler;
	}

	

}
