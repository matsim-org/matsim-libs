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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimComparator;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.households.Household;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.ptproject.qsim.comparators.PersonAgentComparator;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifier;

import playground.christoph.evacuation.config.EvacuationConfig;
import playground.christoph.evacuation.core.utils.geometry.Coord3dImpl;
import playground.christoph.evacuation.events.HouseholdEnterMeetingPointEvent;
import playground.christoph.evacuation.events.HouseholdEnterMeetingPointEventImpl;
import playground.christoph.evacuation.events.HouseholdJoinedEvent;
import playground.christoph.evacuation.events.HouseholdLeaveMeetingPointEvent;
import playground.christoph.evacuation.events.HouseholdSeparatedEvent;
import playground.christoph.evacuation.events.HouseholdSetMeetingPointEvent;
import playground.christoph.evacuation.events.handler.HouseholdEnterMeetingPointEventHandler;
import playground.christoph.evacuation.events.handler.HouseholdJoinedEventHandler;
import playground.christoph.evacuation.events.handler.HouseholdLeaveMeetingPointEventHandler;
import playground.christoph.evacuation.events.handler.HouseholdSeparatedEventHandler;
import playground.christoph.evacuation.events.handler.HouseholdSetMeetingPointEventHandler;
import playground.christoph.evacuation.mobsim.PassengerDepartureHandler;
import playground.christoph.evacuation.mobsim.VehiclesTracker;
import playground.christoph.evacuation.trafficmonitoring.WalkTravelTimeFactory;
import playground.christoph.evacuation.withinday.replanning.utils.HouseholdInfo;
import playground.christoph.evacuation.withinday.replanning.utils.HouseholdsUtils;
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
	HouseholdJoinedEventHandler, HouseholdSeparatedEventHandler, HouseholdSetMeetingPointEventHandler,
	HouseholdEnterMeetingPointEventHandler, HouseholdLeaveMeetingPointEventHandler,	SimulationInitializedListener {

	private static final Logger log = Logger.getLogger(JoinedHouseholdsIdentifier.class);
	
	private final Vehicles vehicles;
	private final HouseholdsUtils householdUtils;
	private final SelectHouseholdMeetingPoint selectHouseholdMeetingPoint;
	private final ModeAvailabilityChecker modeAvailabilityChecker;
	private final VehiclesTracker vehiclesTracker;
	private final Map<Id, PlanBasedWithinDayAgent> agentMapping;
	private final Map<Id, String> transportModeMapping;
	private final Map<Id, Id> driverVehicleMapping;
	private final Set<Id> joinedHouseholds;
	private final Queue<HouseholdDeparture> householdDepartures;
	
	private final WalkSpeedComparator walkSpeedComparator;
	
	public JoinedHouseholdsIdentifier(Vehicles vehicles, HouseholdsUtils householdUtils, 
			SelectHouseholdMeetingPoint selectHouseholdMeetingPoint,
			ModeAvailabilityChecker modeAvailabilityChecker, VehiclesTracker vehiclesTracker) {
		this.vehicles = vehicles;
		this.householdUtils = householdUtils;
		this.selectHouseholdMeetingPoint = selectHouseholdMeetingPoint;
		this.modeAvailabilityChecker = modeAvailabilityChecker;
		this.vehiclesTracker = vehiclesTracker;
		
		this.agentMapping = new HashMap<Id, PlanBasedWithinDayAgent>();
		this.transportModeMapping = new ConcurrentHashMap<Id, String>();
		this.driverVehicleMapping = new ConcurrentHashMap<Id, Id>();
		this.householdDepartures = new PriorityBlockingQueue<HouseholdDeparture>(500, new DepartureTimeComparator());
		this.joinedHouseholds = new HashSet<Id>();
		
		this.walkSpeedComparator = new WalkSpeedComparator();
	}

	@Override
	public Set<PlanBasedWithinDayAgent> getAgentsToReplan(double time) {
		
		// clear maps for every time step
		transportModeMapping.clear();
		driverVehicleMapping.clear();
		
//		Set<PlanBasedWithinDayAgent> set = new HashSet<PlanBasedWithinDayAgent>();
		Set<PlanBasedWithinDayAgent> set = new TreeSet<PlanBasedWithinDayAgent>(new PersonAgentComparator());
	
		while (this.householdDepartures.peek() != null) {
			
			HouseholdDeparture householdDeparture = this.householdDepartures.peek();
			if (householdDeparture.getDepartureTime() <= time) {
				this.householdDepartures.poll();
				
				Id householdId = householdDeparture.getHouseholdId();
				Id facilityId = householdDeparture.getFacilityId();
				HouseholdInfo householdInfo = householdUtils.getHouseholdInfoMap().get(householdId);
				Household household = householdInfo.getHousehold();
				selectHouseholdMeetingPoint.selectRescueMeetingPoint(householdId);
				
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
				for (Id agentId : householdInfo.getHousehold().getMemberIds()) {
					set.add(agentMapping.get(agentId));
				}
			} else {
				break;
			}
		}
		
		return set;
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
	
	@Override
	public void reset(int iteration) {
		agentMapping.clear();
		joinedHouseholds.clear();
		householdDepartures.clear();
		transportModeMapping.clear();
		driverVehicleMapping.clear();
	}

	@Override
	public void handleEvent(HouseholdLeaveMeetingPointEvent event) {
		joinedHouseholds.remove(event.getHouseholdId());
		
		/*
		 * The household has been separated, therefore remove scheduled departure.
		 * 
		 * If the evacuation has started, households should leave their meeting point
		 * before their scheduled departure. 
		 */
		if (householdDepartures.remove(new HouseholdDeparture(event.getHouseholdId(), null, 0.0)) && event.getTime() > EvacuationConfig.evacuationTime) {
			log.warn("Household has left its meeting point before scheduled departure. Id " + event.getHouseholdId());
		}
	}

	@Override
	public void handleEvent(HouseholdEnterMeetingPointEvent event) {
		joinedHouseholds.add(event.getHouseholdId());
		
		boolean secureFacility = selectHouseholdMeetingPoint.isFacilitySecure(event.getFacilityId());

		/*
		 * If the household is located in the secure area, we don't have to do
		 * further replanning.
		 * 
		 * TODO: add a function to assume whether a household "feels" secure
		 * (depending on the distance to the boarder of the evacuation area)
		 */
		if (secureFacility) return;
		
		/*
		 * The household has joined and is going to evacuate united.
		 */
		else {
			/*
			 * Schedule the household departure. If the evacuation has not started yet,
			 * set the earliest possible departure time to the start of the evacuation.
			 * We don't want the households to evacuate earlier...
			 */
			double time;
			if (event.getTime() < EvacuationConfig.evacuationTime) {
				time = EvacuationConfig.evacuationTime;
			} else time = event.getTime();
			
			// TODO: use a function to estimate the departure time
			HouseholdDeparture householdDeparture = new HouseholdDeparture(event.getHouseholdId(), event.getFacilityId(), time + 600);
			
			// if there is an old entry: remove it first
			householdDepartures.remove(householdDeparture);
			householdDepartures.add(householdDeparture);			
		}
	}

	@Override
	public void handleEvent(HouseholdSetMeetingPointEvent event) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public void handleEvent(HouseholdSeparatedEvent event) {
//		joinedHouseholds.remove(event.getHouseholdId());
//		
//		/*
//		 * The household has been separated, therefore remove scheduled departure.
//		 */
//		if (householdDepartures.remove(new HouseholdDeparture(event.getHouseholdId(), null, 0.0))) {
//			log.warn("Household has been separated before scheduled departure. Id " + event.getHouseholdId());
//		}
	}

	@Override
	public void handleEvent(HouseholdJoinedEvent event) {
//		joinedHouseholds.add(event.getHouseholdId());
//		
//		boolean secureFacility = selectHouseholdMeetingPoint.isFacilitySecure(event.getFacilityId());
//
//		/*
//		 * If the household is located in the secure area, we don't have to do
//		 * further replanning.
//		 * 
//		 * TODO: add a function to assume whether a household "feels" secure
//		 * (depending on the distance to the boarder of the evacuation area)
//		 */
//		if (secureFacility) return;
//		
//		/*
//		 * The household has joined and is going to evacuate united.
//		 */
//		else {
//			// TODO: use a function to estimate the departure time
//			HouseholdDeparture householdDeparture = new HouseholdDeparture(event.getHouseholdId(), event.getFacilityId(), event.getTime() + 600);
//			
//			// if there is an old entry: remove it first
//			householdDepartures.remove(householdDeparture);
//			householdDepartures.add(householdDeparture);			
//		}
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

		for (MobsimAgent mobsimAgent : sim.getAgents()) {
			PlanBasedWithinDayAgent withinDayAgent = (PlanBasedWithinDayAgent) mobsimAgent;
			agentMapping.put(withinDayAgent.getId(), withinDayAgent);				
		}
		
		/*
		 * calculate travel times for each agent in the WalkTravelTimeComparator
		 */
		this.walkSpeedComparator.calcTravelTimes(agentMapping);
		
		for (HouseholdInfo householdInfo : this.householdUtils.getHouseholdInfoMap().values()) {
			
			/*
			 * We create a dummy event. 
			 * By using the evacuation time as event time we schedule the evacuation 
			 * of households that do not move until the evacuation starts. Otherwise they
			 * would be ignored.
			 */
			double time = EvacuationConfig.evacuationTime;
			Id householdId = householdInfo.getHousehold().getId();
//			Id linkId = null;	// we don't need this in in the handleEvent method
			Id facilityId = householdInfo.getMeetingPointId();
//			String eventType = "home";
//			HouseholdJoinedEvent event = new HouseholdJoinedEventImpl(time, householdId, linkId, facilityId, eventType);
//			this.handleEvent(event);
			HouseholdEnterMeetingPointEvent event = new HouseholdEnterMeetingPointEventImpl(time, householdId, facilityId);
			this.handleEvent(event);
		}
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
	
	private static class DepartureTimeComparator implements Comparator<HouseholdDeparture>, Serializable, MatsimComparator {

		private static final long serialVersionUID = 1L;

		@Override
		public int compare(HouseholdDeparture h1, HouseholdDeparture h2) {
			int cmp = Double.compare(h1.getDepartureTime(), h2.getDepartureTime());
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				return h2.getHouseholdId().compareTo(h2.getHouseholdId());
			}
			return cmp;
		}
	}
	
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
		
		public void calcTravelTimes(Map<Id, PlanBasedWithinDayAgent> agentMapping) {
			travelTimesMap.clear();
			
			for (PlanBasedWithinDayAgent agent : agentMapping.values()) {
				Person person = agent.getSelectedPlan().getPerson();
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