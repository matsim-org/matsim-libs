/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingScoreManager.java
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.Pair;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.scoring.ScoringFunction;

import playground.christoph.parking.core.events.ParkingSearchEvent;
import playground.christoph.parking.core.events.handler.ParkingSearchEventHandler;
import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.parkingSearch.withinDay_v_STRC.core.mobsim.ParkingInfrastructure_v2;
import playground.wrashid.parkingSearch.withinDay_v_STRC.util.ParkingAgentsTracker_v2;
import playground.wrashid.parkingSearch.withindayFW.analysis.ParkingAnalysisHandler;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyStats;
import playground.wrashid.parkingSearch.withindayFW.util.GlobalParkingSearchParams;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

public class ParkingScoreManager extends ParkingAgentsTracker_v2 implements ActivityHandler, LegHandler, 
		AfterMobsimListener, PersonStuckEventHandler, LinkLeaveEventHandler, TeleportationArrivalEventHandler, ParkingSearchEventHandler {

	private static final Logger log = Logger.getLogger(ParkingScoreManager.class);
	
	private final ParkingPersonalBetas parkingPersonalBetas;

	private EventsToActivities eventsToActivities;
	private EventsToLegs eventsToLegs;
	
	private DoubleValueHashMap<Id> parkingIterationScoreSum;
	private LinkedListValueHashMap<Id, Pair<Id, Double>> parkingWalkTimesLog;
	private LinkedListValueHashMap<Id, Pair<Id, Double>> parkingSearchTimesLog;
	private LinkedListValueHashMap<Id, Pair<Id, Double>> parkingCostLog;
	private ParkingOccupancyStats parkingOccupancy;

	private final Map<Id, List<PlanElement>> planElementMap;
	private final Set<Id> stuckAgents;
	
	private final Map<Leg, Double> parkingSearchStartTimes;
	private final Map<Id, Double> searchingAgentStartTimes;
	
	private ParkingAnalysisHandler parkingAnalysisHandler;

	private ParkingScoreEvaluator parkingScoreEvaluator;

	public ParkingScoreManager(Scenario scenario, ParkingInfrastructure parkingInfrastructure, double distance,
			Controler controler, ParkingPersonalBetas parkingPersonalBetas) {
		super(scenario, parkingInfrastructure, distance, controler);
		
		this.parkingPersonalBetas = parkingPersonalBetas;
		
		this.eventsToActivities = new EventsToActivities();
		this.eventsToActivities.setActivityHandler(this);
		
		this.eventsToLegs = new EventsToLegs();
		this.eventsToLegs.setLegHandler(this);
		
		this.planElementMap = new HashMap<Id, List<PlanElement>>();
		for (Id personId : scenario.getPopulation().getPersons().keySet()) this.planElementMap.put(personId, new ArrayList<PlanElement>());
		
		this.stuckAgents = new HashSet<Id>();
		this.searchingAgentStartTimes = new HashMap<Id, Double>();
		this.parkingSearchStartTimes = new HashMap<Leg, Double>();
		
		this.parkingScoreEvaluator=new ParkingScoreEvaluator((ParkingInfrastructure_v2) parkingInfrastructure,parkingPersonalBetas);
	}
	
	@Override
	public void reset(int iter) {
		super.reset(iter);
		
		this.eventsToActivities.reset(iter);
		this.eventsToLegs.reset(iter);
		
		for (List<PlanElement> list : this.planElementMap.values()) list.clear();
		
		this.stuckAgents.clear();
		this.searchingAgentStartTimes.clear();
		this.parkingSearchStartTimes.clear();
		
		parkingIterationScoreSum = new DoubleValueHashMap<Id>();
		parkingWalkTimesLog = new LinkedListValueHashMap<Id, Pair<Id,Double>>();
		parkingSearchTimesLog = new LinkedListValueHashMap<Id, Pair<Id,Double>>();
		parkingCostLog = new LinkedListValueHashMap<Id, Pair<Id,Double>>();
		parkingOccupancy = new ParkingOccupancyStats();
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		super.handleEvent(event);
		
		this.eventsToActivities.handleEvent(event);
	}
	
	@Override
	public void handleEvent(ActivityEndEvent event) {
		super.handleEvent(event);
		
		this.eventsToActivities.handleEvent(event);
	}

    @Override
    public void handleEvent(PersonArrivalEvent event) {
    	super.handleEvent(event);
    	
    	this.eventsToLegs.handleEvent(event);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
    	super.handleEvent(event);
    	
    	this.eventsToLegs.handleEvent(event);
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
    	super.handleEvent(event);
    	
    	this.eventsToLegs.handleEvent(event);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
    	
    	this.eventsToLegs.handleEvent(event);
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
    	
    	this.eventsToLegs.handleEvent(event);
    }
    

	@Override
	public void handleEvent(ParkingSearchEvent event) {
		this.searchingAgentStartTimes.put(event.getPersonId(), event.getTime());
	}	

	private void updateParkingScoreDuringDay(Id personId, List<PlanElement> planElements, int firstParkingActivityIndex,
			int secondParkingActivityIndex) {
			
		

		Activity firstParkingActivity = (Activity) planElements.get(firstParkingActivityIndex);
		Activity secondParkingActivity = (Activity) planElements.get(secondParkingActivityIndex);
		
		Activity firstActivity = (Activity) planElements.get(firstParkingActivityIndex + 2);
		Activity lastActivity = (Activity) planElements.get(secondParkingActivityIndex - 2);
		
		Leg walkLegFromParking = (Leg) planElements.get(firstParkingActivityIndex + 1);
		Leg walkLegToParking = (Leg) planElements.get(secondParkingActivityIndex - 1);
		
		Leg carLegToFirstParking = (Leg) planElements.get(firstParkingActivityIndex - 1);
		
		double parkingArrivalTime = firstParkingActivity.getStartTime();
		double parkingDepartureTime = secondParkingActivity.getEndTime();
		double parkingDuration = GeneralLib.getIntervalDuration(parkingArrivalTime, parkingDepartureTime);
		double activityDuration = GeneralLib.getIntervalDuration(firstActivity.getStartTime(), lastActivity.getEndTime());
		
		Id parkingFacilityId = firstParkingActivity.getFacilityId();
		double parkingSearchDurationInSeconds = getParkingSearchDurationInMinutes(carLegToFirstParking, parkingArrivalTime) * 60.0;

		ParkingActivityAttributes parkingActivityAttributes = new ParkingActivityAttributes(personId, parkingFacilityId, parkingArrivalTime, parkingDuration, activityDuration, parkingSearchDurationInSeconds, walkLegFromParking.getTravelTime(), walkLegToParking.getTravelTime());
		double parkingScore = getParkingScoreEvaluator().getParkingScore(parkingActivityAttributes);
		
		// parking cost scoring
		//Double parkingCost = getParkingCost(parkingArrivalTime, parkingDuration, parkingFacilityId);

		//if (parkingCost == null) {
		//	DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		//}

		//double costScore = getParkingCostScore(personId, parkingCost);
		//parkingScore += costScore;

		// parking walk time
		//double walkingTimeTotalInMinutes = (walkLegFromParking.getTravelTime() + walkLegToParking.getTravelTime()) / 60.0;
		//double walkScore = getWalkScore(personId, activityDuration, walkingTimeTotalInMinutes);
		//parkingScore += walkScore;

		// parking search time

		//double searchTimeScore = getSearchTimeScore(personId, activityDuration, parkingSearchDurationInSeconds);
		//parkingScore += searchTimeScore;

		parkingIterationScoreSum.incrementBy(personId, parkingScore);

		int previousCarLegPlanElementIndex = firstParkingActivityIndex - 1;

		// update score of currently selected strategy
		parkingStrategyManager.updateScore(personId, previousCarLegPlanElementIndex, parkingScore);

		// reset search time
//		this.parkingSearchStartTime.remove(personId);

		parkingWalkTimesLog.put(personId, new Pair<Id, Double>(parkingFacilityId, parkingActivityAttributes.getTotalWalkDurationInSeconds()/60.0));
		parkingSearchTimesLog.put(personId, new Pair<Id, Double>(parkingFacilityId, parkingSearchDurationInSeconds));

		parkingCostLog.put(personId, new Pair<Id, Double>(parkingFacilityId, getParkingScoreEvaluator().getParkingCost(parkingActivityAttributes)));

		parkingOccupancy.updateParkingOccupancy(parkingFacilityId, parkingArrivalTime, parkingDepartureTime,
				((ParkingInfrastructure_v2) parkingInfrastructure).getParkingCapacity(parkingFacilityId));
	}

	



	

	private double getParkingSearchDurationInMinutes(Leg carLeg, double parkingArrivalTime) {
		double parkingSearchDurationInMinutes = 0;

		parkingSearchDurationInMinutes = GeneralLib.getIntervalDuration(this.getParkingSearchTime(carLeg), parkingArrivalTime) / 60;

		return parkingSearchDurationInMinutes;
	}

	private double getParkingSearchTime(Leg carLeg) {
		Double parkingSearchStartTime = this.parkingSearchStartTimes.get(carLeg);
		if (parkingSearchStartTime == null) return 0.0;
		else return parkingSearchStartTime;
	}
	

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		for (Entry<Id, List<PlanElement>> entry : this.planElementMap.entrySet()) {

			Id personId = entry.getKey();
			List<PlanElement> planElements = entry.getValue();
			
			// ignore stuck agents
			if (this.stuckAgents.contains(personId)) {
				continue;
			}
			
			// identify all parking activities
			List<Integer> parkingActivityIndices = new ArrayList<Integer>();
			for (int planElementIndex = 0; planElementIndex < planElements.size(); planElementIndex++) {
				
				PlanElement planElement = planElements.get(planElementIndex);
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					if (activity.getType().equalsIgnoreCase("parking")) {
						parkingActivityIndices.add(planElementIndex);
					}
				}
			}

			// check whether agent as parking activities
			if (parkingActivityIndices.size() == 0) {
				continue;
			}
			
			if (parkingActivityIndices.size() < 2) {
				log.info("Agent " + personId.toString() + " performs only a single parking activity. This might be a problem. Ignoring agent!");
				continue;
			}
			
			// identify the first and last parking activities
			int firstParkingIndex = parkingActivityIndices.remove(0);
			int lastParkingIndex = parkingActivityIndices.remove(parkingActivityIndices.size() - 1);
						
			// we expect an even number of remaining parking activities
			if (parkingActivityIndices.size() % 2 != 0) {
				log.info("Agent " + personId.toString() + " performs an odd number of parking activity. This might be a problem. Ignoring agent!");
				continue;
			}
			
			/*
			 * Always handle a pair of parking activities.
			 * - parking before activity
			 *   - walk leg to activity
			 *   - activity (some other activities and non-car legs might be performed as well)
			 *   - walk leg from activity
			 * - parking after activity
			 */
			for (int planElementIndex = 0; planElementIndex < parkingActivityIndices.size(); planElementIndex+=2) {

				int firstParkingActivityIndex = parkingActivityIndices.get(planElementIndex);
				int secondParkingActivityIndex = parkingActivityIndices.get(planElementIndex + 1);
				
				updateParkingScoreDuringDay(personId, planElements, firstParkingActivityIndex, secondParkingActivityIndex);
			}
			
			processScoreOfLastParking(personId, planElements, firstParkingIndex, lastParkingIndex);

			ScoringFunction scoringFunction = event.getControler().getPlansScoring().getScoringFunctionForAgent(personId);

			double amount = GlobalParkingSearchParams.getParkingScoreWeight() * parkingIterationScoreSum.get(personId);
			scoringFunction.addMoney(amount);
		}

		IntegerValueHashMap<Id> facilityCapacities = ((ParkingInfrastructure_v2) parkingInfrastructure).getParkingFacilityCapacities();

		if (getParkingAnalysisHandler() != null) {
			getParkingAnalysisHandler().updateParkingOccupancyStatistics(parkingOccupancy, facilityCapacities, event.getIteration());
			getParkingAnalysisHandler().processParkingWalkTimes(parkingWalkTimesLog, event.getIteration());
			getParkingAnalysisHandler().processParkingSearchTimes(parkingSearchTimesLog, event.getIteration());
			getParkingAnalysisHandler().processParkingCost(parkingCostLog, event.getIteration());
			getParkingAnalysisHandler().printShareOfCarUsers();
		}
	}

	private void processScoreOfLastParking(Id personId, List<PlanElement> planElements, int firstParkingIndex, int lastParkingIndex) {
		
		Activity firstParkingActivity = (Activity) planElements.get(firstParkingIndex);
		Activity lastParkingActivity = (Activity) planElements.get(lastParkingIndex);
		
		Activity lastMorningNonParkingActivity= (Activity) planElements.get(firstParkingIndex-2);
		Activity firstEveningNonParkingActivity= (Activity) planElements.get(firstParkingIndex+2);
		
		Leg walkLegToFirstParkingActivity = (Leg) planElements.get(firstParkingIndex - 1);
		Leg walkLegFromLastParkingActivity = (Leg) planElements.get(lastParkingIndex + 1);
		
		Leg carLegToLastParkingActivity = (Leg) planElements.get(lastParkingIndex - 1);
		
		
		
		if (!walkLegToFirstParkingActivity.getMode().equals(TransportMode.walk)) {
			throw new RuntimeException("Expected a walk leg to the first parking acivity but found a " + 
					walkLegToFirstParkingActivity.getMode() + " leg!");
		}
		
		if (!walkLegFromLastParkingActivity.getMode().equals(TransportMode.walk)) {
			throw new RuntimeException("Expected a walk leg from the last parking acivity but found a " + 
					walkLegFromLastParkingActivity.getMode() + " leg!");
		}
		
		if (!carLegToLastParkingActivity.getMode().equals(TransportMode.car)) {
			throw new RuntimeException("Expected a car leg to the last parking acivity but found a " + 
					carLegToLastParkingActivity.getMode() + " leg!");
		}
		
		double firstParkingDepartureTime = firstParkingActivity.getEndTime();
		double lastParkingArrivalTime = lastParkingActivity.getStartTime();
		
		double lastParkingActivityDurationOfDay = GeneralLib.getIntervalDuration(lastParkingArrivalTime, firstParkingDepartureTime);
		
		// parking cost scoring
		Id lastParkingFacilityIdOfDay = lastParkingActivity.getFacilityId();
		double parkingSearchDurationInSeconds = getParkingSearchDurationInMinutes(carLegToLastParkingActivity, lastParkingArrivalTime);

		double activityDuration=GeneralLib.getIntervalDuration(firstEveningNonParkingActivity.getStartTime(), lastMorningNonParkingActivity.getEndTime());
		ParkingActivityAttributes parkingActivityAttributes = new ParkingActivityAttributes(personId, lastParkingFacilityIdOfDay, lastParkingArrivalTime, lastParkingActivityDurationOfDay, activityDuration, parkingSearchDurationInSeconds, walkLegFromLastParkingActivity.getTravelTime(), walkLegToFirstParkingActivity.getTravelTime());
		double parkingScore = getParkingScoreEvaluator().getParkingScore(parkingActivityAttributes);
		
		
		
		
		
		
		
		
		
		//Double parkingCost = getParkingCost(lastParkingArrivalTime, lastParkingActivityDurationOfDay, lastParkingFacilityIdOfDay);

		//if (parkingCost == null) {
		//	DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		//}

		//double costScore = getParkingCostScore(personId, parkingCost);

		//parkingScore += costScore;

		// parking walk time
		//double walkingTimeTotalInMinutes = (walkLegToFirstParkingActivity.getTravelTime() + walkLegFromLastParkingActivity.getTravelTime()) / 60.0;
		//double walkScore = getWalkScore(personId, lastParkingActivityDurationOfDay, walkingTimeTotalInMinutes);
		//parkingScore += walkScore;

		// parking search time

		//double searchTimeScore = getSearchTimeScore(personId, lastParkingActivityDurationOfDay, parkingSearchTimeInSeconds);
		//parkingScore += searchTimeScore;

		//if (walkScore > 0 || costScore > 0 || searchTimeScore > 0) {
		//	DebugLib.stopSystemAndReportInconsistency();
		//}

		parkingIterationScoreSum.incrementBy(personId, parkingScore);

		int lastCarLegIndexOfDay = lastParkingIndex - 1;

		parkingStrategyManager.updateScore(personId, lastCarLegIndexOfDay, parkingScore);

		parkingWalkTimesLog.put(personId, new Pair<Id, Double>(lastParkingFacilityIdOfDay, parkingActivityAttributes.getTotalWalkDurationInSeconds()/60.0));
		parkingSearchTimesLog.put(personId, new Pair<Id, Double>(lastParkingFacilityIdOfDay, parkingActivityAttributes.getParkingSearchDurationInSeconds()));

		parkingCostLog.put(personId, new Pair<Id, Double>(lastParkingFacilityIdOfDay, getParkingScoreEvaluator().getParkingCost(parkingActivityAttributes)));

		parkingOccupancy.updateParkingOccupancy(lastParkingFacilityIdOfDay, lastParkingArrivalTime, lastParkingArrivalTime
				+ lastParkingActivityDurationOfDay,
				((ParkingInfrastructure_v2) parkingInfrastructure).getParkingCapacity(lastParkingFacilityIdOfDay));
	}

	public ParkingAnalysisHandler getParkingAnalysisHandler() {
		return parkingAnalysisHandler;
	}

	public void setParkingAnalysisHandler(ParkingAnalysisHandler parkingAnalysisHandler) {
		this.parkingAnalysisHandler = parkingAnalysisHandler;
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.stuckAgents.add(event.getPersonId());
	}

	@Override
	public void handleLeg(Id agentId, Leg leg) {
		this.planElementMap.get(agentId).add(leg);
		
		// if the agent started its parking search in the current leg, add it to the map
		Double time = this.searchingAgentStartTimes.remove(agentId);
		if (time != null) {
			this.parkingSearchStartTimes.put(leg, time);
		}
	}

	@Override
	public void handleActivity(Id agentId, Activity activity) {
		this.planElementMap.get(agentId).add(activity);
	}

	public ParkingScoreEvaluator getParkingScoreEvaluator() {
		return parkingScoreEvaluator;
	}

}