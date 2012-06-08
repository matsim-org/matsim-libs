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

package playground.wrashid.parkingSearch.withindayFW;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifier;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.DoubleValueHashMap;
import playground.wrashid.lib.obj.HashMapHashSetConcat;
import playground.wrashid.parkingSearch.withinday.ParkingInfrastructure;
import playground.wrashid.parkingSearch.withindayFW.impl.ParkingStrategyManager;

// TODO: clearly inspect, which variables have not been reset at beginning of 1st iteration (after 0th iteration).
public class ParkingAgentsTracker implements LinkEnterEventHandler, AgentArrivalEventHandler, AgentDepartureEventHandler,
		MobsimInitializedListener, MobsimAfterSimStepListener, ActivityEndEventHandler, AfterMobsimListener {

	private final Scenario scenario;
	private final double distance;

	private final Set<Id> carLegAgents;
	private final Set<Id> searchingAgents;
	private final Set<Id> linkEnteredAgents;
	private final Set<Id> lastTimeStepsLinkEnteredAgents;
	private final Map<Id, ActivityFacility> nextActivityFacilityMap;
	private final Map<Id, ExperimentalBasicWithindayAgent> agents;
	private final Map<Id, Id> selectedParkingsMap;
	private final Map<Id, Activity> nextNonParkingActivity;
	private final ParkingInfrastructure parkingInfrastructure;
	private Map<Id, Id> lastParkingFacilityId;
	private Map<Id, Double> lastCarArrivalTimeAtParking;
	private DoubleValueHashMap<Id> parkingIterationScoreSum;
	private ParkingStrategyManager parkingStrategyManager;
	private HashMapHashSetConcat<DuringLegIdentifier,Id> activeReplanningIdentifiers;

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
		this.nextActivityFacilityMap = new HashMap<Id, ActivityFacility>();
		this.agents = new HashMap<Id, ExperimentalBasicWithindayAgent>();
		this.nextNonParkingActivity = new HashMap<Id, Activity>();
		this.lastCarArrivalTimeAtParking = new HashMap<Id, Double>();
		this.parkingIterationScoreSum = new DoubleValueHashMap<Id>();
		this.setActiveReplanningIdentifiers(new HashMapHashSetConcat<DuringLegIdentifier, Id>());
	}

	public Set<Id> getSearchingAgents() {
		return Collections.unmodifiableSet(this.searchingAgents);
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (ExperimentalBasicWithindayAgent) agent);
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
	public void handleEvent(AgentDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.car)) {

			parkingInfrastructure.unParkVehicle(lastParkingFacilityId.get(event.getPersonId()));

			this.carLegAgents.add(event.getPersonId());

			ExperimentalBasicWithindayAgent agent = this.agents.get(event.getPersonId());
			Plan executedPlan = agent.getSelectedPlan();
			int planElementIndex = agent.getCurrentPlanElementIndex();
			

			/*
			 * Get the coordinate of the next non-parking activity's facility.
			 * The currentPlanElement is a car leg, which is followed by a
			 * parking activity and a walking leg to the next non-parking
			 * activity.
			 */
			Activity nextNonParkingActivity = (Activity) executedPlan.getPlanElements().get(planElementIndex + 3);
			this.getNextNonParkingActivity().put(agent.getId(), nextNonParkingActivity);

			ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities()
					.get(nextNonParkingActivity.getFacilityId());
			nextActivityFacilityMap.put(event.getPersonId(), facility);

			Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
			double distanceToNextActivity = CoordUtils.calcDistance(facility.getCoord(), coord);

			/*
			 * If the agent is within distance 'd' to target activity or OR If
			 * the agent enters the link where its next non-parking activity is
			 * performed, mark him ash searching Agent.
			 * 
			 * (this is actually handling a special case, where already at
			 * departure time the agent is within distance 'd' of next
			 * activity).
			 */
			if (shouldStartSearchParking(event.getLinkId(), facility.getLinkId(), distanceToNextActivity)) {
				searchingAgents.add(event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.carLegAgents.remove(event.getPersonId());
		this.searchingAgents.remove(event.getPersonId());
		this.linkEnteredAgents.remove(event.getPersonId());
		this.selectedParkingsMap.remove(event.getPersonId());

		logParkingArrivalTime(event);
	}

	private void logParkingArrivalTime(AgentArrivalEvent event) {
		ExperimentalBasicWithindayAgent agent = this.agents.get(event.getPersonId());
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();

		Leg previousLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex - 1);
		Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex - 1);

		if (previousLeg.getMode().equals(TransportMode.car) && nextLeg.getMode().equals(TransportMode.walk)) {
			// car arrived
			lastCarArrivalTimeAtParking.put(event.getPersonId(), event.getTime());
		}

	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (carLegAgents.contains(event.getPersonId())) {
			if (!searchingAgents.contains(event.getPersonId())) {
				Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
				ActivityFacility facility = nextActivityFacilityMap.get(event.getPersonId());
				double distanceToNextActivity = CoordUtils.calcDistance(facility.getCoord(), coord);

				/*
				 * If the agent is within the parking radius
				 */
				/*
				 * If the agent enters the link where its next non-parking
				 * activity is performed.
				 */

				if (shouldStartSearchParking(event.getLinkId(), facility.getLinkId(), distanceToNextActivity)) {
					searchingAgents.add(event.getPersonId());
					linkEnteredAgents.add(event.getPersonId());
					updateIdentifierOfAgentForParkingSearch(event.getPersonId());
				}
			}
			// the agent is already searching: update its position
			else {
				linkEnteredAgents.add(event.getPersonId());
				updateIdentifierOfAgentForParkingSearch(event.getPersonId());
			}
		}
	}

	private void updateIdentifierOfAgentForParkingSearch(Id personId) {
		ExperimentalBasicWithindayAgent agent = this.agents.get(personId);
		int planElementIndex = agent.getCurrentPlanElementIndex();
		
		getActiveReplanningIdentifiers().put(parkingStrategyManager.getCurrentlySelectedParkingStrategies().get(personId, planElementIndex).getIdentifier(), personId);
	}

	private boolean shouldStartSearchParking(Id currentLinkId, Id nextActivityLinkId, double distanceToNextActivity) {
		return distanceToNextActivity <= distance || nextActivityLinkId.equals(currentLinkId);
	}

	@Override
	public void reset(int iteration) {
		agents.clear();
		carLegAgents.clear();
		searchingAgents.clear();
		linkEnteredAgents.clear();
		selectedParkingsMap.clear();
		nextActivityFacilityMap.clear();
		lastTimeStepsLinkEnteredAgents.clear();
		this.parkingIterationScoreSum = new DoubleValueHashMap<Id>();
	}

	public Map<Id, Activity> getNextNonParkingActivity() {
		return nextNonParkingActivity;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equalsIgnoreCase("parking")) {
			lastParkingFacilityId.put(event.getPersonId(), event.getFacilityId());

			updateParkingScore(event);
		}

	}

	private void updateParkingScore(ActivityEndEvent event) {
		ExperimentalBasicWithindayAgent agent = this.agents.get(event.getPersonId());
		Plan executedPlan = agent.getSelectedPlan();
		int planElementIndex = agent.getCurrentPlanElementIndex();

		Leg previousLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex - 1);
		Leg nextLeg = (Leg) executedPlan.getPlanElements().get(planElementIndex - 1);

		if (previousLeg.getMode().equals(TransportMode.walk) && nextLeg.getMode().equals(TransportMode.car)) {
			Double parkingArrivalTime = lastCarArrivalTimeAtParking.get(event.getPersonId());
			double parkingDuration = GeneralLib.getIntervalDuration(parkingArrivalTime, event.getTime());

			Double parkingCost = parkingInfrastructure.getParkingCostCalculator().getParkingCost(event.getFacilityId(),
					parkingArrivalTime, parkingDuration);

			// car departuing

			// TODO: continue!!!

			// search time?

			// walk time (asymetrie möglich!)?

			// where to get the personal beta's of agents?
			
			parkingIterationScoreSum.incrementBy(event.getPersonId(),0.0);
		}

	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for (Id personId : this.parkingIterationScoreSum.keySet()) {
			ScoringFunction scoringFunction = event.getControler().getPlansScoring().getScoringFunctionForAgent(personId);
			scoringFunction.addMoney(parkingIterationScoreSum.get(personId));
		}
	}

	public ParkingStrategyManager getParkingStrategyManager() {
		return parkingStrategyManager;
	}
	public void setParkingStrategyManager(ParkingStrategyManager parkingStrategyManager) {
		this.parkingStrategyManager=parkingStrategyManager;
	}

	public HashMapHashSetConcat<DuringLegIdentifier,Id> getActiveReplanningIdentifiers() {
		return activeReplanningIdentifiers;
	}

	public void setActiveReplanningIdentifiers(HashMapHashSetConcat<DuringLegIdentifier,Id> activeReplanningIdentifiers) {
		this.activeReplanningIdentifiers = activeReplanningIdentifiers;
	}

}
