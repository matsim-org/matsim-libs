/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingAgentsTracker.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.withindayFW.core;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.contrib.parking.lib.obj.LinkedListValueHashMap;
import org.matsim.contrib.parking.lib.obj.Pair;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import playground.wrashid.lib.obj.HashMapHashSetConcat;
import playground.wrashid.lib.obj.TwoHashMapsConcatenated;
import playground.wrashid.lib.obj.event.EventHandlerCodeSeparator;
import playground.wrashid.parkingSearch.withindayFW.analysis.ParkingAnalysisHandler;
import playground.wrashid.parkingSearch.withindayFW.parkingOccupancy.ParkingOccupancyStats;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.*;
import playground.wrashid.parkingSearch.withindayFW.util.GlobalParkingSearchParams;
import playground.wrashid.parkingSearch.withindayFW.util.ParallelSafePlanElementAccessLib;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

import java.util.*;

// TODO: clearly inspect, which variables have not been reset at beginning of 1st iteration (after 0th iteration).

/**
 * Requirements: If we have car1-park1-walk1-act-walk2-park2-car2, scoring
 * should happen at the end of "park2" activity. The scoring of the last parking
 * activity of the day should happen at the end of the iteration
 * (AfterMobSimListener).
 * 
 * 
 * 
 * @author wrashid
 * 
 */

