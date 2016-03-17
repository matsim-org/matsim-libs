/* *********************************************************************** *
 * project: org.matsim.*
 * QueueLink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.sergioo.ptsim2013.qnetsimengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.mobsim.qsim.interfaces.SignalizeableItem;
import org.matsim.core.mobsim.qsim.interfaces.TimeVariantLink;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultSignalizeableItem;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.FIFOVehicleQ;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.snapshotwriters.VisData;

/**
 * Please read the docu of QBufferItem, QLane, QLinkInternalI (arguably to be renamed
 * into something like AbstractQLink) and QLinkImpl jointly. kai, nov'11
 * 
 * @author dstrippgen
 * @author dgrether
 * @author mrieser
 */
public class PTQLink2 implements NetsimLink, TimeVariantLink {

	// static variables (no problem with memory)
	final private static Logger log = Logger.getLogger(PTQLink2.class);
	private static int spaceCapWarningCount = 0;
	static boolean HOLES = false ; // can be set from elsewhere in package, but not from outside.  kai, nov'10
	private static int congDensWarnCnt = 0;
	private static int congDensWarnCnt2 = 0;

	/**
	 * The remaining integer part of the flow capacity available in one time step to move vehicles into the
	 * buffer.
	 */
	double remainingflowCap = 0.0;
	/**
	 * Stores the accumulated fractional parts of the flow capacity. See also
	 * flowCapFraction.
	 */
	double flowcap_accumulate = 1.0;
	/**
	 * true, i.e. green, if the link is not signalized
	 */
	boolean thisTimeStepGreen = true;
	double inverseFlowCapacityPerTimeStep;
	double flowCapacityPerTimeStepFractionalPart;
	/**
	 * The number of vehicles able to leave the buffer in one time step (usually 1s).
	 */
	double flowCapacityPerTimeStep;
	int bufferStorageCapacity;
	double usedBufferStorageCapacity = 0.0;
	
	// instance variables (problem with memory)
	private final Queue<Hole> holes = new LinkedList<Hole>() ;

	
	private static final Comparator<QVehicle> VEHICLE_EXIT_COMPARATOR = new QVehicleEarliestLinkExitTimeComparator();

	final Link link;

	final QNetwork network;	

	// joint implementation for Customizable
	private Map<String, Object> customAttributes = new HashMap<String, Object>();

	private final Map<Id<Vehicle>, QVehicle> parkedVehicles = new LinkedHashMap<Id<Vehicle>, QVehicle>(10);

	private final Map<Id<Person>, MobsimAgent> additionalAgentsOnLink = new LinkedHashMap<Id<Person>, MobsimAgent>();

	private final Map<Id<Vehicle>, Queue<MobsimDriverAgent>> driversWaitingForCars = new LinkedHashMap<Id<Vehicle>, Queue<MobsimDriverAgent>>();
	
	private final Map<Id<Person>, MobsimDriverAgent> driversWaitingForPassengers = new LinkedHashMap<Id<Person>, MobsimDriverAgent>();
	
	// vehicleId 
	private final Map<Id<Vehicle>, Set<MobsimAgent>> passengersWaitingForCars = new LinkedHashMap<Id<Vehicle>, Set<MobsimAgent>>();

	/**
	 * A list containing all transit vehicles that are at a stop but not
	 * blocking other traffic on the lane.
	 */
	/*package*/ final Queue<QVehicle> transitVehicleStopQueue = new PriorityQueue<QVehicle>(5, VEHICLE_EXIT_COMPARATOR);

	/**
	 * All vehicles from parkingList move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	/*package*/ final Queue<QVehicle> waitingList = new LinkedList<QVehicle>();

	/*package*/ NetElementActivator netElementActivator;

	/*package*/ final boolean insertingWaitingVehiclesBeforeDrivingVehicles;
	/**
	 * Reference to the QueueNode which is at the end of each QueueLink instance
	 */
	private final QNode toQueueNode;

	private boolean active = false;

	private final double length;

	private double freespeedTravelTime = Double.NaN;

	/** the last timestep the front-most vehicle in the buffer was moved. Used for detecting dead-locks. */
	private double bufferLastMovedTime = Time.UNDEFINED_TIME;

	/**
	 * The list of vehicles that have not yet reached the end of the link
	 * according to the free travel speed of the link
	 */
	private final VehicleQ<QVehicle> vehQueue;

	/**
	 * This needs to be a ConcurrentHashMap because it can be accessed concurrently from
	 * two different threads via addFromIntersection(...) and popFirstVehicle().
	 */
	private final Map<QVehicle, Double> linkEnterTimeMap = new ConcurrentHashMap<QVehicle, Double>();

	private double storageCapacity;

	private double usedStorageCapacity;
	
	/**
	 * Holds all vehicles that are ready to cross the outgoing intersection
	 */
	private final Queue<QVehicle> buffer = new LinkedList<QVehicle>();
	
	
	
	/**
	 * null if the link is not signalized
	 */
	private DefaultSignalizeableItem qSignalizedItem = null;
	private double congestedDensity_veh_m;
	private int nHolesMax;

	/**
	 * Initializes a QueueLink with one QueueLane.
	 * @param link2
	 * @param network
	 * @param toNode
	 */
	public PTQLink2(final Link link2, QNetwork network, final QNode toNode) {
		this(link2, network, toNode, new FIFOVehicleQ());
	}

