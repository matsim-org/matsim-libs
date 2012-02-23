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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

import playground.christoph.evacuation.analysis.CoordAnalyzer;
import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.core.utils.geometry.Coord3dImpl;
import playground.christoph.evacuation.mobsim.HouseholdPosition;
import playground.christoph.evacuation.mobsim.HouseholdsTracker;
import playground.christoph.evacuation.mobsim.PassengerDepartureHandler;
import playground.christoph.evacuation.mobsim.Tracker.Position;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.trafficmonitoring.WalkTravelTimeFactory;
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
		SimulationInitializedListener, SimulationAfterSimStepListener {

	private static final Logger log = Logger.getLogger(JoinedHouseholdsIdentifier.class);
	
	private final Vehicles vehicles;
	private final Households households;
	private final ActivityFacilities facilities;
	private final CoordAnalyzer coordAnalyzer;
	private final SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final VehiclesTracker vehiclesTracker;
	private final HouseholdsTracker householdsTracker;
	
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
	
	private final WalkSpeedComparator walkSpeedComparator;
	
	public JoinedHouseholdsIdentifier(Scenario scenario,
			SelectHouseholdMeetingPoint selectHouseholdMeetingPoint,
			ModeAvailabilityChecker modeAvailabilityChecker, CoordAnalyzer coordAnalyzer, 
			VehiclesTracker vehiclesTracker, HouseholdsTracker householdsTracker) {
		this.vehicles = ((ScenarioImpl) scenario).getVehicles();
		this.households = ((ScenarioImpl) scenario).getHouseholds();
		this.facilities = ((ScenarioImpl) scenario).getActivityFacilities();
		this.selectHouseholdMeetingPoint = selectHouseholdMeetingPoint;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.coordAnalyzer = coordAnalyzer;
		this.vehiclesTracker = vehiclesTracker;
		this.householdsTracker = householdsTracker;
		
		this.agentMapping = new HashMap<Id, PlanBasedWithinDayAgent>();
		this.householdMeetingPointMapping = new ConcurrentHashMap<Id, Id>();
		this.transportModeMapping = new ConcurrentHashMap<Id, String>();
		this.driverVehicleMapping = new ConcurrentHashMap<Id, Id>();
		this.householdDepartures = new HashMap<Id, HouseholdDeparture>();
		
		/*
		 * Create WalkSpeedComparator and calculate travel times for each person.
		 */
		this.walkSpeedComparator = new WalkSpeedComparator();
		this.walkSpeedComparator.calcTravelTimes(scenario.getPopulation());
	}

	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		
		/*
		 * Clear maps for every time step.
		 */
		this.householdMeetingPointMapping.clear();
		this.transportModeMapping.clear();
		this.driverVehicleMapping.clear();
		
		Set<PlanBasedWithinDayAgent> set = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
	
		Iterator<Entry<Id, HouseholdDeparture>> iter = this.householdDepartures.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<Id, HouseholdDeparture> entry = iter.next();
			Id householdId = entry.getKey();
			HouseholdDeparture householdDeparture = entry.getValue();
			
			if (householdDeparture.getDepartureTime() == time) {				
				Id facilityId = householdDeparture.getFacilityId();
				Id meetingPointId = selectHouseholdMeetingPoint.selectRescueMeetingPoint(householdId);
				householdMeetingPointMapping.put(householdId, meetingPointId);
				Household household = households.getHouseholds().get(householdId);
				
				Queue<Vehicle> availableVehicles = getAvailableVehicles(household, facilityId);
				Queue<Id> possibleDrivers = new PriorityQueue<Id>(4, walkSpeedComparator);
				Queue<Id> possiblePassengers = new PriorityQueue<Id>(4, walkSpeedComparator);
				
				// identify potential drivers and passengers
				for (Id personId : household.getMemberIds()) {
					if (modeAvailabilityChecker.hasDrivingLicense(personId)) possibleDrivers.add(personId);
					else possiblePassengers.add(personId);					
				}
				
				/*
				 * Fill people into vehicles. Start with largest cars.
				 * Will end if all people are assigned to a vehicle
				 * or no further vehicles or drivers a available.
				 * Remaining agents will walk.
				 */
				while (availableVehicles.peek() != null) {
					Vehicle vehicle = availableVehicles.poll();
					int seats = vehicle.getType().getCapacity().getSeats();
					
					// if no more drivers are available
					if (possibleDrivers.peek() == null) break;
					
					// set transport mode for driver
					Id driverId = possibleDrivers.poll();
					transportModeMapping.put(driverId, TransportMode.car);
					driverVehicleMapping.put(driverId, vehicle.getId());
					seats--;
					
					List<Id> passengers = new ArrayList<Id>();
					while (seats > 0) {
						Id passengerId = null;
						if (possiblePassengers.peek() != null) {
							passengerId = possiblePassengers.poll();
						} else if (possibleDrivers.peek() != null) {
							passengerId = possibleDrivers.poll();
						} else {
							break;
						}
						
						passengers.add(passengerId);
						transportModeMapping.put(passengerId, PassengerDepartureHandler.passengerTransportMode);
						seats--;
					}
					
					// register person as passenger in the vehicle
					for (Id passengerId : passengers) {
						vehiclesTracker.addPassengerToVehicle(passengerId, vehicle.getId());
					}
				}
				
				// if vehicle capacity is exceeded, remaining agents have to walk
				while (possibleDrivers.peek() != null) {
					transportModeMapping.put(possibleDrivers.poll(), TransportMode.walk);					
				}
				while (possiblePassengers.peek() != null) {
					transportModeMapping.put(possiblePassengers.poll(), TransportMode.walk);					
				}

				// finally add agents to replanning set
				for (Id agentId : household.getMemberIds()) {
					set.add(agentMapping.get(agentId));
				}
			}
		}
		
		return set;
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
	 * Return a queue containing a households vehicles ordered by the number
	 * of seats, starting with the car with the highest number.
	 */
	private Queue<Vehicle> getAvailableVehicles(Household household, Id facilityId) {
		List<Id> availableVehicles = modeAvailabilityChecker.getAvailableCars(household, facilityId);
		
		Queue<Vehicle> queue = new PriorityQueue<Vehicle>(2, new VehicleSeatsComparator());
		
		for (Id id : availableVehicles) {
			Vehicle vehicle = vehicles.getVehicles().get(id);
			queue.add(vehicle);
		}
		
		return queue;
	}
	
	/*
	 * Create a mapping between personIds and the agents in the mobsim.
	 * 
	 * Moreover ensure that the joinedHouseholds and householdDeparture
	 * data structures are filled properly. When the simulation starts,
	 * all households are joined at their home facility.
	 */
	@Override
	public void notifySimulationInitialized(SimulationInitializedEvent e) {
		QSim sim = (QSim) e.getQueueSimulation();

		this.agentMapping.clear();
		for (MobsimAgent mobsimAgent : sim.getAgents()) {
			PlanBasedWithinDayAgent withinDayAgent = (PlanBasedWithinDayAgent) mobsimAgent;
			agentMapping.put(withinDayAgent.getId(), withinDayAgent);				
		}
	}
	
	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
		
		if (e.getSimulationTime() == EvacuationConfig.evacuationTime) {
			this.initiallyCollectHouseholds(e.getSimulationTime());
		} else if (e.getSimulationTime() > EvacuationConfig.evacuationTime) {
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
					log.warn("Household missed its departure time! Id " + householdId);
					iter.remove();
				}
			}
		}
	}
	
	/*
	 * Start collecting households when the evacuation has started.
	 */
	private void initiallyCollectHouseholds(double time) {
		
		/*
		 * Get a Set of Ids of all households to initally define their departure time.
		 */
		this.householdDepartures.clear();
		
		Map<Id, HouseholdPosition> householdPositions = this.householdsTracker.getHouseholdPositions();
		
		for (Entry<Id, HouseholdPosition> entry : householdPositions.entrySet()) {
			Id householdId = entry.getKey();
			HouseholdPosition householdPosition = entry.getValue();
			
			// if the household is joined
			if (householdPosition.isHouseholdJoined()) {
				
				// if the household is at a facility
				if (householdPosition.getPositionType() == Position.FACILITY) {
					
					//if the household is at its meeting point facility
					if (householdPosition.getPositionId().equals(householdPosition.getMeetingPointFacilityId())) {
						
						/*
						 * If the meeting point is not secure, schedule a departure.
						 * Otherwise ignore the household since it current location
						 * is already secure.
						 */
						Id facilityId = householdPosition.getPositionId();
						ActivityFacility facility = this.facilities.getFacilities().get(facilityId);
						boolean facilityIsSecure = !this.coordAnalyzer.isFacilityAffected(facility);
						if (!facilityIsSecure) {
							HouseholdDeparture householdDeparture = createHouseholdDeparture(time, householdId, householdPosition.getPositionId());
							this.householdDepartures.put(householdId, householdDeparture);
						}
					}
				}
				
			}
		}
	}
	
	private void updateHouseholds(Set<Id> householdsToUpdate, double time) {
		
		for (Id id : householdsToUpdate) {
			HouseholdPosition householdPosition = householdsTracker.getHouseholdPosition(id);
			HouseholdDeparture householdDeparture = this.householdDepartures.get(id);
			
			/*
			 * Check whether the household is joined.
			 */
			boolean isJoined = householdPosition.isHouseholdJoined();
			boolean wasJoined = (householdDeparture != null);
			if (isJoined) {
				/*
				 * Check whether the household is in a facility.
				 */
				Position positionType = householdPosition.getPositionType();
				if (positionType == Position.FACILITY) {
					Id facilityId = householdPosition.getPositionId();
					Id meetingPointId = householdPosition.getMeetingPointFacilityId();
					
					/*
					 * Check whether the household is at its meeting facility.
					 */
					if (meetingPointId.equals(facilityId)) {
						
						/*
						 * The household is at its meeting point. If no departure has
						 * been scheduled so far and the facility is not secure, schedule one.
						 */
						if (householdDeparture == null) {
							
							ActivityFacility facility = this.facilities.getFacilities().get(facilityId);
							boolean facilityIsSecure = !this.coordAnalyzer.isFacilityAffected(facility);
							if (!facilityIsSecure) {
								// ... and schedule the household's departure.
								householdDeparture = createHouseholdDeparture(time, id, meetingPointId);
								this.householdDepartures.put(id, householdDeparture);
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
						this.householdDepartures.remove(id);
						if (time > EvacuationConfig.evacuationTime) {
							log.warn("Household is joined at a facility which is not its meeting facility. Id: " + id);							
						}
					}
				}
				
				/*
				 * The household is joined but not at a facility. Therefore ensure
				 * that there is no departure scheduled.
				 */
				else {				
					this.householdDepartures.remove(id);
				}
			}
			
			/*
			 * The household is not joined. Therefore ensure that there is no departure
			 * scheduled for for that household.
			 */
			else {
				this.householdDepartures.remove(id);
				
				/*
				 * If the household was joined and the evacuation has already started.
				 * We do not expect to find a departure before it was scheduled.
				 */
				if (wasJoined && time < householdDeparture.departureTime) {
					log.warn("Household has left its meeting point before scheduled departure. Id " + id);
				}
			}
		}
	}
	
	private HouseholdDeparture createHouseholdDeparture(double time, Id householdId, Id facilityId) {
		
		// TODO: use a function to estimate the departure time
		HouseholdDeparture householdDeparture = new HouseholdDeparture(householdId, facilityId, time + 600);
		
		return householdDeparture;
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
	
	/*
	 * Compares walk speeds of people respecting their age, gender, etc.
	 */
	private static class WalkSpeedComparator implements Comparator<Id>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;
		
		private final PersonalizableTravelTime travelTime;
		private final Link link; 
		private final Map<Id, Double> travelTimesMap;
		
		public WalkSpeedComparator() {
			travelTime = new WalkTravelTimeFactory(new PlansCalcRouteConfigGroup()).createTravelTime();
			
			NetworkFactory factory = new NetworkFactoryImpl(NetworkImpl.createNetwork());
			Node startNode = factory.createNode(new IdImpl("startNode"), new Coord3dImpl(0, 0, 0));
			Node endNode = factory.createNode(new IdImpl("endNode"), new Coord3dImpl(1, 0, 0));
			link = factory.createLink(new IdImpl("link"), startNode, endNode);
			link.setLength(1.0);
			
			travelTimesMap = new HashMap<Id, Double>();
		}
		
		public void calcTravelTimes(Population population) {
			travelTimesMap.clear();
			
			for (Person person : population.getPersons().values()) {
				travelTime.setPerson(person);
				double tt = travelTime.getLinkTravelTime(link, 0.0);
				travelTimesMap.put(person.getId(), tt);
			}
		}
		
		@Override
		public int compare(Id id1, Id id2) {
			double tt1 = travelTimesMap.get(id1);
			double tt2 = travelTimesMap.get(id2);
			
			/*
			 * Invert the return value since people with long travel times should be
			 * at the front end of the queue.
			 */
			return -Double.compare(tt1, tt2);
		}
		
	}
	
	private static class VehicleSeatsComparator implements Comparator<Vehicle>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;
		
		@Override
		public int compare(Vehicle v1, Vehicle v2) {
			
			int seats1 = v1.getType().getCapacity().getSeats();
			int seats2 = v2.getType().getCapacity().getSeats();
			
			if (seats1 > seats2) return 1;
			else if (seats1 < seats2) return -1;
			// both have the same number of seats: order them based on their Id
			else return v1.getId().compareTo(v2.getId());
		}
	}

}