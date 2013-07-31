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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.mobsim.qsim.comparators.PersonAgentComparator;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.HouseholdPosition;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.InformedHouseholdsTracker;
import playground.christoph.evacuation.mobsim.Tracker.Position;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.EvacuationDecision;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;
import playground.christoph.evacuation.utils.DeterministicRNG;
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
public class JoinedHouseholdsIdentifier extends DuringActivityIdentifier implements 
		MobsimInitializedListener, MobsimAfterSimStepListener {

	private static final Logger log = Logger.getLogger(JoinedHouseholdsIdentifier.class);
	
	private final Households households;
	private final ActivityFacilities facilities;
	private final CoordAnalyzer coordAnalyzer;
	private final SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final VehiclesTracker vehiclesTracker;
	private final HouseholdsTracker householdsTracker;
	private final InformedHouseholdsTracker informedHouseholdsTracker;
	private final DecisionDataProvider decisionDataProvider;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	
	private final DeterministicRNG rng;
	private final Map<Id, HouseholdDeparture> householdDepartures;
	private final Map<Id, PlanBasedWithinDayAgent> agentMapping;
	
	/*
	 * Maps to store information for the replanner.
	 * Where does the household meet? Which transport mode does
	 * an agent use? Which agents are drivers?
	 */
	private final Map<Id, Id> householdMeetingPointMapping;
	private final Map<Id, String> transportModeMapping;
	private final Map<Id, Id> driverVehicleMapping;
	
	public JoinedHouseholdsIdentifier(Scenario scenario, SelectHouseholdMeetingPoint selectHouseholdMeetingPoint,
			CoordAnalyzer coordAnalyzer, VehiclesTracker vehiclesTracker, HouseholdsTracker householdsTracker,
			InformedHouseholdsTracker informedHouseholdsTracker, ModeAvailabilityChecker modeAvailabilityChecker,
			DecisionDataProvider decisionDataProvider, JointDepartureOrganizer jointDepartureOrganizer) {
		this.households = ((ScenarioImpl) scenario).getHouseholds();
		this.facilities = ((ScenarioImpl) scenario).getActivityFacilities();
		this.selectHouseholdMeetingPoint = selectHouseholdMeetingPoint;
		this.coordAnalyzer = coordAnalyzer;
		this.vehiclesTracker = vehiclesTracker;
		this.householdsTracker = householdsTracker;
		this.informedHouseholdsTracker = informedHouseholdsTracker;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.decisionDataProvider = decisionDataProvider;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
		
		this.rng = new DeterministicRNG(123654);
		this.agentMapping = new HashMap<Id, PlanBasedWithinDayAgent>();
		this.householdMeetingPointMapping = new ConcurrentHashMap<Id, Id>();
		this.transportModeMapping = new ConcurrentHashMap<Id, String>();
		this.driverVehicleMapping = new ConcurrentHashMap<Id, Id>();
		this.householdDepartures = new ConcurrentHashMap<Id, HouseholdDeparture>();
	}

	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		
		/*
		 * Clear maps for every time step.
		 */
		this.householdMeetingPointMapping.clear();
		this.transportModeMapping.clear();
		this.driverVehicleMapping.clear();
	
		Set<Id> agentIds = new HashSet<Id>();
		
		Iterator<Entry<Id, HouseholdDeparture>> iter = this.householdDepartures.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<Id, HouseholdDeparture> entry = iter.next();
			Id householdId = entry.getKey();
			HouseholdDeparture householdDeparture = entry.getValue();
			
			/*
			 * If the household departs in the current time step.
			 */
			if (householdDeparture.getDepartureTime() == time) {
				
				Id facilityId = householdDeparture.getFacilityId();
				Id linkId = this.facilities.getFacilities().get(facilityId).getLinkId();
				Id meetingPointId = selectHouseholdMeetingPoint.selectNextMeetingPoint(householdId);
				householdMeetingPointMapping.put(householdId, meetingPointId);
				Household household = households.getHouseholds().get(householdId);

				/*
				 * Store mapping which is read by the replanners
				 */
				HouseholdModeAssignment assignment = modeAvailabilityChecker.getHouseholdModeAssignment(household, facilityId);	
				driverVehicleMapping.putAll(assignment.getDriverVehicleMap());
				transportModeMapping.putAll(assignment.getTransportModeMap());
				
				/*
				 * Create and add joint departures in the departure handler for the mobsim.
				 */
				createJointDepartures(assignment, linkId);
				
				// finally add agents to replanning set
				agentIds.addAll(household.getMemberIds());
			}
		}

		// apply filter to remove agents that should not be replanned
		this.applyFilters(agentIds, time);

		Set<PlanBasedWithinDayAgent> agentsToReplan = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
		for (Id agentId : agentIds) agentsToReplan.add(agentMapping.get(agentId));
		
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
	
	/**
	 * @return The mapping between a household and the meeting point that should be used.
	 */
	public Map<Id, Id> getHouseholdMeetingPointMapping() {
		return this.householdMeetingPointMapping;
	}
	
	/**
	 * @return The mapping between an agent and the transportMode that should be used.
	 */
	public Map<Id, String> getTransportModeMapping() {
		return this.transportModeMapping;
	}
	
	/**
	 * @return The mapping between an agent and the vehicle that should be used.
	 */
	public Map<Id, Id> getDriverVehicleMapping() {
		return this.driverVehicleMapping;
	}
	
	/*
	 * Create a mapping between personIds and the agents in the mobsim.
	 * 
	 * Moreover ensure that the joinedHouseholds and householdDeparture
	 * data structures are filled properly. When the simulation starts,
	 * all households are joined at their home facility.
	 */
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		QSim sim = (QSim) e.getQueueSimulation();

		this.agentMapping.clear();
		for (MobsimAgent mobsimAgent : sim.getAgents()) {
			PlanBasedWithinDayAgent withinDayAgent = (PlanBasedWithinDayAgent) mobsimAgent;
			agentMapping.put(withinDayAgent.getId(), withinDayAgent);				
		}
	}
	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		
		if (e.getSimulationTime() == EvacuationConfig.evacuationTime) {
			this.householdDepartures.clear();
		} else if (e.getSimulationTime() > EvacuationConfig.evacuationTime) {
			
			/*
			 * Get a Set of Ids of households which have been informed in the current time step.
			 * Define their departure time, if they are joined.
			 */
			this.initiallyCollectHouseholds(e.getSimulationTime());
			
			/*
			 * Get a Set of Ids of households which might have changed their state
			 * in the current time step.
			 */
			Set<Id> householdsToUpdate = this.householdsTracker.getHouseholdsToUpdate();
			this.updateHouseholds(householdsToUpdate, e.getSimulationTime());
			
			/*
			 * Check whether a household has missed its departure.
			 */
			Iterator<Entry<Id, HouseholdDeparture>> iter = this.householdDepartures.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<Id, HouseholdDeparture> entry = iter.next();
				Id householdId = entry.getKey();
				HouseholdDeparture householdDeparture = entry.getValue();
				if (householdDeparture.departureTime < e.getSimulationTime()) {
					log.warn("Household missed its departure time! Id " + householdId + ". Simulation time: " + e.getSimulationTime() +
							", expected departure time: " + householdDeparture.departureTime);
					iter.remove();
				}
			}
		}
	}
	
	/*
	 * Collect households which have just been informed that they should (probably) evacuate.
	 */
	private void initiallyCollectHouseholds(double time) {
		
		/*
		 * Get a Set of Ids of all households which have just been informed.
		 */	
		Set<Id> informedHouseholds = this.informedHouseholdsTracker.getHouseholdsInformedInLastTimeStep();
		for (Id householdId : informedHouseholds) {
			HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
			HouseholdPosition householdPosition = hdd.getHouseholdPosition();
			
			// if the household is joined
			if (hdd.isJoined()) {
				
				// if the household is at a facility
				if (householdPosition.getPositionType() == Position.FACILITY) {
					
					//if the household is at its meeting point facility
					if (householdPosition.getPositionId().equals(hdd.getMeetingPointFacilityId())) {
						
						/*
						 * If the meeting point is not secure and the household is willing 
						 * to evacuate, schedule a departure. Otherwise ignore the household.
						 */
						Id facilityId = householdPosition.getPositionId();
						ActivityFacility facility = this.facilities.getFacilities().get(facilityId);
						boolean facilityIsSecure = !this.coordAnalyzer.isFacilityAffected(facility);
						
						boolean householdParticipates;
						Participating participating = this.decisionDataProvider.getHouseholdDecisionData(householdId).getParticipating();
						if (participating == Participating.TRUE) householdParticipates = true;
						else if (participating == Participating.FALSE) householdParticipates = false;
						else throw new RuntimeException("Households participation state is undefined: " + householdId.toString());
						
						if (!facilityIsSecure && householdParticipates) {																											
							HouseholdDeparture householdDeparture = createHouseholdDeparture(time, hdd, householdPosition.getPositionId());
							this.householdDepartures.put(householdId, householdDeparture);
							
//							/*
//							 * The initial meeting points have been selected by the SelectHouseholdMeetingPoint class.
//							 */
//							this.decisionDataProvider.getHouseholdDecisionData(householdId).getMeetingPointFacilityId();
						}
					}
				}
			}
		}
	}
	
	private void updateHouseholds(Set<Id> householdsToUpdate, double time) {
		
		for (Id householdId : householdsToUpdate) {			
			/*
			 * Ignore households which are not informed so far.
			 */
			if (!informedHouseholdsTracker.isHouseholdInformed(householdId)) continue;
			
			HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
			HouseholdPosition householdPosition = hdd.getHouseholdPosition();
			HouseholdDeparture householdDeparture = this.householdDepartures.get(householdId);
			
			/*
			 * Check whether the household is joined.
			 */
			boolean isJoined = this.decisionDataProvider.getHouseholdDecisionData(householdId).isJoined();
			boolean wasJoined = (householdDeparture != null);
			if (isJoined) {
				/*
				 * Check whether the household is in a facility.
				 */
				Position positionType = householdPosition.getPositionType();
				if (positionType == Position.FACILITY) {
					Id facilityId = householdPosition.getPositionId();
					Id meetingPointId = this.decisionDataProvider.getHouseholdDecisionData(householdId).getMeetingPointFacilityId();
					
					/*
					 * Check whether the household is at its meeting facility.
					 */
					if (meetingPointId.equals(facilityId)) {
						
						/*
						 * The household is at its meeting point. If no departure has
						 * been scheduled so far, the household evacuates and the 
						 * facility is not secure, schedule one.
						 */
						if (householdDeparture == null) {
							
							ActivityFacility facility = this.facilities.getFacilities().get(facilityId);
							boolean facilityIsSecure = !this.coordAnalyzer.isFacilityAffected(facility);
							
							boolean householdParticipates;
							Participating participating = this.decisionDataProvider.getHouseholdDecisionData(householdId).getParticipating();
							if (participating == Participating.TRUE) householdParticipates = true;
							else if (participating == Participating.FALSE) householdParticipates = false;
							else throw new RuntimeException("Households participation state is undefined: " + householdId.toString());
							
							if (!facilityIsSecure && householdParticipates) {
								// ... and schedule the household's departure.
								householdDeparture = createHouseholdDeparture(time, hdd, meetingPointId);
								this.householdDepartures.put(householdId, householdDeparture);
							}
						}
					} 
					
					/*
					 * The household is joined at a facility which is not its
					 * meeting facility. Ensure that no departure is scheduled
					 * and create a warn message if the evacuation has already
					 * started. 
					 * TODO: check whether this could be a valid state.
					 */
					else {
						this.householdDepartures.remove(householdId);
						if (time > EvacuationConfig.evacuationTime && !facilityId.toString().contains("pickup")) {
							log.warn("Household is joined at a facility which is not its meeting facility. Id: " + householdId);							
						}
					}
				}
				
				/*
				 * The household is joined but not at a facility. Therefore ensure
				 * that there is no departure scheduled.
				 */
				else {				
					this.householdDepartures.remove(householdId);
				}
			}
			
			/*
			 * The household is not joined. Therefore ensure that there is no departure
			 * scheduled for for that household.
			 */
			else {
				this.householdDepartures.remove(householdId);
				
				/*
				 * If the household was joined and the evacuation has already started.
				 * We do not expect to find a departure before it was scheduled.
				 */
				if (wasJoined && time < householdDeparture.departureTime) {
					log.warn("Household has left its meeting point before scheduled departure. Id " + householdId);
				}
			}
		}
	}
	
	/*
	 * Create a HouseholdDeparture object. The household's departure time depends on its
	 * evacuation decision (immediately vs. later).
	 * We further assume that the household requires at least a certain time to grab some
	 * basic stuff. Therefore, we add an offset to the current time based on a Rayleigh
	 * distribution.
	 */
	private HouseholdDeparture createHouseholdDeparture(double currentTime, HouseholdDecisionData hdd, Id facilityId) {
		
		Id householdId = hdd.getHouseholdId();
		EvacuationDecision evacuationDecision = hdd.getEvacuationDecision();
		double departureDelay = calculateDepartureDelay(householdId);
		double earliestDepartureTime = currentTime + departureDelay;
		
		double departureTime;
		if (evacuationDecision == EvacuationDecision.IMMEDIATELY) {
			departureTime = earliestDepartureTime;
		} else if (evacuationDecision == EvacuationDecision.LATER) {
			// TODO: re-estimate evacuate from home time?
			double evacuateFromHomeTime = hdd.getHouseholdEvacuateFromHomeTime();	// arrive at rescue facility
			double householdReturnHomeTime = hdd.getHouseholdReturnHomeTime();	// all household members meet at home
			double evacuateFromHomeTravelTime = evacuateFromHomeTime - householdReturnHomeTime;
			
			double latestLeaveTime = hdd.getLatestAcceptedLeaveTime();	// leave affected area
			double latestDepartureTime = latestLeaveTime - evacuateFromHomeTravelTime;
			
			/*
			 * The household stays as long as possible at home.
			 */
			if (latestDepartureTime > earliestDepartureTime) {
				departureTime = latestDepartureTime;
			} else 
				departureTime = earliestDepartureTime;
			
		} else throw new RuntimeException("Unexpected evacuation decision found: " + evacuationDecision.toString());
						
		/*
		 * We have to add one second here. This ensure that some code which is executed
		 * at the end of a time step is executed when the simulation has started.
		 */
		HouseholdDeparture householdDeparture = new HouseholdDeparture(householdId, facilityId, departureTime + 1);
		
		return householdDeparture;
	}
	
	/*
	 * TODO: use a function to estimate the departure time based on household characteristics and current time
	 * 
	 * So far use a Rayleigh Distribution with a sigma of 600. After 706s ~ 50% of all households have reached
	 * their departure time.
	 */
	private final double sigma = 600;
	private final double upperLimit = 0.999999;
	
	private double calculateDepartureDelay(Id householdId) {
		
		double rand = this.rng.idToRandomDouble(householdId);
		
		if (rand == 0.0) return 0.0;
		else if (rand > upperLimit) rand = upperLimit;
		
		return Math.floor(Math.sqrt(-2 * sigma*sigma * Math.log(1 - rand)));	
	}
	
	/*
	 * A datastructure to store households and their planned departures.
	 */
	private static class HouseholdDeparture {
		
		private final Id householdId;
		private final Id facilityId;
		private final double departureTime;
		
		public HouseholdDeparture(Id householdId, Id facilityId, double departureTime) {
			this.householdId = householdId;
			this.facilityId = facilityId;
			this.departureTime = departureTime;
		}
		
		public Id getHouseholdId() {
			return this.householdId;
		}
		
		public Id getFacilityId() {
			return this.facilityId;
		}
		
		public double getDepartureTime() {
			return this.departureTime;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof HouseholdDeparture) {
				return ((HouseholdDeparture) o).getHouseholdId().equals(householdId);
			}
			return false;
		}
	}
	
}