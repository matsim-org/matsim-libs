/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdDepartureManager.java
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

package playground.christoph.evacuation.mobsim;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.households.Household;
import org.matsim.households.Households;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.mobsim.Tracker.Position;
import playground.christoph.evacuation.mobsim.decisiondata.DecisionDataProvider;
import playground.christoph.evacuation.mobsim.decisiondata.HouseholdDecisionData;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.EvacuationDecision;
import playground.christoph.evacuation.mobsim.decisionmodel.EvacuationDecisionModel.Participating;
import playground.christoph.evacuation.withinday.replanning.identifiers.JoinedHouseholdsIdentifier;

public class HouseholdDepartureManager implements MobsimBeforeSimStepListener, MobsimAfterSimStepListener {

	private static final Logger log = Logger.getLogger(JoinedHouseholdsIdentifier.class);
	
	private final Households households;
	private final ActivityFacilities facilities;
	private final CoordAnalyzer coordAnalyzer;
	private final HouseholdsTracker householdsTracker;
	private final InformedHouseholdsTracker informedHouseholdsTracker;
	private final DecisionDataProvider decisionDataProvider;
	
	private final JoinedHouseholdsContext joinedHouseholdsContext;
	private final Map<Id, HouseholdDeparture> scheduledHouseholdDepartures;
	private final Queue<HouseholdDeparture> plannedDeparturesQueue;
	private final Queue<HouseholdDeparture> handledDeparturesQueue;
	
	private Set<Id> householdsToHandle;	// households to handle in the current time step
	
	public HouseholdDepartureManager(Scenario scenario, CoordAnalyzer coordAnalyzer, HouseholdsTracker householdsTracker, 
			InformedHouseholdsTracker informedHouseholdsTracker, DecisionDataProvider decisionDataProvider) {
		this.households = ((ScenarioImpl) scenario).getHouseholds();
		this.facilities = scenario.getActivityFacilities();
		this.coordAnalyzer = coordAnalyzer;
		this.householdsTracker = householdsTracker;
		this.informedHouseholdsTracker = informedHouseholdsTracker;
		this.decisionDataProvider = decisionDataProvider;
		
		this.joinedHouseholdsContext = new JoinedHouseholdsContext();
		this.scheduledHouseholdDepartures = new ConcurrentHashMap<Id, HouseholdDeparture>();
		this.plannedDeparturesQueue = new PriorityBlockingQueue<HouseholdDeparture>();
		this.handledDeparturesQueue = new PriorityBlockingQueue<HouseholdDeparture>();
	}

	public JoinedHouseholdsContext getJoinedHouseholdsContext() {
		return this.joinedHouseholdsContext;
	}
	
	public Map<Id, HouseholdDeparture> getScheduledHouseholdDepartures() {
		return this.scheduledHouseholdDepartures;
	}
	
	public HouseholdDeparture peekPlannedDepartureFromQueue() {
		return this.plannedDeparturesQueue.peek();
	}

	public HouseholdDeparture pollPlannedDepartureFromQueue() {
		return this.plannedDeparturesQueue.poll();
	}
	
	public void addHandledDepartureToQueue(HouseholdDeparture householdDeparture) {
		this.handledDeparturesQueue.add(householdDeparture);
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		
		double time = e.getSimulationTime();
		
		// if the evacuation has not started yet
		if (EvacuationConfig.evacuationTime > time) return;
		
		this.householdsToHandle = this.collectHouseholdsToHandle(time);
		this.handleHouseholds(householdsToHandle, time);
	}
	

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		double time = e.getSimulationTime();
		
		// if the evacuation has not started yet
		if (EvacuationConfig.evacuationTime > time) return;
				
		this.checkDepartureQueues(time);
		
		this.updateHouseholds(householdsToHandle, time);
		
