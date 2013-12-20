/* *********************************************************************** *
 * project: org.matsim.*
 * JoinedHouseholdsIdentifier.java
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

package playground.christoph.evacuation.withinday.replanning.identifiers;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

import playground.christoph.evacuation.mobsim.HouseholdDepartureManager;
import playground.christoph.evacuation.mobsim.HouseholdDepartureManager.HouseholdDeparture;
import playground.christoph.evacuation.mobsim.HouseholdDepartureManager.JoinedHouseholdsContext;
import playground.christoph.evacuation.withinday.replanning.utils.HouseholdModeAssignment;
import playground.christoph.evacuation.withinday.replanning.utils.ModeAvailabilityChecker;
import playground.christoph.evacuation.withinday.replanning.utils.SelectHouseholdMeetingPoint;

/**
 *  Define which households will relocate to another (secure!) location
 *  at which time.
 *  
 *  Moreover it is decided which transport mode will be used for the evacuation.
 *  If a car is available, it is used. Otherwise the people will walk.
 *  
 *  @author cdobler
 */
public class JoinedHouseholdsIdentifier extends DuringActivityIdentifier {

	private static final Logger log = Logger.getLogger(JoinedHouseholdsIdentifier.class);
	
	private final Households households;
	private final ActivityFacilities facilities;
	private final SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	private final MobsimDataProvider mobsimDataProvider;
	private final HouseholdDepartureManager householdDepartureManager;
	private final JoinedHouseholdsContext joinedHouseholdsContext;
	
	public JoinedHouseholdsIdentifier(Scenario scenario, SelectHouseholdMeetingPoint selectHouseholdMeetingPoint,
			ModeAvailabilityChecker modeAvailabilityChecker, JointDepartureOrganizer jointDepartureOrganizer,
			MobsimDataProvider mobsimDataProvider, HouseholdDepartureManager householdDepartureManager) {
		this.households = ((ScenarioImpl) scenario).getHouseholds();
		this.facilities = scenario.getActivityFacilities();
		this.selectHouseholdMeetingPoint = selectHouseholdMeetingPoint;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
		this.mobsimDataProvider = mobsimDataProvider;
		this.householdDepartureManager = householdDepartureManager;
		this.joinedHouseholdsContext = householdDepartureManager.getJoinedHouseholdsContext();
	}

	@Override
	public Set<MobsimAgent> getAgentsToReplan(double time) {
		
		this.householdDepartureManager.handleHouseholdsStuff(time);
		
		/*
		 * Clear maps for every time step.
		 */
		this.joinedHouseholdsContext.reset();
	
		Set<Id> agentIds = new HashSet<Id>();
		
		HouseholdDeparture householdDeparture = null;
		Queue<HouseholdDeparture> plannedDeparturesQueue = this.householdDepartureManager.getPlannedDeparturesQueue();
		Queue<HouseholdDeparture> handledDeparturesQueue = this.householdDepartureManager.getHandledDeparturesQueue();
		
		while ((householdDeparture = plannedDeparturesQueue.peek()) != null) {
//		while(true) {
//			householdDeparture = plannedDeparturesQueue.peek();
			Id householdId = householdDeparture.getHouseholdId();
			
			/*
			 * If the household departs in the current time step.
			 */
			if (householdDeparture.getDepartureTime() == time) {
				
				Id facilityId = householdDeparture.getFacilityId();
				Id linkId = this.facilities.getFacilities().get(facilityId).getLinkId();
				Id meetingPointId = selectHouseholdMeetingPoint.selectNextMeetingPoint(householdId);
				this.joinedHouseholdsContext.getHouseholdMeetingPointMap().put(householdId, meetingPointId);
				Household household = households.getHouseholds().get(householdId);

				/*
				 * Store mapping which is read by the replanners
				 */
				HouseholdModeAssignment assignment = modeAvailabilityChecker.getHouseholdModeAssignment(household, facilityId);	
				this.joinedHouseholdsContext.getDriverVehicleMap().putAll(assignment.getDriverVehicleMap());
				this.joinedHouseholdsContext.getTransportModeMap().putAll(assignment.getTransportModeMap());
				
				/*
				 * Create and add joint departures in the departure handler for the mobsim.
				 */
				createJointDepartures(assignment, linkId);
				
				// finally add agents to replanning set
				agentIds.addAll(household.getMemberIds());
				
				// remove entry from the queue ...
				plannedDeparturesQueue.poll();
				
				// ... and add it to the handledDeparturesQueue which is used for a consistency check
				handledDeparturesQueue.add(householdDeparture);
			} else break;
		}

		// apply filter to remove agents that should not be replanned
		this.applyFilters(agentIds, time);

		Set<MobsimAgent> agentsToReplan = new TreeSet<MobsimAgent>(new PersonAgentComparator());
		for (Id agentId : agentIds) agentsToReplan.add(this.mobsimDataProvider.getAgent(agentId));
		
		return agentsToReplan;
	}