	/** 
	 * This constructor allows inserting a custom vehicle queue proper, e.g. to implement passing.
	 * 
	 */
	public PTQLink2(final Link link2, QNetwork network, final QNode toNode, final VehicleQ<QVehicle> vehicleQueue) {
		this.link = link2 ;
		this.network = network;
		this.netElementActivator = network.simEngine;
		this.insertingWaitingVehiclesBeforeDrivingVehicles = 
				network.simEngine.getMobsim().getScenario().getConfig().qsim().isInsertingWaitingVehiclesBeforeDrivingVehicles() ;
		this.toQueueNode = toNode;
		this.vehQueue = vehicleQueue;
		this.length = this.getLink().getLength();
		this.freespeedTravelTime = this.length / this.getLink().getFreespeed();
		if (Double.isNaN(this.freespeedTravelTime)) {
			throw new IllegalStateException("Double.NaN is not a valid freespeed travel time for a link. Please check the attributes length and freespeed!");
		}
		this.calculateCapacities();
		
		if ( HOLES ) {
			for ( int ii=0 ; ii<this.storageCapacity; ii++ ) {
				Hole hole = new Hole() ;	
				hole.setEarliestLinkExitTime( Double.NEGATIVE_INFINITY ) ;
				this.holes.add(hole) ;
			}
			//  this does, once more, not work with variable vehicle sizes.  kai, may'13
		}
		
	}

	/* 
	 *  There are two "active" functionalities (see isActive()).  It probably still works, but it does not look like
	 * it is intended this way.  kai, nov'11
	 */
	void activateLink() {
		if (!this.active) {
			netElementActivator.activateLink(this);
			this.active = true;
		}
	}