		this.householdsToHandle = null;
	}
	
	private Set<Id> collectHouseholdsToHandle(double time) {
		
		Set<Id> householdsToHandle = new LinkedHashSet<Id>();
		
		// have all households already been informed?
		boolean allHouseholdsInformed = this.informedHouseholdsTracker.allHouseholdsInformed();

		/*
		 * If already all households have been informed, we only have to handle
		 * those households which have joined recently. Otherwise, we have to check
		 * for each recently joined household whether it is already informed.
		 * 
		 * The second part of the if statement is a special case.
		 * If the last households have been informed in time step i, then in time step i + 1
		 * recently joined AND recently informed households have to be taken into account.  
		 */
		if (allHouseholdsInformed && this.informedHouseholdsTracker.getAllHouseholdsInformedTime() + 1.0 < time) {
			householdsToHandle.addAll(this.householdsTracker.getHouseholdsJoinedInLastTimeStep());
		} else {			
			// check all recently informed households
			Set<Id> recentlyInformedHouseholds = this.informedHouseholdsTracker.getHouseholdsInformedInLastTimeStep();
			for (Id householdId : recentlyInformedHouseholds) {	
				if (this.householdsTracker.getJoinedHouseholds().contains(householdId)) {
					householdsToHandle.add(householdId);
				}
			}
			
			// check all recently joined households
			Set<Id> recentlyJoinedHouseholds = this.householdsTracker.getHouseholdsJoinedInLastTimeStep();
			for (Id householdId : recentlyJoinedHouseholds) {
				if (this.informedHouseholdsTracker.isHouseholdInformed(householdId)) householdsToHandle.add(householdId);
			}			
		}

		/*
		 * Some households might contain only a single member or travel together when being informed.
		 * When they reach their meeting point, they have to be handled as well. 
		 */
		Set<Id> updatedHouseholds = this.householdsTracker.getHouseholdsUpdatedInLastTimeStep();
		for (Id householdId : updatedHouseholds) {
			// skip not informed households
			if (!this.informedHouseholdsTracker.isHouseholdInformed(householdId)) continue;
			
			// skip not joined households
			if (!this.householdsTracker.getJoinedHouseholds().contains(householdId)) continue;
			
			// skip households not located in a facility
			HouseholdPosition householdPosition = this.householdsTracker.getHouseholdPosition(householdId);
			if (!householdPosition.getPositionType().equals(Position.FACILITY)) continue;
			
			// skip households not located at their meeting facility
			HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
			if (!householdPosition.getPositionId().equals(hdd.getMeetingPointFacilityId())) continue;
			
			// Seems like we have to handle the household. Since its a set, we do not create duplicates.
			householdsToHandle.add(householdId);
		}
		
		return householdsToHandle;
	}

	/*
	 * Handle all households which have recently been informed and are already joined or
	 * were already informed and have just joined.
	 */
	private void handleHouseholds(Set<Id> householdsToHandle, double time) {

		for (Id householdId : householdsToHandle) {
			HouseholdDecisionData hdd = this.decisionDataProvider.getHouseholdDecisionData(householdId);
			HouseholdPosition householdPosition = hdd.getHouseholdPosition();
			
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
						this.scheduledHouseholdDepartures.put(householdId, householdDeparture);
						this.plannedDeparturesQueue.add(householdDeparture);
					}
				} else {
					/*
					 * How to handle agents which are at another location? They will probably still travel
					 * together. However, this should probably be handled in the initial replanner.
					 */
				}
			}
		}
	}
	
	/*
	 * Check whether a household has missed its departure.
	 */
	private void checkDepartureQueues(double time) {
		
		HouseholdDeparture householdDeparture = null;
		while ((householdDeparture = this.plannedDeparturesQueue.peek()) != null) {
			
			if (householdDeparture.departureTime < time) {
				log.warn("Household " + householdDeparture.householdId + " missed its departure time!" + 
						" Simulation time: " + time +
						", expected departure time: " + householdDeparture.departureTime);
				this.plannedDeparturesQueue.poll();
				this.scheduledHouseholdDepartures.remove(householdDeparture.householdId);
			} else break;
		}
		
		while ((householdDeparture = this.handledDeparturesQueue.peek()) != null) {
			
			// departed as planned - remove entry from scheduledHouseholdDepartures map
			if (householdDeparture.departureTime == time) {
				this.handledDeparturesQueue.poll();
				this.scheduledHouseholdDepartures.remove(householdDeparture.householdId);
				
				// check number of departed household members
				if (householdDeparture.getExpectedDepartures() != householdDeparture.getPerformedDepartures()) {
					log.warn("Number of expected departing people (" + householdDeparture.getExpectedDepartures() +
							") does not match number of departed people (" + householdDeparture.getPerformedDepartures() +
							") for household " + householdDeparture.householdId + "!");
				}
			} else if (householdDeparture.departureTime > time) {
				log.warn("Household " + householdDeparture.householdId + " should not have departed yet!" + 
						" Simulation time: " + time +
						", expected departure time: " + householdDeparture.departureTime);
				this.handledDeparturesQueue.poll();
				this.scheduledHouseholdDepartures.remove(householdDeparture.householdId);

				if (householdDeparture.getExpectedDepartures() != householdDeparture.getPerformedDepartures()) {
					log.warn("Number of expected departing people (" + householdDeparture.getExpectedDepartures() +
							") does not match number of departed people (" + householdDeparture.getPerformedDepartures() +
							") for household " + householdDeparture.householdId + "!");
				}
			} else break;
		}
	}
	
	// TODO: does this stuff make sense?
	private void updateHouseholds(Set<Id> handledHouseholds, double time) {
		
		// get all households which have been updated in the last time step
		Set<Id> updatedHouseholds = this.householdsTracker.getHouseholdsUpdatedInLastTimeStep();

		for (Id householdId : updatedHouseholds) {
			
			// ignore households which have been handled
			if (handledHouseholds.contains(householdId)) continue;
			
//			// ignore households which have joined in the last time step
//			if (this.householdsTracker.getHouseholdsJoinedInLastTimeStep().contains(householdId)) continue;
			
			HouseholdDeparture scheduledDeparture = this.scheduledHouseholdDepartures.get(householdId);
			if (scheduledDeparture != null) {
//				throw new RuntimeException("State of household " + householdId.toString() + " has changed at " +
//						"time " + time + ". This seems to be unwanted behaviour since there is a scheduled household " +
//						"departure (scheduled departure time " + scheduledDeparture.departureTime +
//						") which has not been performed. Aborting.");
				log.warn("State of household " + householdId.toString() + " has changed at " +
						"time " + time + ". This seems to be unwanted behaviour since there is a scheduled household " +
						"departure (scheduled departure time " + scheduledDeparture.departureTime +
						") which has not been performed.");
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
		double departureDelay = hdd.getDepartureTimeDelay();
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
		Household household = this.households.getHouseholds().get(hdd.getHouseholdId());
		HouseholdDeparture householdDeparture = new HouseholdDeparture(householdId, facilityId, departureTime + 1, 
				household.getMemberIds().size());
		
		return householdDeparture;
	}
	
	public static class JoinedHouseholdsContext {
		/*
		 * Maps to store information for the replanner.
		 * Where does the household meet? Which transport mode does
		 * an agent use? Which agents are drivers?
		 */
		private final Map<Id, Id> householdMeetingPointMapping;
		private final Map<Id, String> transportModeMapping;
		private final Map<Id, Id> driverVehicleMapping;
		
		public JoinedHouseholdsContext() {
			this.householdMeetingPointMapping = new ConcurrentHashMap<Id, Id>();
			this.transportModeMapping = new ConcurrentHashMap<Id, String>();
			this.driverVehicleMapping = new ConcurrentHashMap<Id, Id>();
		}
		
		/**
		 * @return The mapping between a household and the meeting point that should be used.
		 */
		public Map<Id, Id> getHouseholdMeetingPointMap() {
			return householdMeetingPointMapping;
		}

		/**
		 * @return The mapping between an agent and the transportMode that should be used.
		 */
		public Map<Id, String> getTransportModeMap() {
			return transportModeMapping;
		}

		/**
		 * @return The mapping between an agent and the vehicle that should be used.
		 */
		public Map<Id, Id> getDriverVehicleMap() {
			return driverVehicleMapping;
		}

		public void reset() {
			this.householdMeetingPointMapping.clear();
			this.transportModeMapping.clear();
			this.driverVehicleMapping.clear();
		}
	}
	
	/*
	 * A data structure to store households and their planned departures.
	 */
	public static class HouseholdDeparture implements Comparable<HouseholdDeparture> {
		
		private final Id householdId;
		private final Id facilityId;
		private final double departureTime;
		private final int expectedDepartures;
		private AtomicInteger performedDepartures = new AtomicInteger(0);
		
		public HouseholdDeparture(Id householdId, Id facilityId, double departureTime, int expectedDepartures) {
			this.householdId = householdId;
			this.facilityId = facilityId;
			this.departureTime = departureTime;
			this.expectedDepartures = expectedDepartures;
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

		/*
		 *  Number of agents involved in the HouseholdDeparture. So far, this is
		 *  the number of household members.
		 */
		public int getExpectedDepartures() {
			return this.expectedDepartures;
		}
		
		public void incPerformedDepartures() {
			this.performedDepartures.incrementAndGet();
		}
		
		public int getPerformedDepartures() {
			return this.performedDepartures.get();
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof HouseholdDeparture) {
				return ((HouseholdDeparture) o).getHouseholdId().equals(householdId);
			}
			return false;
		}

		@Override
		public int compareTo(HouseholdDeparture other) {
			int cmp = Double.compare(this.departureTime, other.departureTime);
			if (cmp == 0) return this.householdId.compareTo(other.householdId);
			else return cmp;
		}
	}

}