	private void createJointDepartures(HouseholdModeAssignment assignment, Id linkId) {
		Set<Id> driverIds = assignment.getDriverVehicleMap().keySet();
		
		for (Id driverId : driverIds) {
			Id vehicleId = assignment.getDriverVehicleMap().get(driverId);
			Set<Id> passengerIds = new LinkedHashSet<Id>();
			for (Entry<Id, Id> entry : assignment.getPassengerVehicleMap().entrySet()) {
				if (entry.getValue().equals(vehicleId)) {
					passengerIds.add(entry.getKey());
				}
			}
			
			this.jointDepartureOrganizer.createJointDeparture(linkId, vehicleId, driverId, passengerIds);			
		}
	}
	
	public void incPerformedDepartures(Id householdId) {
		this.householdDepartureManager.getScheduledHouseholdDepartures().get(householdId).incPerformedDepartures();
	}
	
	/**
	 * @return The mapping between a household and the meeting point that should be used.
	 */
	@Deprecated // use joinedHouseholdsContext instead
	public Map<Id, Id> getHouseholdMeetingPointMapping() {
		return this.joinedHouseholdsContext.getHouseholdMeetingPointMap();
	}
	
	/**
	 * @return The mapping between an agent and the transportMode that should be used.
	 */
	@Deprecated // use joinedHouseholdsContext instead
	public Map<Id, String> getTransportModeMapping() {
		return this.joinedHouseholdsContext.getTransportModeMap();
	}
	