public class ParkingAgentsTracker extends EventHandlerCodeSeparator implements MobsimInitializedListener,
		MobsimAfterSimStepListener, AfterMobsimListener, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;

	protected static final Logger log = Logger.getLogger(ParkingAgentsTracker.class);
	private final Scenario scenario;
	private final double distance;
	private ParkingOccupancyStats parkingOccupancy;
	private ParkingAnalysisHandler parkingAnalysisHandler;
	private final Set<Id> carLegAgents;
	private final Set<Id> searchingAgents;
	private final Set<Id> linkEnteredAgents;
	private final Set<Id> lastTimeStepsLinkEnteredAgents;
	protected final Map<Id, PersonDriverAgentImpl> agents;
	private final Map<Id, Id> selectedParkingsMap;
	private final Map<Id, Activity> nextNonParkingActivity;
	private final ParkingInfrastructure parkingInfrastructure;
	private Map<Id, Id> lastParkingFacilityId;
	private UpdateLastParkingArrivalTime lastCarArrivalTimeAtParking;
	private DoubleValueHashMap<Id> parkingIterationScoreSum;
	private ParkingStrategyManager parkingStrategyManager;
	private HashMapHashSetConcat<DuringLegAgentSelector, Id> activeReplanningIdentifiers;
	private Map<Id, Double> previousNonParkingActivityStartTime;
	// private Map<Id, Double> firstParkingWalkTime;
	// private Map<Id, Double> secondParkingWalkTime;
	private Map<Id, Double> searchStartTime;
	private Map<Id, Double> lastCarMovementRegistered;
	private Set<Id> didUseCarOnce;

	private Map<Id, Double> endTimeOfPreviousActivity = new HashMap<Id, Double>();

	private Map<Id, Double> firstParkingWalkTimeOfDay = new HashMap<Id, Double>();

	private Map<Id, Integer> firstParkingActivityPlanElemIndex = new HashMap<Id, Integer>();
	private Map<Id, Integer> lastParkingActivityPlanElemIndex = new HashMap<Id, Integer>();
	private CaptureParkingWalkTimesDuringDay parkingWalkTimesDuringDay;
	private CaptureWalkDurationOfFirstAndLastOfDay walkDurationFirstAndLastOfDay;
	private CaptureDurationOfLastParkingOfDay durationOfLastParkingOfDay;

	private CapturePreviousActivityDurationDuringDay previousActivityDurationDuringDay;

	private CaptureLastActivityDurationOfDay durationOfLastActivityOfDay;
	private CaptureFirstCarDepartureTimeOfDay firstCarDepartureTimeOfDay;
	
	// persondId, <parkingFacilityId,walkTimeForBothParkingLegsInMinutes>
	private LinkedListValueHashMap<Id, Pair<Id,Double>> parkingWalkTimesLog;
	private LinkedListValueHashMap<Id, Pair<Id,Double>> parkingSearchTimesLog;
	private LinkedListValueHashMap<Id, Pair<Id,Double>> parkingCostLog;

	/**
	 * Tracks agents' car legs and check whether they have to start their
	 * parking search.
	 * 
	 * @param scenario
	 * @param distance
	 *            defines in which distance to the destination of a car trip an
	 *            agent starts its parking search
	 * @param parkingInfrastructure
	 */
	public ParkingAgentsTracker(Scenario scenario, double distance, ParkingInfrastructure parkingInfrastructure) {
		super();
		
		this.parkingOccupancy = new ParkingOccupancyStats();
		this.scenario = scenario;
		this.distance = distance;
		this.parkingInfrastructure = parkingInfrastructure;

		this.carLegAgents = new HashSet<Id>();
		this.linkEnteredAgents = new HashSet<Id>();
		this.selectedParkingsMap = new HashMap<Id, Id>();
		this.lastTimeStepsLinkEnteredAgents = new TreeSet<Id>(); // This set has
																	// to be be
																	// deterministic!
		this.searchingAgents = new HashSet<Id>();
		this.agents = new HashMap<Id, PersonDriverAgentImpl>();
		this.nextNonParkingActivity = new HashMap<Id, Activity>();

		this.parkingIterationScoreSum = new DoubleValueHashMap<Id>();
		this.setActiveReplanningIdentifiers(new HashMapHashSetConcat<DuringLegAgentSelector, Id>());
		this.previousNonParkingActivityStartTime = new HashMap<Id, Double>();
		this.setSearchStartTime(new HashMap<Id, Double>());
		this.lastParkingFacilityId = new HashMap<Id, Id>();
		this.lastCarMovementRegistered = new HashMap<Id, Double>();
		this.endTimeOfPreviousActivity = new HashMap<Id, Double>();
		this.didUseCarOnce = new HashSet<Id>();
		this.parkingWalkTimesLog=new LinkedListValueHashMap<Id, Pair<Id,Double>>();
		this.parkingSearchTimesLog=new LinkedListValueHashMap<Id, Pair<Id,Double>>();
		this.parkingCostLog=new LinkedListValueHashMap<Id, Pair<Id,Double>>();

	}

	private void initHandlers() {
		addHandler(new UpdateEndTimeOfPreviousActivity(endTimeOfPreviousActivity));

		this.parkingWalkTimesDuringDay = new CaptureParkingWalkTimesDuringDay(agents, firstParkingActivityPlanElemIndex,
				lastParkingActivityPlanElemIndex);
		addHandler(this.parkingWalkTimesDuringDay);

		this.walkDurationFirstAndLastOfDay = new CaptureWalkDurationOfFirstAndLastOfDay(agents,
				firstParkingActivityPlanElemIndex, lastParkingActivityPlanElemIndex);
		addHandler(this.walkDurationFirstAndLastOfDay);

		this.durationOfLastParkingOfDay = new CaptureDurationOfLastParkingOfDay();
		addHandler(this.durationOfLastParkingOfDay);

		this.lastCarArrivalTimeAtParking = new UpdateLastParkingArrivalTime(agents);
		addHandler(this.lastCarArrivalTimeAtParking);

		this.previousActivityDurationDuringDay = new CapturePreviousActivityDurationDuringDay(agents,
				firstParkingActivityPlanElemIndex, lastParkingActivityPlanElemIndex);
		addHandler(this.previousActivityDurationDuringDay);

		this.durationOfLastActivityOfDay = new CaptureLastActivityDurationOfDay(agents, firstParkingActivityPlanElemIndex,
				lastParkingActivityPlanElemIndex);
		addHandler(this.durationOfLastActivityOfDay);

		this.setFirstCarDepartureTimeOfDay(new CaptureFirstCarDepartureTimeOfDay());
		addHandler(this.getFirstCarDepartureTimeOfDay());
	}

	public Set<Id> getSearchingAgents() {
		return Collections.unmodifiableSet(this.searchingAgents);
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (PersonDriverAgentImpl) agent);
		}

		initializeFirstAndLastParkingActPlanElemIndex();

		initHandlers();
	}

	private void initializeFirstAndLastParkingActPlanElemIndex() {
		for (PersonDriverAgentImpl agent : this.agents.values()) {
			Plan executedPlan = agent.getCurrentPlan();

			for (int i = 0; i < executedPlan.getPlanElements().size(); i++) {
				Id personId = agent.getPerson().getId();
				if (!firstParkingActivityPlanElemIndex.containsKey(personId)) {
					if (executedPlan.getPlanElements().get(i) instanceof ActivityImpl) {
						Activity act = (Activity) executedPlan.getPlanElements().get(i);
						if (act.getType().equalsIgnoreCase("parking")) {
							firstParkingActivityPlanElemIndex.put(personId, i);
							break;
						}
					}
				}
			}

			for (int i = executedPlan.getPlanElements().size() - 1; i >= 0; i--) {
				Id personId = agent.getPerson().getId();
				if (!lastParkingActivityPlanElemIndex.containsKey(personId)) {
					if (executedPlan.getPlanElements().get(i) instanceof ActivityImpl) {
						Activity act = (Activity) executedPlan.getPlanElements().get(i);
						if (act.getType().equalsIgnoreCase("parking")) {
							lastParkingActivityPlanElemIndex.put(personId, i);
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		lastTimeStepsLinkEnteredAgents.clear();
		lastTimeStepsLinkEnteredAgents.addAll(linkEnteredAgents);
		linkEnteredAgents.clear();
	}

	public Set<Id> getLinkEnteredAgents() {
		return lastTimeStepsLinkEnteredAgents;
	}

	public void setSelectedParking(Id agentId, Id parkingFacilityId) {
		selectedParkingsMap.put(agentId, parkingFacilityId);
	}

	public Id getSelectedParking(Id agentId) {
		return selectedParkingsMap.get(agentId);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		super.handleEvent(event);

		if (event.getLegMode().equals(TransportMode.car)) {
			Id personId = event.getPersonId();

			getLastCarMovementTime().put(personId, event.getTime());

			getParkingInfrastructure().unParkVehicle(lastParkingFacilityId.get(personId));

			this.carLegAgents.add(personId);

			PersonDriverAgentImpl agent = this.agents.get(personId);
			Plan executedPlan = agent.getCurrentPlan();
			int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

			

			// TwoHashMapsConcatenated<Id, Integer, ParkingStrategy>
			// currentlySelectedParkingStrategies =
			// parkingStrategyManager.getCurrentlySelectedParkingStrategies();
			// activeReplanningIdentifiers.put(currentlySelectedParkingStrategies.get(personId,
			// planElementIndex).getIdentifier(), personId);

			/*
			 * Get the coordinate of the next non-parking activity's facility.
			 * The currentPlanElement is a car leg, which is followed by a
			 * parking activity and a walking leg to the next non-parking
			 * activity.
			 */
			Activity nextNonParkingActivity = (Activity) executedPlan.getPlanElements().get(planElementIndex + 3);
			this.getNextNonParkingActivity().put(agent.getId(), nextNonParkingActivity);

			Link nextActivityLink = getNextActivityLink(personId);
	
			//nextActivityFacilityMap.put(personId, facility);

			Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
			double distanceToNextActivity = CoordUtils.calcEuclideanDistance(nextActivityLink.getCoord(), coord);

			/*
			 * If the agent is within distance 'd' to target activity or OR If
			 * the agent enters the link where its next non-parking activity is
			 * performed, mark him ash searching Agent.
			 * 
			 * (this is actually handling a special case, where already at
			 * departure time the agent is within distance 'd' of next
			 * activity).
			 */

			if (planElementIndex == 33) {
				
				PersonDriverAgentImpl experimentalBasicWithindayAgent = agents.get(personId);
				
				
				 DebugLib.traceAgent(personId,10);
			}
			
		
			if (shouldStartSearchParking(event.getLinkId(), nextActivityLink, distanceToNextActivity)) {
				
				if (planElementIndex == 33) {
					
					PersonDriverAgentImpl experimentalBasicWithindayAgent = agents.get(personId);
					
					
					 DebugLib.traceAgent(personId,10);
				}
				
				searchingAgents.add(personId);
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		super.handleEvent(event);

		if ( WithinDayAgentUtils.getCurrentPlanElementIndex(agents.get(event.getPersonId())) == 33 ) {
			 DebugLib.traceAgent(event.getPersonId(),10);
		}

		Id personId = event.getPersonId();
		this.carLegAgents.remove(personId);
		this.searchingAgents.remove(personId);
		this.linkEnteredAgents.remove(personId);
		this.selectedParkingsMap.remove(personId);

		PersonDriverAgentImpl agent = this.agents.get(personId);
		
		int planElementIndex = ParallelSafePlanElementAccessLib.getCurrentExpectedLegIndex(agent);
		TwoHashMapsConcatenated<Id, Integer, ParkingStrategy> currentlySelectedParkingStrategies = parkingStrategyManager
				.getCurrentlySelectedParkingStrategies();

		if (event.getLegMode().equals(TransportMode.car)) {
			getLastCarMovementTime().put(personId, event.getTime());
			activeReplanningIdentifiers.removeValue(currentlySelectedParkingStrategies.get(personId, planElementIndex)
					.getIdentifier(), personId);
		}

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		super.handleEvent(event);
		
//		Id personId = event.getDriverId();
		Id personId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;

		Integer currentPlanElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agents.get(personId)) ;
		

		if (currentPlanElementIndex == 33) {
			
			PersonDriverAgentImpl experimentalBasicWithindayAgent = agents.get(personId);
			
			
			 DebugLib.traceAgent(personId,10);
		}
		
		getLastCarMovementTime().put(personId, event.getTime());
		if (carLegAgents.contains(personId)) {
			if (!searchingAgents.contains(personId)) {
				Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
				
				Link nextActivityLink = getNextActivityLink(personId);
				
				double distanceToNextActivity = CoordUtils.calcEuclideanDistance(nextActivityLink.getCoord(), coord);

				/*
				 * If the agent is within the parking radius
				 */
				/*
				 * If the agent enters the link where its next non-parking
				 * activity is performed.
				 */

				if (currentPlanElementIndex == 33) {
					
					PersonDriverAgentImpl experimentalBasicWithindayAgent = agents.get(personId);
					
					
					 DebugLib.traceAgent(personId,10);
				}
				
				
				
				if (shouldStartSearchParking(event.getLinkId(), nextActivityLink, distanceToNextActivity)) {
					searchingAgents.add(personId);
					linkEnteredAgents.add(personId);
					updateIdentifierOfAgentForParkingSearch(personId);
				}
			}
			// the agent is already searching: update its position
			else {
				linkEnteredAgents.add(personId);
				updateIdentifierOfAgentForParkingSearch(personId);
			}
		}
	}

	private Link getNextActivityLink(Id personId) {
		Integer currentPlanElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agents.get(personId));

		ActivityImpl act=(ActivityImpl) agents.get(personId).getCurrentPlan().getPlanElements().get(currentPlanElementIndex+1);
		
		Id actLinkId=act.getLinkId();
		Link actLink = scenario.getNetwork().getLinks().get(actLinkId);
		
		return actLink;
	}

	private void updateIdentifierOfAgentForParkingSearch(Id personId) {
		PersonDriverAgentImpl agent = this.agents.get(personId);
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		if (WithinDayAgentUtils.getCurrentPlanElementIndex(agents.get(personId)) == 3) {
			// DebugLib.traceAgent(personId);
		}

		if (parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, planElementIndex) == null) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		if (parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, planElementIndex).getIdentifier() == null) {
			DebugLib.emptyFunctionForSettingBreakPoint();
		}

		getActiveReplanningIdentifiers().put(
				parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, planElementIndex).getIdentifier(),
				personId);
	}

	
	private boolean shouldStartSearchParking(Id currentLinkId, Link nextActivityLink, double distanceToNextActivity) {
		return distanceToNextActivity <= distance || nextActivityLink.getId().equals(currentLinkId) || nextActivityLink.getLength()>distance;
	}

	@Override
	public void reset(int iteration) {
		super.reset(iteration);
		
		delegate.reset(iteration);

		agents.clear();
		carLegAgents.clear();
		searchingAgents.clear();
		linkEnteredAgents.clear();
		selectedParkingsMap.clear();
		lastTimeStepsLinkEnteredAgents.clear();
		this.parkingIterationScoreSum = new DoubleValueHashMap<Id>();
		didUseCarOnce.clear();

		firstParkingActivityPlanElemIndex.clear();
		lastParkingActivityPlanElemIndex.clear();
		parkingOccupancy = new ParkingOccupancyStats();
		parkingWalkTimesLog=new LinkedListValueHashMap<Id, Pair<Id,Double>>();
		parkingSearchTimesLog=new LinkedListValueHashMap<Id, Pair<Id,Double>>();
		parkingCostLog=new LinkedListValueHashMap<Id, Pair<Id,Double>>();
	}

	public Map<Id, Activity> getNextNonParkingActivity() {
		return nextNonParkingActivity;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		// DebugLib.traceAgent(event.getDriverId());

		super.handleEvent(event);

		Id personId = event.getPersonId();
		endTimeOfPreviousActivity.put(personId, event.getTime());

		PersonDriverAgentImpl agent = this.agents.get(personId);
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = ParallelSafePlanElementAccessLib.getCurrentExpectedActIndex(agent);

		if (event.getActType().equalsIgnoreCase("parking")) {
			lastParkingFacilityId.put(personId, event.getFacilityId());

			if (executedPlan.getPlanElements().size()<=planElementIndex+1){
				//TODO: this is just a hack!
				planElementIndex-=2;
			}
			
			Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex + 1);
			
			if (isPlanElementDuringDay(personId, planElementIndex) && nextLeg.getMode().equals(TransportMode.car)) {

				updateParkingScoreDuringDay(event);

			}

		} else {

		}

	}

	private boolean isPlanElementDuringDay(Id personId, int planElementIndex) {
		return planElementIndex > firstParkingActivityPlanElemIndex.get(personId)
				&& planElementIndex < lastParkingActivityPlanElemIndex.get(personId);
	}

	// precondition: method used on activity with type parking
	// private boolean isEndParkingActivity(Id personId) {
	// ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
	// Plan executedPlan = agent.getSelectedPlan();
	// int planElementIndex = agent.getCurrentPlanElementIndex();
	//
	// Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex +
	// 1);
	//
	// return nextLeg.getMode().equals(TransportMode.car);
	// }

	private void updateParkingScoreDuringDay(ActivityEndEvent event) {
		double parkingScore = 0.0;

		Id personId = event.getPersonId();

		double parkingArrivalTime = lastCarArrivalTimeAtParking.getTime(personId);
		double parkingDepartureTime = event.getTime();
		double parkingDuration = GeneralLib.getIntervalDuration(lastCarArrivalTimeAtParking.getTime(personId),
				parkingDepartureTime);
		double activityDuration = previousActivityDurationDuringDay.getDuration(personId);
		Id parkingFacilityId = event.getFacilityId();
		
		
		

		// parking cost scoring
		Double parkingCost = getParkingCost(parkingArrivalTime, parkingDuration, parkingFacilityId);

		forSettingBreakPoint(parkingFacilityId, parkingCost);
		
		
		if (parkingCost == null) {
			DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		}
		
		double costScore=getParkingCostScore(personId,parkingCost);
		parkingScore += costScore;

		// parking walk time

		double walkingTimeTotalInMinutes = parkingWalkTimesDuringDay.getSumBothParkingWalkDurationsInSecond(personId) / 60.0;
		double walkScore = getWalkScore(personId, activityDuration, walkingTimeTotalInMinutes);
		parkingScore += walkScore;

		// parking search time

		if (this.getSearchStartTime().get(personId) == null) {
			List<PlanElement> planElements = agents.get(personId).getCurrentPlan().getPlanElements();
			System.out.println(WithinDayAgentUtils.getCurrentPlanElementIndex(agents.get(personId)));

			System.out
					.println("first possiblity: probably, you should start earlier the search with this algorithm (agent did not register with any identifier for this search till now and already drove to initially planned parking)");

			System.out
			.println("second probability: two consquete parking are on the same link");
			
			System.out
			.println("third probability: the network is too coarse, so that the agent reaches the destination, even before search area started (due to long link) => this should be fixed now in method 'shouldStartSearchParking'");
			
			// as initial parking can be located away from real destination
			// (even further away for test cases than the start search range,
			// this error can occur => must fix it - one way to do it: place
			// initial parking always at the destination link (TODO)

		}

		double parkingSearchTimeInMinutes = getParkingSearchTimeInMinutes(personId, parkingArrivalTime);

		if (parkingFacilityId.toString().contains("gp") && parkingSearchTimeInMinutes>0){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		double searchTimeScore = getSearchTimeScore(personId, activityDuration, parkingSearchTimeInMinutes);
		parkingScore += searchTimeScore;
		
		if (walkScore>0 || costScore>0 || searchTimeScore>0){
			DebugLib.stopSystemAndReportInconsistency();
		}

		parkingIterationScoreSum.incrementBy(personId, parkingScore);

		Integer previousCarLegPlanElementIndex = getIndexOfPreviousCarLeg(personId);
		parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, previousCarLegPlanElementIndex)
				.putScore(personId, previousCarLegPlanElementIndex, parkingScore);

		// System.out.println(agents.get(personId).getCurrentPlanElementIndex());
		// Integer currentPlanElementIndex =
		// agents.get(personId).getCurrentPlanElementIndex();
		// DebugLib.traceAgent(personId);
		// reset search time
		getSearchStartTime().remove(personId);

		parkingWalkTimesLog.put(personId, new Pair<Id, Double>(parkingFacilityId, walkingTimeTotalInMinutes));
		parkingSearchTimesLog.put(personId, new Pair<Id, Double>(parkingFacilityId, parkingSearchTimeInMinutes));
		
		parkingCostLog.put(personId, new Pair<Id, Double>(parkingFacilityId, parkingCost));
		
		parkingOccupancy.updateParkingOccupancy(parkingFacilityId, parkingArrivalTime, parkingDepartureTime,parkingInfrastructure.getFacilityCapacities().get(parkingFacilityId));
	
		
	}

	private double getParkingSearchTimeInMinutes(Id personId, double parkingArrivalTime) {
		double parkingSearchTimeInMinutes=0;
		if (ifParkingSearchTimeDifferentThanZero(personId)) {
			if (fixedParkingSearchTime(personId)){
				parkingSearchTimeInMinutes=-1*this.getSearchStartTime().get(personId)/60;
			} else {
				parkingSearchTimeInMinutes = GeneralLib.getIntervalDuration(this.getSearchStartTime().get(personId),
						parkingArrivalTime) / 60;
			}
		}
		return parkingSearchTimeInMinutes;
	}

	private boolean fixedParkingSearchTime(Id personId) {
		return this.getSearchStartTime().get(personId)<0;
	}

	private boolean ifParkingSearchTimeDifferentThanZero(Id personId) {
		
		if (this.getSearchStartTime().get(personId)==null){
			DebugLib.emptyFunctionForSettingBreakPoint();
		}
		
		return this.getSearchStartTime().get(personId) != Double.NEGATIVE_INFINITY;
	}

	private Integer getIndexOfPreviousCarLeg(Id personId) {
		PersonDriverAgentImpl agent = this.agents.get(personId);
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		for (int i = planElementIndex; i > 0; i--) {
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

	private void updateNextParkingActivityIfNeeded(Id personId) {
		PersonDriverAgentImpl agent = this.agents.get(personId);
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		Activity currentParking = (Activity) executedPlan.getPlanElements().get(planElementIndex);
		Activity nextParking = (Activity) executedPlan.getPlanElements().get(planElementIndex + 2);

		if (currentParking.getLinkId() == nextParking.getLinkId()) {
			Id newParkingFacilityId = getParkingInfrastructure().getClosestParkingFacilityNotOnLink(currentParking.getCoord(),
					currentParking.getLinkId());
			Activity newParkingAct = InsertParkingActivities.createParkingActivity(scenario, newParkingFacilityId);
			executedPlan.getPlanElements().remove(planElementIndex + 2);
			executedPlan.getPlanElements().add(planElementIndex + 2, newParkingAct);
		}

	}

	public double getSearchTimeScore(Id personId, double activityDuration, double parkingSearchTimeInMinutes) {
		ParkingPersonalBetas parkingPersonalBetas = parkingStrategyManager.getParkingPersonalBetas();
		return parkingPersonalBetas.getParkingSearchTimeBeta(personId, activityDuration) * parkingSearchTimeInMinutes;
	}

	public double getWalkScore(Id personId, double activityDuration, double walkingTimeTotalInMinutes) {
		ParkingPersonalBetas parkingPersonalBetas = parkingStrategyManager.getParkingPersonalBetas();

		return parkingPersonalBetas.getParkingWalkTimeBeta(personId, activityDuration) * walkingTimeTotalInMinutes;

	}

	
	
	
	public double getParkingCostScore(Id personId, double parkingArrivalTime, double parkingDuration, Id facilityId) {
		Double parkingCost = getParkingCost(parkingArrivalTime, parkingDuration, facilityId);

		//forSettingBreakPoint(facilityId, parkingCost);
		
		if (parkingCost == null) {
			DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		}
		
		return getParkingCostScore(personId,parkingCost);
	}
	
	public double getParkingCostScore(Id personId,Double parkingCost) {
		ParkingPersonalBetas parkingPersonalBetas = parkingStrategyManager.getParkingPersonalBetas();
		
		if (parkingCost == null) {
			DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		}
		
		return parkingPersonalBetas.getParkingCostBeta(personId) * parkingCost;
	}
	

	private Double getParkingCost(double parkingArrivalTime, double parkingDuration, Id facilityId) {
		return getParkingInfrastructure().getParkingCostCalculator().getParkingCost(facilityId, parkingArrivalTime,
				parkingDuration);
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for (Id personId : this.parkingIterationScoreSum.keySet()) {
			processScoreOfLastParking(personId);

            PlansScoring result;
//            throw new RuntimeException("To modify scoring for your Agents, please either:" +
//"(1) throw a PersonMoneyEvent for an appropriate amount or" +
//"(2) set a custom ScoringFunctionFactory which calculates what you need or" +
//"(3) talk to developers list.");
//            ScoringFunction scoringFunction = result.getScoringFunctionForAgent(personId);
			ScoringFunction scoringFunction = null;
			double amount = GlobalParkingSearchParams.getParkingScoreWeight() *  parkingIterationScoreSum.get(personId);
			scoringFunction.addMoney(amount);
		}
		
		log.info("parking capacity constraint violations:" + parkingOccupancy.getNumberOfMaximumParkingCapacitConstraintViolations());
		
		if (getParkingAnalysisHandler()!=null){
			getParkingAnalysisHandler().updateParkingOccupancyStatistics(parkingOccupancy, parkingInfrastructure.getFacilityCapacities(), event.getIteration());
			getParkingAnalysisHandler().processParkingWalkTimes(parkingWalkTimesLog, event.getIteration());
			getParkingAnalysisHandler().processParkingSearchTimes(parkingSearchTimesLog, event.getIteration());
			getParkingAnalysisHandler().processParkingCost(parkingCostLog, event.getIteration());
			getParkingAnalysisHandler().printShareOfCarUsers();
		}
		
		IntegerValueHashMap<ParkingStrategy> numberOfTimesStrategyUser=new IntegerValueHashMap<ParkingStrategy>();
		for (Id personId:parkingStrategyManager.getCurrentlySelectedParkingStrategies().getKeySet1()){
			for (Integer index:parkingStrategyManager.getCurrentlySelectedParkingStrategies().getKeySet2(personId)){
				numberOfTimesStrategyUser.increment(parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, index));
			}
		}
		
		//"Strategies used by agents"
		for (ParkingStrategy ps:numberOfTimesStrategyUser.getKeySet()){
			log.info("parking strategy currently selected (iteration):\t" + event.getIteration() + "\t" +  ps.getIdentifier() + "\t" + numberOfTimesStrategyUser.get(ps));
		}
	}

	private void processScoreOfLastParking(Id personId) {
		double parkingScore = 0.0;

		Double parkingArrivalTime = lastCarArrivalTimeAtParking.getTime(personId);
		double lastParkingActivityDurationOfDay = durationOfLastParkingOfDay.getDuration(personId);

		// parking cost scoring

		Id lastParkingFacilityIdOfDay = getLastParkingFacilityIdOfDay(personId);
		
		
		Double parkingCost = getParkingCost(parkingArrivalTime, lastParkingActivityDurationOfDay, lastParkingFacilityIdOfDay);

		forSettingBreakPoint(lastParkingFacilityIdOfDay, parkingCost);
		
		if (parkingCost == null) {
			DebugLib.stopSystemAndReportInconsistency("probably the facilityId set is not that of a parking, resp. no mapping found");
		}
		
		double costScore=getParkingCostScore(personId,parkingCost);
		
		parkingScore += costScore;

		// parking walk time

		double walkingTimeTotalInMinutes = walkDurationFirstAndLastOfDay.getSumBothParkingWalkDurationsInSecond(personId) / 60.0;
		double walkScore = getWalkScore(personId, lastParkingActivityDurationOfDay, walkingTimeTotalInMinutes);
		parkingScore += walkScore;

		// parking search time
		double parkingSearchTimeInMinutes=getParkingSearchTimeInMinutes(personId, parkingArrivalTime);
		
		double searchTimeScore = getSearchTimeScore(personId, lastParkingActivityDurationOfDay, parkingSearchTimeInMinutes);
		parkingScore += searchTimeScore;

		if (walkScore>0 || costScore>0 || searchTimeScore>0){
			DebugLib.stopSystemAndReportInconsistency();
		}
		
		parkingIterationScoreSum.incrementBy(personId, parkingScore);

		Integer lastCarLegIndexOfDay = getLastCarLegIndexOfDay(personId);
		parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, lastCarLegIndexOfDay)
				.putScore(personId, lastCarLegIndexOfDay, parkingScore);

		parkingWalkTimesLog.put(personId, new Pair<Id, Double>(lastParkingFacilityIdOfDay, walkingTimeTotalInMinutes));
		parkingSearchTimesLog.put(personId, new Pair<Id, Double>(lastParkingFacilityIdOfDay, parkingSearchTimeInMinutes));
		
		parkingCostLog.put(personId, new Pair<Id, Double>(lastParkingFacilityIdOfDay, parkingCost));
		
		//double firstDepartureTimeOfDay=durationOfLastParkingOfDay.getFirstDepartureTimeOfDay(personId);
		
		parkingOccupancy.updateParkingOccupancy(lastParkingFacilityIdOfDay, parkingArrivalTime, parkingArrivalTime+lastParkingActivityDurationOfDay, parkingInfrastructure.getFacilityCapacities().get(lastParkingFacilityIdOfDay));
	}

	private void forSettingBreakPoint(Id parkingFacilityId, Double parkingCost) {
		if (parkingFacilityId.toString().contains("gp") || parkingFacilityId.toString().contains("stp")){
			if (parkingCost>70){
				DebugLib.emptyFunctionForSettingBreakPoint();
			}
		}
	}

	private Integer getLastCarLegIndexOfDay(Id personId) {
		PersonDriverAgentImpl agent = this.agents.get(personId);
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		List<PlanElement> planElements = executedPlan.getPlanElements();
		// -4 should is the first possible index of a car leg, starting from the
		// end of day
		for (int i = planElements.size() - 4; i > 0; i--) {

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

	private Id getLastParkingFacilityIdOfDay(Id personId) {
		PersonDriverAgentImpl agent = this.agents.get(personId);
		Plan executedPlan = agent.getCurrentPlan();

		Activity lastParkingActivity = (Activity) executedPlan.getPlanElements().get(
				lastParkingActivityPlanElemIndex.get(personId));

		return lastParkingActivity.getFacilityId();
	}

	public ParkingStrategyManager getParkingStrategyManager() {
		return parkingStrategyManager;
	}

	public void setParkingStrategyManager(ParkingStrategyManager parkingStrategyManager) {
		this.parkingStrategyManager = parkingStrategyManager;
	}

	public HashMapHashSetConcat<DuringLegAgentSelector, Id> getActiveReplanningIdentifiers() {
		return activeReplanningIdentifiers;
	}

	public void setActiveReplanningIdentifiers(HashMapHashSetConcat<DuringLegAgentSelector, Id> activeReplanningIdentifiers) {
		this.activeReplanningIdentifiers = activeReplanningIdentifiers;
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		super.handleEvent(event);

		if (!event.getActType().equalsIgnoreCase("parking")) {
			previousNonParkingActivityStartTime.put(event.getPersonId(), event.getTime());
		}

		if (event.getActType().equalsIgnoreCase("parking")) {
			PersonDriverAgentImpl agent = this.agents.get(event.getPersonId());
			Plan executedPlan = agent.getCurrentPlan();
			int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

			Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex + 1);

			if (!didUseCarOnce.contains(event.getPersonId()) && nextLeg.getMode().equals(TransportMode.car)) {
				double walkDuration = GeneralLib.getIntervalDuration(endTimeOfPreviousActivity.get(event.getPersonId()),
						event.getTime());
				firstParkingWalkTimeOfDay.put(event.getPersonId(), walkDuration);
				didUseCarOnce.add(event.getPersonId());
			}

			Activity nextAct = (Activity) executedPlan.getPlanElements().get(planElementIndex + 2);

			if (nextAct.getType().equalsIgnoreCase("parking")) {
				// if current parking activity linkId==next parking activity
				// link Id => change link Id of next parking activity!
				// updateNextParkingActivityIfNeeded(event.getDriverId());
			}

		}
	}

	public void putSearchStartTime(Id personId, double searchStartTime) {
		this.getSearchStartTime().put(personId, searchStartTime);
	}

	public Map<Id, Double> getSearchStartTime() {
		return searchStartTime;
	}

	public void setSearchStartTime(Map<Id, Double> searchStartTime) {
		this.searchStartTime = searchStartTime;
	}

	public Map<Id, Double> getLastCarMovementTime() {
		return lastCarMovementRegistered;
	}

	public void setLastCarMovementRegistered(Map<Id, Double> lastCarMovementRegistered) {
		this.lastCarMovementRegistered = lastCarMovementRegistered;
	}

	public ParkingInfrastructure getParkingInfrastructure() {
		return parkingInfrastructure;
	}

	public CaptureFirstCarDepartureTimeOfDay getFirstCarDepartureTimeOfDay() {
		return firstCarDepartureTimeOfDay;
	}

	public void setFirstCarDepartureTimeOfDay(CaptureFirstCarDepartureTimeOfDay firstCarDepartureTimeOfDay) {
		this.firstCarDepartureTimeOfDay = firstCarDepartureTimeOfDay;
	}

	public ParkingAnalysisHandler getParkingAnalysisHandler() {
		return parkingAnalysisHandler;
	}

	public void setParkingAnalysisHandler(ParkingAnalysisHandler parkingAnalysisHandler) {
		this.parkingAnalysisHandler = parkingAnalysisHandler;
	}

	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}
	
	
}
