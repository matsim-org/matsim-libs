/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingAgentsTracker.java
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

package playground.christoph.parking.withinday.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.facilities.ActivityFacility;
import org.matsim.withinday.mobsim.MobsimDataProvider;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;

public class ParkingAgentsTracker implements LinkEnterEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler, MobsimAfterSimStepListener,
		MobsimEngine {

	private static final Logger log = Logger.getLogger(ParkingAgentsTracker.class);
	
	// TODO: allow each agent to decided whether starting its parking search or not
	
	protected final Scenario scenario;
	protected final ParkingInfrastructure parkingInfrastructure;
	protected final MobsimDataProvider mobsimDataProvider;
	private final double distance;

	private final Set<Id> carLegAgents;
	private final Set<Id> searchingAgents;
	private final Set<Id> linkEnteredAgents;
	private final Set<Id> lastTimeStepsLinkEnteredAgents;
	private final Map<Id, ActivityFacility> nextActivityFacilityMap;
	private final Map<Id, Id> selectedParkingsMap;
	private final Set<Id> recentlyArrivedDrivers;
	private final Map<Id, Id> recentlyDepartingDrivers;
	private final Set<Id> recentlyWaitingDrivers;

	protected InternalInterface internalIterface;
	
	/**
	 * Tracks agents' car legs and check whether they have to start their parking search.
	 * 
	 * @param scenario
	 * @param distance
	 *            defines in which distance to the destination of a car trip an
	 *            agent starts its parking search
	 */
	public ParkingAgentsTracker(Scenario scenario, ParkingInfrastructure parkingInfrastructure, 
			MobsimDataProvider mobsimDataProvider, double distance) {
		this.scenario = scenario;
		this.parkingInfrastructure = parkingInfrastructure;
		this.mobsimDataProvider = mobsimDataProvider;
		this.distance = distance;

		this.carLegAgents = new HashSet<Id>();
		this.linkEnteredAgents = new HashSet<Id>();
		this.selectedParkingsMap = new HashMap<Id, Id>();
		this.lastTimeStepsLinkEnteredAgents = new TreeSet<Id>(); // This set has to be deterministic!
		this.searchingAgents = new HashSet<Id>();
		this.nextActivityFacilityMap = new HashMap<Id, ActivityFacility>();
		this.recentlyArrivedDrivers = new HashSet<Id>();
		this.recentlyDepartingDrivers = new HashMap<Id, Id>();
		this.recentlyWaitingDrivers = new HashSet<Id>();
	}

	public Set<Id> getSearchingAgents() {
		return Collections.unmodifiableSet(this.searchingAgents);
	}

	/*
	 * The code could probably be moved to the doSimStep(...) method if it is ensured
	 * that it registered after QNetsimEnding and MultiModalEngine in the QSim.
	 * However, think also about race conditions (not all events from the current time step
	 * may have been processed when the doSimStep(...) method is called.
	 */
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		lastTimeStepsLinkEnteredAgents.clear();
		lastTimeStepsLinkEnteredAgents.addAll(linkEnteredAgents);
		linkEnteredAgents.clear();
		
		/*
		 * If parked is false, the agent waits for a parking spot to become available.
		 * For those agents, the duration and end time of the parking activity is set 
		 * to Time.UNDEFINED_TIME. As a result, the agent should not end the activity
		 * anymore.
		 */
		for (Id agentId : this.recentlyWaitingDrivers) {
			MobsimAgent agent = this.mobsimDataProvider.getAgent(agentId);
			Activity parkingActivity = (Activity) ((PlanAgent) agent).getCurrentPlanElement();
			parkingActivity.setEndTime(Time.UNDEFINED_TIME);
			parkingActivity.setMaximumDuration(Time.UNDEFINED_TIME);
			WithinDayAgentUtils.resetCaches(agent);
			this.internalIterface.rescheduleActivityEnd(agent);
		}
		this.recentlyWaitingDrivers.clear();
		
		/*
		 * Move waiting vehicles to free parkings.
		 * Update the waiting agents' activity end times.
		 */
		Collection<Id> parkedAgentIds = this.parkingInfrastructure.waitingToParking();
		for (Id agentId : parkedAgentIds) {
			MobsimAgent agent = this.mobsimDataProvider.getAgent(agentId);
			Activity parkingActivity = (Activity) ((PlanAgent) agent).getCurrentPlanElement();
			parkingActivity.setEndTime(e.getSimulationTime() + 180);
			parkingActivity.setMaximumDuration(parkingActivity.getEndTime() - parkingActivity.getStartTime());
			WithinDayAgentUtils.resetCaches(agent);
			this.internalIterface.rescheduleActivityEnd(agent);
		}
	}

	/*
	 * Agents that are searching and that have just entered a new link.
	 */
	public Set<Id> getLinkEnteredAgents() {
		return lastTimeStepsLinkEnteredAgents;
	}

	public void setSelectedParking(Id agentId, Id parkingFacilityId, boolean reserveAsWaiting) {
		
		this.selectedParkingsMap.put(agentId, parkingFacilityId);
		
//		Id vehicleId = this.parkingInfrastructure.getVehicleId(agents.get(agentId).getSelectedPlan().getPerson());
		Id vehicleId = agentId;	// so far, this is true...
		
		if (!reserveAsWaiting) {
			this.parkingInfrastructure.reserveParking(vehicleId, parkingFacilityId);			
		} else {
			this.parkingInfrastructure.reserveWaiting(vehicleId, parkingFacilityId);
		}
	}

	public Id getSelectedParking(Id agentId) {
		return this.selectedParkingsMap.get(agentId);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		
		// get the facility Id where the agent performed the activity
		Id facilityId = this.recentlyDepartingDrivers.remove(event.getPersonId());

		if (event.getLegMode().equals(TransportMode.car)) {
			this.carLegAgents.add(event.getPersonId());

			// Get the agent's next non-parking activity and the facility where it is performed.
			Activity nextNonParkingActivity = getNextNonParkingActivity(event.getPersonId());
			ActivityFacility facility = ((ScenarioImpl) scenario).getActivityFacilities().getFacilities().get(nextNonParkingActivity.getFacilityId());
			this.nextActivityFacilityMap.put(event.getPersonId(), facility);
			
			// Get the coordinate of the next non-parking activity's facility.
			Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
			double distanceToNextActivity = CoordUtils.calcDistance(facility.getCoord(), coord);

			/*
			 * If the agent is within distance 'd' to target activity or OR if the
			 * agent enters the link where its next non-parking activity is
			 * performed, mark him as searching agent.
			 * 
			 * (this is actually handling a special case, where already at departure time
			 * the agent is within distance 'd' of next activity).
			 */
			if (shouldStartSearchParking(event.getLinkId(), facility.getLinkId(), distanceToNextActivity)) {
				searchingAgents.add(event.getPersonId());
			}
			
			// mark the parking slot as free
			Id vehicleId = event.getPersonId();	// so far, this is true...
			this.parkingInfrastructure.unParkVehicle(vehicleId, facilityId);
			
			/*
			 * TODO: Check whether an agent's leg starts and ends at the same link.
			 * This should be prevented by the replanning framework.
			 */
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		this.recentlyDepartingDrivers.put(event.getPersonId(), event.getFacilityId());
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.searchingAgents.remove(event.getPersonId());
		this.linkEnteredAgents.remove(event.getPersonId());
		this.nextActivityFacilityMap.remove(event.getPersonId());
		this.selectedParkingsMap.remove(event.getPersonId());
		
		boolean wasCarTrip = this.carLegAgents.remove(event.getPersonId());
		if (wasCarTrip) this.recentlyArrivedDrivers.add(event.getPersonId());

	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		
		boolean wasCarTrip = this.recentlyArrivedDrivers.remove(event.getPersonId());
		if (wasCarTrip) {
			Id vehicleId = event.getPersonId(); // so far, this is true...
			boolean parked = this.parkingInfrastructure.parkVehicle(vehicleId, event.getFacilityId());
			
			/*
			 * If parked is false, the agent waits for a parking spot to become available.
			 * For those agents, the end time of the parking activity is set to Double.MAX_VALUE
			 */
			if (!parked) {
				this.recentlyWaitingDrivers.add(vehicleId);
			}
		}
	}
	
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (carLegAgents.contains(event.getPersonId())) {
			if (!searchingAgents.contains(event.getPersonId())) {
				Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
				ActivityFacility facility = nextActivityFacilityMap.get(event.getPersonId());
				double distanceToNextActivity = CoordUtils.calcDistance(facility.getCoord(), coord);
				
				if (shouldStartSearchParking(event.getLinkId(), facility.getLinkId(), distanceToNextActivity)) {
					searchingAgents.add(event.getPersonId());
					linkEnteredAgents.add(event.getPersonId());
				}
			}
			// the agent is already searching: update its position
			else {
				linkEnteredAgents.add(event.getPersonId());
			}
		}
	}
	
	/*
	 * The currentPlanElement is a car leg, which is followed by a
	 * parking activity and a walking leg to the next non-parking
	 * activity.
	 */
	private Activity getNextNonParkingActivity(Id agentId) {

		MobsimAgent agent = this.mobsimDataProvider.getAgent(agentId);
		Plan executedPlan = ((PlanAgent) agent).getCurrentPlan();
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		
		Activity nextNonParkingActivity = (Activity) executedPlan.getPlanElements().get(planElementIndex + 3);		
		return nextNonParkingActivity;
	}
	
	/*
	 * If the agent is within the parking radius or if the agent enters the link 
	 * where its next non-parking activity is performed.
	 */
	private boolean shouldStartSearchParking(Id currentLinkId, Id nextActivityLinkId, double distanceToNextActivity) {
		return distanceToNextActivity <= distance || nextActivityLinkId.equals(currentLinkId);
	}

	@Override
	public void reset(int iteration) {
		this.carLegAgents.clear();
		this.searchingAgents.clear();
		this.linkEnteredAgents.clear();
		this.selectedParkingsMap.clear();
		this.nextActivityFacilityMap.clear();
		this.lastTimeStepsLinkEnteredAgents.clear();
		this.recentlyArrivedDrivers.clear();
		this.recentlyDepartingDrivers.clear();		
	}

	/*
	 * MobsimEngine Methods
	 */
	@Override
	public void doSimStep(double time) {
		// nothing to do here...
	}

	@Override
	public void onPrepareSim() {
		// nothing to do here...
	}

	@Override
	public void afterSim() {
		// nothing to do here...
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalIterface = internalInterface;
	}

}