	void clearVehicles() {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

		/*
		 * Some agents might be present in multiple lists/maps.
		 * Ensure that only one stuck event per agent is created.
		 */
		Set<Id<Person>> stuckAgents = new HashSet<Id<Person>>();
		
		for (QVehicle veh : this.parkedVehicles.values()) {
			if (veh.getDriver() != null) {
				// skip transit driver which perform an activity while their vehicle is parked
				if (veh.getDriver().getState() != State.LEG) continue;

				if (stuckAgents.contains(veh.getDriver().getId())) continue;
				else stuckAgents.add(veh.getDriver().getId());
				
				
				this.network.simEngine.getMobsim().getEventsManager().processEvent(
						new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
				this.network.simEngine.getMobsim().getAgentCounter().incLost();
				this.network.simEngine.getMobsim().getAgentCounter().decLiving();
			}
			
			for (PassengerAgent passenger : veh.getPassengers()) {
				if (stuckAgents.contains(passenger.getId())) continue;
				else stuckAgents.add(passenger.getId());
				
				MobsimAgent mobsimAgent = (MobsimAgent) passenger;
				
				this.network.simEngine.getMobsim().getEventsManager().processEvent(
						new PersonStuckEvent(now, mobsimAgent.getId(), veh.getCurrentLink().getId(), mobsimAgent.getMode()));
				this.network.simEngine.getMobsim().getAgentCounter().incLost();
				this.network.simEngine.getMobsim().getAgentCounter().decLiving();
			}
		}
		this.parkedVehicles.clear();
		for (MobsimAgent driver : driversWaitingForPassengers.values()) {		
			if (stuckAgents.contains(driver.getId())) continue;
			else stuckAgents.add(driver.getId());
			
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new PersonStuckEvent(now, driver.getId(), driver.getCurrentLinkId(), driver.getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		driversWaitingForPassengers.clear();
		
		
		for (Queue<MobsimDriverAgent> queue : driversWaitingForCars.values()) {
			for (MobsimAgent driver : queue) {
				if (stuckAgents.contains(driver.getId())) continue;
				stuckAgents.add(driver.getId());
				
				this.network.simEngine.getMobsim().getEventsManager().processEvent(
						new PersonStuckEvent(now, driver.getId(), driver.getCurrentLinkId(), driver.getMode()));
				this.network.simEngine.getMobsim().getAgentCounter().incLost();
				this.network.simEngine.getMobsim().getAgentCounter().decLiving();
			}
		}
		driversWaitingForCars.clear();
		for (Set<MobsimAgent> passengers : passengersWaitingForCars.values()) {
			for (MobsimAgent passenger : passengers) {
				if (stuckAgents.contains(passenger.getId())) continue;
				else stuckAgents.add(passenger.getId());
				
				this.network.simEngine.getMobsim().getEventsManager().processEvent(
						new PersonStuckEvent(now, passenger.getId(), passenger.getCurrentLinkId(), passenger.getMode()));
				this.network.simEngine.getMobsim().getAgentCounter().incLost();
				this.network.simEngine.getMobsim().getAgentCounter().decLiving();				
			}
		}
		this.passengersWaitingForCars.clear();
		
		for (QVehicle veh : this.waitingList) {
			if (stuckAgents.contains(veh.getDriver().getId())) continue;
			else stuckAgents.add(veh.getDriver().getId());
			
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		this.waitingList.clear();

		for (QVehicle veh : this.vehQueue) {
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		this.vehQueue.clear();
		this.linkEnterTimeMap.clear();

		for (QVehicle veh : this.buffer) {
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		this.buffer.clear();
		this.usedBufferStorageCapacity = 0;
	}


	boolean doSimStep(double now) {
		updateBufferCapacity();

		if ( this.insertingWaitingVehiclesBeforeDrivingVehicles ) {
			moveWaitToBuffer(now);
			moveLaneToBuffer(now);
		} else {
			moveLaneToBuffer(now);
			moveWaitToBuffer(now);
		}
		// moveLaneToBuffer moves vehicles from lane to buffer.  Includes possible vehicle arrival.  Which, I think, would only be triggered
		// if this is the original lane.
		
		// moveWaitToBuffer moves waiting (i.e. just departed) vehicles into the buffer.

		this.active = this.isActive();
		return active;
	}


	/**
	 * Move vehicles from link to buffer, according to buffer capacity and
	 * departure time of vehicle. Also removes vehicles from lane if the vehicle
	 * arrived at its destination.
	 *
	 * @param now
	 *          The current time.
	 */
	private void moveLaneToBuffer(final double now) {
		QVehicle veh;

		this.moveTransitToQueue(now);

		// handle regular traffic
		while ((veh = this.vehQueue.peek()) != null) {
			if (veh.getEarliestLinkExitTime() > now){
				return;
			}
			MobsimDriverAgent driver = veh.getDriver();

			boolean handled = this.handleTransitStop(now, veh, driver);

			if (!handled) {
				// Check if veh has reached destination:
				if ((this.getLink().getId().equals(driver.getDestinationLinkId())) && (driver.chooseNextLinkId() == null)) {
					this.addParkedVehicle(veh);
					network.simEngine.letVehicleArrive(veh);
					this.makeVehicleAvailableToNextDriver(veh, now);
					// remove _after_ processing the arrival to keep link active
					this.vehQueue.poll();
					this.usedStorageCapacity -= veh.getSizeInEquivalents();
					if ( HOLES ) {
						Hole hole = new Hole() ;
						hole.setEarliestLinkExitTime( now + this.link.getLength()*3600./15./1000. ) ;
						holes.add( hole ) ;
					}
					continue;
				}

				/* is there still room left in the buffer, or is it overcrowded from the
				 * last time steps? */
				if (!hasFlowCapacityLeftAndBufferSpace()) {
					return;
				}

				if (driver instanceof TransitDriverAgent) {
					TransitDriverAgent trDriver = (TransitDriverAgent) driver;
					Id<Link> nextLinkId = trDriver.chooseNextLinkId();
					if (nextLinkId == null || nextLinkId.equals(trDriver.getCurrentLinkId())) {
						// special case: transit drivers can specify the next link being the current link
						// this can happen when a transit-lines route leads over exactly one link
						// normally, vehicles would not even drive on that link, but transit vehicles must
						// "drive" on that link in order to handle the stops on that link
						// so allow them to return some non-null link id in chooseNextLink() in order to be
						// placed on the link, and here we'll remove them again if needed...
						// ugly hack, but I didn't find a nicer solution sadly... mrieser, 5mar2011
						
						// Beispiel: Kanzler-Ubahn in Berlin.  Im Visum-Netz mit nur 1 Kante, mit Haltestelle am Anfang und
						// am Ende der Kante.  Zweite Haltestelle wird nur bedient, wenn das Fahrzeug im matsim-Sinne zum 
						// zweiten Mal auf die Kante gesetzt wird (oder so ähnlich, aber wir brauchen "nextLink==currentLink").
						// kai & marcel, mar'12
						
						network.simEngine.letVehicleArrive(veh);
						this.addParkedVehicle(veh);
						makeVehicleAvailableToNextDriver(veh, now);
						// remove _after_ processing the arrival to keep link active
						this.vehQueue.poll();
						this.usedStorageCapacity -= veh.getSizeInEquivalents();
						if ( HOLES ) {
							Hole hole = new Hole() ;
							hole.setEarliestLinkExitTime( now + this.link.getLength()*3600./15./1000. ) ;
							holes.add( hole ) ;
						}
						continue;
					}
				}
				addToBuffer(veh, now);
				this.vehQueue.poll();
				this.usedStorageCapacity -= veh.getSizeInEquivalents();
				if ( HOLES ) {
					Hole hole = new Hole() ;
					double offset = this.link.getLength()*3600./15./1000. ;
					hole.setEarliestLinkExitTime( now + 0.9*offset + 0.2*MatsimRandom.getRandom().nextDouble()*offset ) ;
					holes.add( hole ) ;
				}
			}
		} // end while
	}

	/*package*/ final void addParkedVehicle(MobsimVehicle vehicle) {
		QVehicle qveh = (QVehicle) vehicle; // cast ok: when it gets here, it needs to be a qvehicle to work.
		this.parkedVehicles.put(qveh.getId(), qveh);
		qveh.setCurrentLink(this.link);
	}
	/*package*/ final QVehicle removeParkedVehicle(Id<Vehicle> vehicleId) {
		return this.parkedVehicles.remove(vehicleId);
	}
	/*package*/ QVehicle getParkedVehicle(Id<Vehicle> vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}
	
	final void letVehicleDepart(QVehicle vehicle, double now) {
		MobsimDriverAgent driver = vehicle.getDriver();
		if (driver == null) throw new RuntimeException("Vehicle cannot depart without a driver!");
		
		EventsManager eventsManager = network.simEngine.getMobsim().getEventsManager();
		eventsManager.processEvent(new PersonEntersVehicleEvent(now, driver.getId(), vehicle.getId()));
		this.addDepartingVehicle(vehicle);
	}

	/*
	 * If the vehicle is parked at the current link, insert the passenger,
	 * create an enter event and return true. Otherwise add the agent to
	 * the waiting list and return false.
	 */
	final boolean insertPassengerIntoVehicle(MobsimAgent passenger, Id<Vehicle> vehicleId, double now) {
		QVehicle vehicle = this.getParkedVehicle(vehicleId);
		
		// if the vehicle is not parked at the link, mark the agent as passenger waiting for vehicle
		if (vehicle == null) {
			registerPassengerAgentWaitingForCar(passenger, vehicleId);
			return false;
		} else {
			boolean added = vehicle.addPassenger((PassengerAgent) passenger);
			if (!added) {
				log.warn("Passenger " + passenger.getId().toString() + 
				" could not be inserted into vehicle " + vehicleId.toString() +
				" since there is no free seat available!");
				return false;
			}
			
			((PassengerAgent) passenger).setVehicle(vehicle);
			EventsManager eventsManager = network.simEngine.getMobsim().getEventsManager();
			eventsManager.processEvent(new PersonEntersVehicleEvent(now, passenger.getId(), vehicle.getId()));
			// TODO: allow setting passenger's currentLinkId to null
			
			return true;
		}
	}

	final boolean addTransitToBuffer(final double now, final QVehicle veh) {
		if (veh.getDriver() instanceof TransitDriverAgent) {
			TransitDriverAgent driver = (TransitDriverAgent) veh.getDriver();
			while (true) {
				TransitStopFacility stop = driver.getNextTransitStop();
				if ((stop != null) && (stop.getLinkId().equals(getLink().getId()))) {
					double delay = driver.handleTransitStop(stop, now);
					if (delay > 0.0) {
						veh.setEarliestLinkExitTime(now + delay);
						// add it to the stop queue, can do this as the waitQueue is also non-blocking anyway
						transitVehicleStopQueue.add(veh);
						return true;
					}
				} else {
					return false;
				}
			}
		}
		return false;
	}
	
	void makeVehicleAvailableToNextDriver(QVehicle veh, double now) {
		
		/*
		 * Insert waiting passengers into vehicle.
		 */
		Id<Vehicle> vehicleId = veh.getId();
		Set<MobsimAgent> passengers = this.passengersWaitingForCars.get(vehicleId);
		if (passengers != null) {
			// Copy set of passengers since otherwise we would modify it concurrently.
			List<MobsimAgent> passengersToHandle = new ArrayList<MobsimAgent>(passengers);
			for (MobsimAgent passenger : passengersToHandle) {
				this.unregisterPassengerAgentWaitingForCar(passenger, vehicleId);
				this.insertPassengerIntoVehicle(passenger, vehicleId, now);
			}
		}
		
		/*
		 * If the next driver is already waiting for the vehicle, check whether
		 * all passengers are also there. If not, the driver is not inserted
		 * into the vehicle and the vehicle does not depart.
		 */
		final Queue<MobsimDriverAgent> driversWaitingForCar = driversWaitingForCars.get(veh.getId());
		final boolean thereIsDriverWaiting = driversWaitingForCar != null && !driversWaitingForCar.isEmpty();
		if ( thereIsDriverWaiting ) {
			MobsimDriverAgent driverWaitingForPassengers =
				driversWaitingForPassengers.get(
						driversWaitingForCar.element().getId());
			if (driverWaitingForPassengers != null) return;
		}
		
		/*
		 * If there is a driver waiting for its vehicle, and this car is not currently already leaving again with the
		 * same vehicle, put the new driver into the vehicle and let it depart.
		 */
		if (thereIsDriverWaiting && veh.getDriver() == null) {
			// set agent as driver and then let the vehicle depart
			veh.setDriver(driversWaitingForCar.remove());
			if (driversWaitingForCar.isEmpty()) {
				final Queue<MobsimDriverAgent> r = driversWaitingForCars.remove(veh.getId());
				assert r == driversWaitingForCar;
			}
			removeParkedVehicle( veh.getId() );
			this.letVehicleDepart(veh, now);
		}
	}
	/**
	 * Move as many waiting cars to the link as it is possible
	 *
	 * @param now
	 *          the current time
	 */
	private void moveWaitToBuffer(final double now) {
		while (hasFlowCapacityLeftAndBufferSpace()) {
			QVehicle veh = this.waitingList.poll();
			if (veh == null) {
				return;
			}

			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new VehicleEntersTrafficEvent(now, veh.getDriver().getId(), this.getLink().getId(), veh.getId(), veh.getDriver().getMode(), 1.0));
			boolean handled = this.addTransitToBuffer(now, veh);

			if (!handled) {


				if (veh.getDriver() instanceof TransitDriverAgent) {
					TransitDriverAgent trDriver = (TransitDriverAgent) veh.getDriver();
					Id<Link> nextLinkId = trDriver.chooseNextLinkId();
					if (nextLinkId == null || nextLinkId.equals(trDriver.getCurrentLinkId())) {
						// special case: transit drivers can specify the next link being the current link
						// this can happen when a transit-lines route leads over exactly one link
						// normally, vehicles would not even drive on that link, but transit vehicles must
						// "drive" on that link in order to handle the stops on that link
						// so allow them to return some non-null link id in chooseNextLink() in order to be
						// placed on the link, and here we'll remove them again if needed...
						// ugly hack, but I didn't find a nicer solution sadly... mrieser, 5mar2011
						trDriver.endLegAndComputeNextState(now);
						this.addParkedVehicle(veh);
						this.network.simEngine.internalInterface.arrangeNextAgentState(trDriver) ;
						this.makeVehicleAvailableToNextDriver(veh, now);
						// remove _after_ processing the arrival to keep link active
						this.vehQueue.poll();
						this.usedStorageCapacity -= veh.getSizeInEquivalents();
						if ( HOLES ) {
							Hole hole = new Hole() ;
							hole.setEarliestLinkExitTime( now + this.link.getLength()*3600./15./1000. ) ;
							holes.add( hole ) ;
						}
						continue;
					}
				}

				addToBuffer(veh, now);
				//				this.linkEnterTimeMap.put(veh, now);
				// ( really??  kai, jan'11)
			}
		}
	}

	/**
	 * This method
	 * moves transit vehicles from the stop queue directly to the front of the
	 * "queue" of the QLink. An advantage is that this will observe flow
	 * capacity restrictions. 
	 */
	private void moveTransitToQueue(final double now) {
		QVehicle veh;
		// handle transit traffic in stop queue
		List<QVehicle> departingTransitVehicles = null;
		while ((veh = transitVehicleStopQueue.peek()) != null) {
			// there is a transit vehicle.
			if (veh.getEarliestLinkExitTime() > now) {
				break;
			}
			if (departingTransitVehicles == null) {
				departingTransitVehicles = new LinkedList<QVehicle>();
			}
			departingTransitVehicles.add(transitVehicleStopQueue.poll());
		}
		if (departingTransitVehicles != null) {
			// add all departing transit vehicles at the front of the vehQueue
			ListIterator<QVehicle> iter = departingTransitVehicles.listIterator(departingTransitVehicles.size());
			while (iter.hasPrevious()) {
				this.vehQueue.addFirst(iter.previous());
			}
		}
	}


	private boolean handleTransitStop(final double now, final QVehicle veh,
			final MobsimDriverAgent driver) {
		boolean handled = false;
		// handle transit driver if necessary
		if (driver instanceof TransitDriverAgent) {
			TransitDriverAgent transitDriver = (TransitDriverAgent) veh.getDriver();
			TransitStopFacility stop = transitDriver.getNextTransitStop();
			if ((stop != null) && (stop.getLinkId().equals(getLink().getId()))) {
				double delay = transitDriver.handleTransitStop(stop, now);
				if (delay > 0.0) {

					veh.setEarliestLinkExitTime(now + delay);
					// (if the vehicle is not removed from the queue in the following lines, then this will effectively block the lane

					if (!stop.getIsBlockingLane()) {
						this.vehQueue.poll(); // remove the bus from the queue
						transitVehicleStopQueue.add(veh); // and add it to the stop queue
					}
				}
				/* start over: either this veh is still first in line,
				 * but has another stop on this link, or on another link, then it is moved on
				 */
				handled = true;
			}
		}
		return handled;
	}

	/*package*/ final void addDepartingVehicle(MobsimVehicle mvehicle) {
		QVehicle vehicle = (QVehicle) mvehicle;
		this.waitingList.add(vehicle);
		vehicle.setCurrentLink(this.getLink());
		this.activateLink();
	}
	
	boolean isNotOfferingVehicle() {
		return this.buffer.isEmpty();
	}

	boolean hasSpace() {
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;

		boolean storageOk = this.usedStorageCapacity < this.storageCapacity ;
		if ( !HOLES ) {
			return storageOk ;
		}
		// continue only if HOLES
		if ( !storageOk ) {
			return false ;
		}
		// at this point, storage is ok, so start checking holes:
		QItem hole = holes.peek();
		if ( hole==null ) { // no holes available at all; in theory, this should not happen since covered by !storageOk
			//			log.warn( " !hasSpace since no holes available ") ;
			return false ;
		}
		if ( hole.getEarliestLinkExitTime() > now ) {
			//			log.warn( " !hasSpace since all hole arrival times lie in future ") ;
			return false ;
		}
		return true ;
	}


	@Override
	public void recalcTimeVariantAttributes() {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay() ;

		this.freespeedTravelTime = this.length / this.getLink().getFreespeed(now);
		calculateFlowCapacity(now);
		calculateStorageCapacity(now);
	}

	private void calculateCapacities() {
		calculateFlowCapacity(Time.UNDEFINED_TIME);
		calculateStorageCapacity(Time.UNDEFINED_TIME);
		this.flowcap_accumulate = (this.flowCapacityPerTimeStepFractionalPart == 0.0 ? 0.0 : 1.0);
	}

	private void calculateFlowCapacity(final double time) {
		this.flowCapacityPerTimeStep = ((LinkImpl)this.getLink()).getFlowCapacityPerSec(time);
		// we need the flow capacity per sim-tick and multiplied with flowCapFactor
		this.flowCapacityPerTimeStep = this.flowCapacityPerTimeStep
				* network.simEngine.getMobsim().getSimTimer().getSimTimestepSize()
				* network.simEngine.getMobsim().getScenario().getConfig().qsim().getFlowCapFactor();
		this.inverseFlowCapacityPerTimeStep = 1.0 / this.flowCapacityPerTimeStep;
		this.flowCapacityPerTimeStepFractionalPart = this.flowCapacityPerTimeStep - (int) this.flowCapacityPerTimeStep;
	}

	private void calculateStorageCapacity(final double time) {
		double storageCapFactor = network.simEngine.getMobsim().getScenario().getConfig().qsim().getStorageCapFactor();
		this.bufferStorageCapacity = (int) Math.ceil(this.flowCapacityPerTimeStep);

		double numberOfLanes = this.getLink().getNumberOfLanes(time);
		// first guess at storageCapacity:
		this.storageCapacity = (this.length * numberOfLanes)
				/ ((NetworkImpl) network.simEngine.getMobsim().getScenario().getNetwork()).getEffectiveCellSize() * storageCapFactor;

		// storage capacity needs to be at least enough to handle the cap_per_time_step:
		this.storageCapacity = Math.max(this.storageCapacity, this.bufferStorageCapacity);

		/*
		 * If speed on link is relatively slow, then we need MORE cells than the
		 * above spaceCap to handle the flowCap. Example: Assume freeSpeedTravelTime
		 * (aka freeTravelDuration) is 2 seconds. Than I need the spaceCap = TWO times
		 * the flowCap to handle the flowCap.
		 */
		double tempStorageCapacity = this.freespeedTravelTime * this.flowCapacityPerTimeStep;
		// yy note: freespeedTravelTime may be Inf.  In this case, storageCapacity will also be set to Inf.  This can still be
		// interpreted, but it means that the link will act as an infinite sink.  kai, nov'10

		if (this.storageCapacity < tempStorageCapacity) {
			if (spaceCapWarningCount <= 10) {
				log.warn("Link " + this.getLink().getId() + " too small: enlarge storage capacity from: " + this.storageCapacity
						+ " Vehicles to: " + tempStorageCapacity + " Vehicles.  This is not fatal, but modifies the traffic flow dynamics.");
				if (spaceCapWarningCount == 10) {
					log.warn("Additional warnings of this type are suppressed.");
				}
				spaceCapWarningCount++;
			}
			this.storageCapacity = tempStorageCapacity;
		}
		
		if ( HOLES ) {
			// yyyy number of initial holes (= max number of vehicles on link given bottleneck spillback) is, in fact, dicated
			// by the bottleneck flow capacity, together with the fundamental diagram. :-(  kai, ???'10
			//
			// Alternative would be to have link entry capacity constraint.  This, however, does not work so well with the
			// current "parallel" logic, where capacity constraints are modeled only on the link.  kai, nov'10
			double bnFlowCap_s = ((LinkImpl)this.link).getFlowCapacityPerSec() ;

			// ( c * n_cells - cap * L ) / (L * c) = (n_cells/L - cap/c) ;
			congestedDensity_veh_m = this.storageCapacity/this.link.getLength() - (bnFlowCap_s*3600.)/(15.*1000) ;

			if ( congestedDensity_veh_m > 10. ) {
				if ( congDensWarnCnt2 < 1 ) {
					congDensWarnCnt2++ ;
					log.warn("congestedDensity_veh_m very large: " + congestedDensity_veh_m
							+ "; does this make sense?  Setting to 10 veh/m (which is still a lot but who knows). "
							+ "Definitely can't have it at Inf." ) ;
				}
			}

			// congestedDensity is in veh/m.  If this is less than something reasonable (e.g. 1veh/50m) or even negative,
			// then this means that the link has not enough storageCapacity (essentially not enough lanes) to transport the given
			// flow capacity.  Will increase the storageCapacity accordingly:
			if ( congestedDensity_veh_m < 1./50 ) {
				if ( congDensWarnCnt < 1 ) {
					congDensWarnCnt++ ;
					log.warn( "link not ``wide'' enough to process flow capacity with holes.  increasing storage capacity ...") ;
					log.warn( Gbl.ONLYONCE ) ;
				}
				this.storageCapacity = (1./50 + bnFlowCap_s*3600./(15.*1000)) * this.link.getLength() ;
				congestedDensity_veh_m = this.storageCapacity/this.link.getLength() - (bnFlowCap_s*3600.)/(15.*1000) ;
			}

			nHolesMax = (int) Math.ceil( congestedDensity_veh_m * this.link.getLength() ) ;
			log.warn(
					" nHoles: " + nHolesMax
					+ " storCap: " + this.storageCapacity
					+ " len: " + this.link.getLength()
					+ " bnFlowCap: " + bnFlowCap_s
					+ " congDens: " + congestedDensity_veh_m
					) ;
			for ( int ii=0 ; ii<nHolesMax ; ii++ ) {
				Hole hole = new Hole() ;
				hole.setEarliestLinkExitTime( 0. ) ;
				holes.add( hole ) ;
			}
			//			System.exit(-1);
		}
	}

	QVehicle getVehicle(Id<Vehicle> vehicleId) {
		QVehicle ret = this.parkedVehicles.get(vehicleId);
		if (ret != null) {
			return ret;
		}
		for (QVehicle veh : this.vehQueue) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QVehicle veh : this.buffer) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		for (QVehicle veh : this.waitingList) {
			if (veh.getId().equals(vehicleId))
				return veh;
		}
		return null;
	}

	@Override
	public final Collection<MobsimVehicle> getAllVehicles() {
		Collection<MobsimVehicle> vehicles = this.getAllNonParkedVehicles();
		vehicles.addAll(this.parkedVehicles.values());
		return vehicles;
	}
	
	@Override
	public Collection<MobsimVehicle> getAllNonParkedVehicles(){
		Collection<MobsimVehicle> vehicles = new ArrayList<MobsimVehicle>();
		vehicles.addAll(this.transitVehicleStopQueue);
		vehicles.addAll(this.waitingList);
		vehicles.addAll(this.vehQueue);
		vehicles.addAll(this.buffer);
		return vehicles;
	}

	/**
	 * @return the total space capacity available on that link (includes the space on lanes if available)
	 */
	double getSpaceCap() {
		return this.storageCapacity;
	}

	int vehOnLinkCount() {
		// called by one test case
		return this.vehQueue.size();
	}


	@Override
	public Link getLink() {
		return this.link;
	}

	public QNode getToNode() {
		return this.toQueueNode;
	}

	/**
	 * This method returns the normalized capacity of the link, i.e. the capacity
	 * of vehicles per second. It is considering the capacity reduction factors
	 * set in the config and the simulation's tick time.
	 *
	 * @return the flow capacity of this link per second, scaled by the config
	 *         values and in relation to the SimulationTimer's simticktime.
	 */
	double getSimulatedFlowCapacity() {
		return this.flowCapacityPerTimeStep;
	}

	private boolean isActive() {
		/*
		 * Leave Link active as long as there are vehicles on the link (ignore
		 * buffer because the buffer gets emptied by nodes and not links) and leave
		 * link active until buffercap has accumulated (so a newly arriving vehicle
		 * is not delayed).
		 */
		boolean active = (this.flowcap_accumulate < 1.0) || (!this.vehQueue.isEmpty()) 
				|| (!this.waitingList.isEmpty() || (!this.transitVehicleStopQueue.isEmpty()));
		return active;
	}

	private double effectiveVehicleFlowConsumptionInPCU( QVehicle veh ) {
//		return Math.min(1.0, veh.getSizeInEquivalents() ) ;
		return veh.getSizeInEquivalents();
	}

	private void addToBuffer(final QVehicle veh, final double now) {
		// We are trying to modify this so it also works for vehicles different from size one.  The idea is that vehicles
		// _larger_ than size one can move as soon as at least one unit of flow or storage capacity is available.  
		// kai/mz/amit, mar'12
		
		// yy might make sense to just accumulate to "zero" and go into negative when something is used up.
		// kai/mz/amit, mar'12
		
		if (this.remainingflowCap >= 1.0) {
			this.remainingflowCap -= this.effectiveVehicleFlowConsumptionInPCU(veh); 
		}
		else if (this.flowcap_accumulate >= 1.0) {
			this.flowcap_accumulate -= this.effectiveVehicleFlowConsumptionInPCU(veh);
		}
		else {
			throw new IllegalStateException("Buffer of link " + this.getLink().getId() + " has no space left!");
		}
		this.buffer.add(veh);
		this.usedBufferStorageCapacity = this.usedBufferStorageCapacity + veh.getSizeInEquivalents();
		if (buffer.size() == 1) {
			this.bufferLastMovedTime = now;
			// (if there is one vehicle in the buffer now, there were zero vehicles in the buffer before.  in consequence,
			// need to reset the lastMovedTime.  If, in contrast, there was already a vehicle in the buffer before, we can
			// use the lastMovedTime that was (somehow) computed for that vehicle.)
		}
		this.getToNode().activateNode();
	}

	QVehicle popFirstVehicle() {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		QVehicle veh = this.buffer.poll();
		this.usedBufferStorageCapacity = this.usedBufferStorageCapacity - veh.getSizeInEquivalents();
		this.bufferLastMovedTime = now; // just in case there is another vehicle in the buffer that is now the new front-most
		this.linkEnterTimeMap.remove(veh);
		this.network.simEngine.getMobsim().getEventsManager().processEvent(new LinkLeaveEvent(now, veh.getId(), this.getLink().getId()));
		return veh;
	}

	QVehicle getFirstVehicle() {
		return this.buffer.peek();
	}

	double getLastMovementTimeOfFirstVehicle() {
		return this.bufferLastMovedTime;
	}

	public boolean hasGreenForToLink(Id<Link> toLinkId){
		if (this.qSignalizedItem != null){
			return this.qSignalizedItem.isLinkGreenForToLink(toLinkId);
		}
		return true; //the lane is not signalized and thus always green
	}

	public void setSignalStateAllTurningMoves(SignalGroupState state) {
		this.qSignalizedItem.setSignalStateAllTurningMoves(state);
		
		this.thisTimeStepGreen  = this.qSignalizedItem.isLinkGreen();
		// (this is only for capacity accumulation)
	}

	public void setSignalStateForTurningMove(SignalGroupState state, Id<Link> toLinkId) {
		if (!this.getToNode().getNode().getOutLinks().containsKey(toLinkId)){
			throw new IllegalArgumentException("ToLink " + toLinkId + " is not reachable from QLink Id " + this.getLink().getId());
		}
		this.qSignalizedItem.setSignalStateForTurningMove(state, toLinkId);

		this.thisTimeStepGreen = this.qSignalizedItem.isLinkGreen();
		// (this is only for capacity accumulation.  As soon as at least one turning relation is green, the "link" is considered
		// green).
	}

	public void setSignalized(boolean isSignalized) {
		this.qSignalizedItem  = new DefaultSignalizeableItem(this.getLink().getToNode().getOutLinks().keySet());
	}

	static class Hole extends QItem {
		private double earliestLinkEndTime ;

		@Override
		public double getEarliestLinkExitTime() {
			return earliestLinkEndTime;
		}

		@Override
		public void setEarliestLinkExitTime(double earliestLinkEndTime) {
			this.earliestLinkEndTime = earliestLinkEndTime;
		}
	}

	final void updateBufferCapacity() {
		this.remainingflowCap = this.flowCapacityPerTimeStep;
//		if (this.thisTimeStepGreen && this.flowcap_accumulate < 1.0 && this.hasBufferSpaceLeft()) {
			if (this.thisTimeStepGreen && this.flowcap_accumulate < 1.0 && this.isNotOfferingVehicle() ) {
			this.flowcap_accumulate += this.flowCapacityPerTimeStepFractionalPart;
		}
	}


	final boolean hasFlowCapacityLeftAndBufferSpace() {
		return (
				hasBufferSpaceLeft() 
				&& 
				((this.remainingflowCap >= 1.0) || (this.flowcap_accumulate >= 1.0))
				);
	}


	private boolean hasBufferSpaceLeft() {
		return usedBufferStorageCapacity < this.bufferStorageCapacity;
	}
	
	/*package*/ void registerDriverAgentWaitingForCar(final MobsimDriverAgent agent) {
		final Id<Vehicle> vehicleId = agent.getPlannedVehicleId() ;
		Queue<MobsimDriverAgent> queue = driversWaitingForCars.get( vehicleId );

		if ( queue == null ) {
			queue = new LinkedList<MobsimDriverAgent>();
			driversWaitingForCars.put( vehicleId , queue );
		}

		queue.add( agent );
	}

	/*package*/ void registerDriverAgentWaitingForPassengers(MobsimDriverAgent agent) {
		driversWaitingForPassengers.put(agent.getId(), agent);
	}

	/*package*/ MobsimAgent unregisterDriverAgentWaitingForPassengers(Id<Person> agentId) {
		return driversWaitingForPassengers.remove(agentId);
	}
	
	/*package*/ void registerPassengerAgentWaitingForCar(MobsimAgent agent, Id<Vehicle> vehicleId) {
		Set<MobsimAgent> passengers = passengersWaitingForCars.get(vehicleId);
		if (passengers == null) {
			passengers = new LinkedHashSet<MobsimAgent>();
			passengersWaitingForCars.put(vehicleId, passengers);
		}
		passengers.add(agent);
	}
	
	/*package*/ MobsimAgent unregisterPassengerAgentWaitingForCar(MobsimAgent agent, Id<Vehicle> vehicleId) {
		Set<MobsimAgent> passengers = passengersWaitingForCars.get(vehicleId);
		if (passengers != null && passengers.remove(agent)) return agent;
		else return null;
	}
	
	/*package*/ Set<MobsimAgent> getAgentsWaitingForCar(Id<Vehicle> vehicleId) {
		Set<MobsimAgent> set = passengersWaitingForCars.get(vehicleId);
		if (set != null) return Collections.unmodifiableSet(set);
		else return null;
	}
	
	/*package*/ void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
		this.additionalAgentsOnLink.put(planAgent.getId(), planAgent);
	}

	/*package*/ MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> mobsimAgentId) {
		return this.additionalAgentsOnLink.remove(mobsimAgentId);
	}
	
	/*package*/ void setNetElementActivator(NetElementActivator qSimEngineRunner) {
		this.netElementActivator = qSimEngineRunner;
	}
	
	@Override
	public Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	@Override
	public VisData getVisData() {
		return null;
	}
	
	/**
	 * Adds a vehicle to the link (i.e. the "queue"), called by
	 * {@link QNode#moveVehicleOverNode(QVehicle, QueueLane, double)}.
	 *
	 * @param veh
	 *          the vehicle
	 */
	private final static int[] TIMES = {0, 1, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
	private final static double[] MEANS_R = {22.32, 25.47, 21.01, 17.58, 16.23, 16.29, 18.53, 19.06, 18.75, 18.51, 18.12, 18.07, 18.05, 17.97, 16.25, 14.52, 15.68, 17.81, 18.93, 20.13, 22.13};
	private final static double[] STDS_R = {6.08, 8.55, 6.33, 6.05, 5.64, 5.57, 5.92, 6.16, 5.95, 6.00, 6.02, 5.86, 5.70, 5.71, 5.42, 5.34, 5.63, 5.99, 6.14, 6.46, 6.71};
	private final static double[] MEANS_S = {21.25, 21.92, 20.38, 19.30, 19.14, 19.20, 20.08, 20.61, 20.63, 20.54, 20.37, 20.32, 20.20, 20.11, 19.69, 19.03, 19.17, 19.74, 19.95, 20.15, 20.82};
	private final static double[] STDS_S = {2.42, 1.92, 2.30, 3.12, 2.70, 2.62, 2.53, 2.37, 2.31, 2.32, 2.40, 2.38, 2.38, 2.44, 2.49, 2.84, 2.92, 2.72, 2.62, 2.64, 2.42};
	private static final double MIN_SPEED_BUS = 10/3.6;
	final void addFromUpstream(final QVehicle veh) {
		double now = network.simEngine.getMobsim().getSimTimer().getTimeOfDay();
		activateLink();
		this.linkEnterTimeMap.put(veh, now);
		this.usedStorageCapacity += veh.getSizeInEquivalents();
		double speed = veh.getMaximumVelocity();
		if(speed==7.22) {
			if(link.getNumberOfLanes()>5)
				speed = 50/3.6;
			else if(link.getFreespeed()>4)
				speed = 40/3.6;
			else
				TIMES:
				for(int i=0; i<TIMES.length; i++)
					if(TIMES[i]==(int)(now/3600)%24)
						try {
							double r = MatsimRandom.getRandom().nextDouble();
							speed = new NormalDistribution(veh.getMaximumVelocity()-0.5556-(MEANS_S[i]>MEANS_R[i]?(MEANS_S[i]-MEANS_R[i])/3.6:0), STDS_R[i]*1.1/3.6).inverseCumulativeProbability(r);
							break TIMES;
						} catch (NotStrictlyPositiveException e) {
							e.printStackTrace();
						}
			speed = Math.max(MIN_SPEED_BUS, speed);
		}
		double vehicleTravelTime = this.length/speed;
		double earliestExitTime = now + Math.max(this.freespeedTravelTime, vehicleTravelTime);
		earliestExitTime = Math.floor(earliestExitTime);
		veh.setEarliestLinkExitTime(earliestExitTime);
		veh.setCurrentLink(this.getLink());
		this.vehQueue.add(veh);
		this.network.simEngine.getMobsim().getEventsManager().processEvent(
				new LinkEnterEvent(now, veh.getId(),
						this.getLink().getId()));
		if ( HOLES ) {
			holes.poll();
		}
	}
	
}
