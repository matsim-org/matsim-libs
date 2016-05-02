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

package playground.wrashid.parkingSearch.withinday;

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
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
// events werden während sim step geschmissen, aftermobsimstep kommt nachher.
public class ParkingAgentsTracker implements LinkEnterEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler,
		MobsimInitializedListener, MobsimAfterSimStepListener, VehicleEntersTrafficEventHandler, 
		VehicleLeavesTrafficEventHandler {
	
	private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler() ;

	private final Scenario scenario;
	private final double distance;

	private final Set<Id> carLegAgents;
	private final Set<Id> searchingAgents;
	private final Set<Id> linkEnteredAgents;
	private final Set<Id> lastTimeStepsLinkEnteredAgents;
	private final Map<Id, ActivityFacility> nextActivityFacilityMap;
	private final Map<Id, PersonDriverAgentImpl> agents;
	private final Map<Id, Id> selectedParkingsMap;

	/**
	 * Tracks agents' car legs and check whether they have to start their
	 * parking search.
	 * 
	 * @param scenario
	 * @param distance
	 *            defines in which distance to the destination of a car trip an
	 *            agent starts its parking search
	 */
	public ParkingAgentsTracker(Scenario scenario, double distance) {
		this.scenario = scenario;
		this.distance = distance;

		this.carLegAgents = new HashSet<Id>();
		this.linkEnteredAgents = new HashSet<Id>();
		this.selectedParkingsMap = new HashMap<Id, Id>();
		this.lastTimeStepsLinkEnteredAgents = new TreeSet<Id>(); // This set has
																	// to be be
																	// deterministic!
		this.searchingAgents = new HashSet<Id>();
		this.nextActivityFacilityMap = new HashMap<Id, ActivityFacility>();
		this.agents = new HashMap<Id, PersonDriverAgentImpl>();
	}

	public Set<Id> getSearchingAgents() {
		return Collections.unmodifiableSet(this.searchingAgents);
	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		for (MobsimAgent agent : ((QSim) e.getQueueSimulation()).getAgents()) {
			this.agents.put(agent.getId(), (PersonDriverAgentImpl) agent);
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
		if (event.getLegMode().equals(TransportMode.car)) {
			this.carLegAgents.add(event.getPersonId());

			PersonDriverAgentImpl agent = this.agents.get(event.getPersonId());
			Plan executedPlan = agent.getCurrentPlan();
			int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent) ;

			/*
			 * Get the coordinate of the next non-parking activity's facility.
			 * The currentPlanElement is a car leg, which is followed by a
			 * parking activity and a walking leg to the next non-parking
			 * activity.
			 */
			Activity nextNonParkingActivity = (Activity) executedPlan.getPlanElements().get(planElementIndex + 3);
			ActivityFacility facility = scenario.getActivityFacilities().getFacilities()
					.get(nextNonParkingActivity.getFacilityId());
			nextActivityFacilityMap.put(event.getPersonId(), facility);

			Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
			double distanceToNextActivity = CoordUtils.calcEuclideanDistance(facility.getCoord(), coord);

			/*
			 * If the agent is within distance 'd' to target activity or OR If the
			 * agent enters the link where its next non-parking activity is
			 * performed, mark him ash searching Agent.
			 * 
			 * (this is actually handling a special case, where already at departure time
			 * the agent is within distance 'd' of next activity).
			 */
			if (shouldStartSearchParking(event.getLinkId(), facility.getLinkId(), distanceToNextActivity)) {
				searchingAgents.add(event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.carLegAgents.remove(event.getPersonId());
		this.searchingAgents.remove(event.getPersonId());
		this.linkEnteredAgents.remove(event.getPersonId());
		this.selectedParkingsMap.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Person> driverId = delegate.getDriverOfVehicle( event.getVehicleId() ) ;
		
		if (carLegAgents.contains(driverId)) {
			if (!searchingAgents.contains(driverId)) {
				Coord coord = scenario.getNetwork().getLinks().get(event.getLinkId()).getCoord();
				ActivityFacility facility = nextActivityFacilityMap.get(driverId);
				double distanceToNextActivity = CoordUtils.calcEuclideanDistance(facility.getCoord(), coord);

				/*
				 * If the agent is within the parking radius
				 */
				/*
				 * If the agent enters the link where its next non-parking
				 * activity is performed.
				 */
				
				if (shouldStartSearchParking(event.getLinkId(), facility.getLinkId(), distanceToNextActivity)) {
					searchingAgents.add(driverId);
					linkEnteredAgents.add(driverId);
				}
			}
			// the agent is already searching: update its position
			else {
				linkEnteredAgents.add(driverId);
			}
		}
	}

	private boolean shouldStartSearchParking(Id currentLinkId, Id nextActivityLinkId, double distanceToNextActivity) {
		return distanceToNextActivity <= distance || nextActivityLinkId.equals(currentLinkId);
	}

	@Override
	public void reset(int iteration) {
		delegate.reset(iteration);
		agents.clear();
		carLegAgents.clear();
		searchingAgents.clear();
		linkEnteredAgents.clear();
		selectedParkingsMap.clear();
		nextActivityFacilityMap.clear();
		lastTimeStepsLinkEnteredAgents.clear();
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.delegate.handleEvent(event);
	}

}