	/**
	 * @return The mapping between an agent and the vehicle that should be used.
	 */
	@Deprecated // use joinedHouseholdsContext instead
	public Map<Id, Id> getDriverVehicleMapping() {
		return this.joinedHouseholdsContext.getDriverVehicleMap();
	}

//	@Override
//	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
//		
//		if (e.getSimulationTime() == EvacuationConfig.evacuationTime) {
//			this.scheduledHouseholdDepartures.clear();
//		} else if (e.getSimulationTime() > EvacuationConfig.evacuationTime) {
//			
//			/*
//			 * Get a Set of Ids of households which have been informed in the current time step.
//			 * Define their departure time, if they are joined.
//			 */
//			this.initiallyCollectHouseholds(e.getSimulationTime());
//			
//			/*
//			 * Get a Set of Ids of households which might have changed their state
//			 * in the current time step.
//			 */
//			Set<Id> householdsToUpdate = this.householdsTracker.getHouseholdsToUpdate();
//			this.updateHouseholds(householdsToUpdate, e.getSimulationTime());
//			
//			/*
//			 * Check whether a household has missed its departure.
//			 */
//			Iterator<Entry<Id, HouseholdDeparture>> iter = this.scheduledHouseholdDepartures.entrySet().iterator();
//			while (iter.hasNext()) {
//				Entry<Id, HouseholdDeparture> entry = iter.next();
//				Id householdId = entry.getKey();
//				HouseholdDeparture householdDeparture = entry.getValue();
//				if (householdDeparture.departureTime < e.getSimulationTime()) {
//					log.warn("Household missed its departure time! Id " + householdId + ". Simulation time: " + e.getSimulationTime() +
//							", expected departure time: " + householdDeparture.departureTime);
//					iter.remove();
//				}
//			}
//		}
//	}
	
//	private void updateHouseholds(Set<Id> householdsToUpdate, double time) {
//		
//		for (Id householdId : householdsToUpdate) {
//			/*
//			 * Ignore households which are not informed so far.
//			 */
//			if (!informedHouseholdsTracker.isHouseholdInformed(householdId)) continue;
//			
//			HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
//			HouseholdPosition householdPosition = hdd.getHouseholdPosition();
//			HouseholdDeparture householdDeparture = this.scheduledHouseholdDepartures.get(householdId);
//			
//			/*
//			 * Check whether the household is joined.
//			 */
//			boolean isJoined = this.decisionDataProvider.getHouseholdDecisionData(householdId).getHouseholdPosition().isHouseholdJoined();
//			boolean wasJoined = (householdDeparture != null);
//			if (isJoined) {
//				/*
//				 * Check whether the household is in a facility.
//				 */
//				Position positionType = householdPosition.getPositionType();
//				if (positionType == Position.FACILITY) {
//					Id facilityId = householdPosition.getPositionId();
//					Id meetingPointId = this.decisionDataProvider.getHouseholdDecisionData(householdId).getMeetingPointFacilityId();
//					
//					/*
//					 * Check whether the household is at its meeting facility.
//					 */
//					if (meetingPointId.equals(facilityId)) {
//						
//						/*
//						 * The household is at its meeting point. If no departure has been scheduled so far, 
//						 * the household evacuates and the facility is not secure, schedule one.
//						 */
//						if (householdDeparture == null) {
//							
//							ActivityFacility facility = this.facilities.getFacilities().get(facilityId);
//							boolean facilityIsSecure = !this.coordAnalyzer.isFacilityAffected(facility);
//							
//							boolean householdParticipates;
//							Participating participating = this.decisionDataProvider.getHouseholdDecisionData(householdId).getParticipating();
//							if (participating == Participating.TRUE) householdParticipates = true;
//							else if (participating == Participating.FALSE) householdParticipates = false;
//							else throw new RuntimeException("Households participation state is undefined: " + householdId.toString());
//							
//							if (!facilityIsSecure && householdParticipates) {
//								// ... and schedule the household's departure.
//								householdDeparture = createHouseholdDeparture(time, hdd, meetingPointId);
//								this.scheduledHouseholdDepartures.put(householdId, householdDeparture);
//							}
//						}
//					} 
//					
//					/*
//					 * The household is joined at a facility which is not its meeting facility. Ensure that no
//					 * departure is scheduled and create a warn message if the evacuation has already started. 
//					 * 
//					 * TODO: check whether this could be a valid state.
//					 */
//					else {
//						this.scheduledHouseholdDepartures.remove(householdId);
//						if (time > EvacuationConfig.evacuationTime && !facilityId.toString().contains("pickup")) {
//							log.warn("Household is joined at a facility which is not its meeting facility. Id: " + householdId);							
//						}
//					}
//				}
//				
//				/*
//				 * The household is joined but not at a facility. Therefore ensure
//				 * that there is no departure scheduled.
//				 */
//				else {				
//					this.scheduledHouseholdDepartures.remove(householdId);
//				}
//			}
//			
//			/*
//			 * The household is not joined. Therefore ensure that there is no departure
//			 * scheduled for for that household.
//			 */
//			else {
//				this.scheduledHouseholdDepartures.remove(householdId);
//				
//				/*
//				 * If the household was joined and the evacuation has already started.
//				 * We do not expect to find a departure before it was scheduled.
//				 */
//				if (wasJoined && time < householdDeparture.departureTime) {
//					log.warn("Household has left its meeting point before scheduled departure. Id " + householdId);
//				}
//			}
//		}
//	}
		
}