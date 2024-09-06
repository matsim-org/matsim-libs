/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.TransitDriverAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineI.NetsimInternalInterface;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

/**
 * {@link QLinkI} is the interface; this here is an abstract class that contains implementation
 * of non-traffic-dynamics "infrastructure" provided by the link, such as parking or the logic for transit.
 *
 * @author nagel
 *
 */
abstract class AbstractQLink implements QLinkI {
	// yy The way forward might be to separate the "service link/network" from the "movement link/network".  The
	// service link would do things like park agents, accept additional agents on link, accept passengers or drivers waiting for
	// cars, etc.  Both the thread-based parallelization and the visualization then would have to treat those service links
	// separately, which feels fairly messy.  Maybe better leave things as they are.  kai, mar'16

	public enum HandleTransitStopResult {
		continue_driving, rehandle, accepted

	}

	private static final Logger log = LogManager.getLogger(AbstractQLink.class);

	private final Link link;

//	private final QNetwork qnetwork ;
	private NetElementActivationRegistry netElementActivationRegistry;
	// (NOTE: via the qnetwork you reach the QNetsimEngine.  That is the "global" thing.  In contrast, via the netElementActivator,
	// you reach the QNetsimEngineRunner.  That is the thread that runs the QLink.  Kai, mar'16

	// joint implementation for Customizable
	private final Map<String, Object> customAttributes = new HashMap<>();

//	private final Map<Id<Vehicle>, QVehicle> parkedVehicles = new LinkedHashMap<>(10);
	private final Map<Id<Vehicle>, QVehicle> parkedVehicles = new ConcurrentHashMap<>(10);

//	private final Map<Id<Person>, MobsimAgent> additionalAgentsOnLink = new LinkedHashMap<>();
	private final Map<Id<Person>, MobsimAgent> additionalAgentsOnLink = new ConcurrentHashMap<>( ) ;

	private final Map<Id<Vehicle>, Queue<MobsimDriverAgent>> driversWaitingForCars = new LinkedHashMap<>();

	private final Map<Id<Person>, MobsimDriverAgent> driversWaitingForPassengers = new LinkedHashMap<>();

	private final Map<Id<Vehicle>, Set<MobsimAgent>> passengersWaitingForCars = new LinkedHashMap<>();

	/**
	 * All vehicles from parkingList move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	private final Queue<QVehicle> waitingList = new LinkedList<>();

	private boolean active = false;

	private TransitQLink transitQLink;

	private final QNodeI toQNode ;

	private final NetsimEngineContext context;

	private final NetsimInternalInterface netsimEngine;
	private final LinkSpeedCalculator linkSpeedCalculator;
	private final VehicleHandler vehicleHandler;

	AbstractQLink(Link link, QNodeI toNode, NetsimEngineContext context, NetsimInternalInterface netsimEngine2, LinkSpeedCalculator linkSpeedCalculator, VehicleHandler vehicleHandler) {
		this.link = link ;
		this.toQNode = toNode ;
		this.context = context;
		this.netsimEngine = netsimEngine2;
		this.linkSpeedCalculator = linkSpeedCalculator;
		this.vehicleHandler = vehicleHandler;
	}

	@Override
	public QNodeI getToNode() {
		return toQNode ;
	}

	/**
	 * Links are active while (see checkForActivity()): () vehicles move on it; () vehicles wait to enter; () vehicles wait at the transit stop.
	 * Once all of those have left the link, the link is no longer active.  It then needs to be activated from the outside, which is done by
	 * this method.
	 * <br>
	 * seems ok as public interface function. kai, aug'15
	 */
	private void activateLink() {
		if (!this.active) {
			netElementActivationRegistry.registerLinkAsActive(this);
			this.active = true;
		}
		// This is a bit involved since we do not want to ask the registry in every time step if the link is already active.
	}
	private static int wrnCnt = 0 ;

	public final void addParkedVehicle(MobsimVehicle vehicle, boolean isInitial) {
		QVehicle qveh = (QVehicle) vehicle; // cast ok: when it gets here, it needs to be a qvehicle to work.

		if ( this.parkedVehicles.put(qveh.getId(), qveh) != null ) {
			if ( wrnCnt < 1 ) {
				wrnCnt++ ;
				log.warn( "existing vehicle on link was just overwritten by other vehicle with same ID.  Not clear what this means.  Continuing anyways ...") ;
				log.warn( Gbl.ONLYONCE ) ;
			}
		}
		qveh.setCurrentLink(this.link);

		if (isInitial) {
			vehicleHandler.handleInitialVehicleArrival(qveh, link);
		}
	}

	@Override
	public final void addParkedVehicle(MobsimVehicle vehicle) {
		addParkedVehicle(vehicle, true);
	}

	/* package */ final boolean letVehicleArrive(QVehicle qveh) {
		if (vehicleHandler.handleVehicleArrival(qveh, this.getLink())) {
			addParkedVehicle(qveh, false);
			double now = context.getSimTimer().getTimeOfDay();
			context.getEventsManager().processEvent(new VehicleLeavesTrafficEvent(now , qveh.getDriver().getId(),
					this.link.getId(), qveh.getId(), qveh.getDriver().getMode(), 1.0 ) ) ;

			this.netsimEngine.letVehicleArrive(qveh);
			makeVehicleAvailableToNextDriver(qveh);
			return true;
		}

		return false;
	}

	@Override
	public final QVehicle removeParkedVehicle(Id<Vehicle> vehicleId) {
		return this.parkedVehicles.remove(vehicleId);
	}

	@Override
	public QVehicle getParkedVehicle(Id<Vehicle> vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}

	private final void addDepartingVehicle(MobsimVehicle mvehicle) {
		QVehicle vehicle = (QVehicle) mvehicle;
		this.waitingList.add(vehicle);
		vehicle.setCurrentLink(this.getLink());
		this.activateLink();
		vehicleHandler.handleVehicleDeparture(vehicle, link);
	}

	@Override
	public void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
		this.additionalAgentsOnLink.put(planAgent.getId(), planAgent);
	}

	@Override
	public MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> mobsimAgentId) {
		return this.additionalAgentsOnLink.remove(mobsimAgentId);
	}

	@Override
	public Collection<MobsimAgent> getAdditionalAgentsOnLink() {
		return Collections.unmodifiableCollection( this.additionalAgentsOnLink.values());
	}

	@Override
	public void clearVehicles() {
		double now = context.getSimTimer().getTimeOfDay();

		/*
		 * Some agents might be present in multiple lists/maps.
		 * Ensure that only one stuck event per agent is created.
		 */
		Set<Id<Person>> stuckAgents = new HashSet<>();

		for (QVehicle veh : this.parkedVehicles.values()) {
			if (veh.getDriver() != null) {
				// skip transit driver which perform an activity while their vehicle is parked
				if (veh.getDriver().getState() != State.LEG) continue;

				if (stuckAgents.contains(veh.getDriver().getId())) continue;
				else stuckAgents.add(veh.getDriver().getId());

				context.getEventsManager().processEvent(
						new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));

				context.getEventsManager().processEvent(
						new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
				context.getAgentCounter().incLost();
				context.getAgentCounter().decLiving();
			}

			for (PassengerAgent passenger : veh.getPassengers()) {
				if (stuckAgents.contains(passenger.getId())) continue;
				else stuckAgents.add(passenger.getId());

				MobsimAgent mobsimAgent = (MobsimAgent) passenger;

				context.getEventsManager().processEvent(
						new PersonStuckEvent(now, mobsimAgent.getId(), veh.getCurrentLink().getId(), mobsimAgent.getMode()));
				context.getAgentCounter().incLost();
				context.getAgentCounter().decLiving();
			}
		}
		this.parkedVehicles.clear();
		for (MobsimAgent driver : driversWaitingForPassengers.values()) {
			if (stuckAgents.contains(driver.getId())) continue;
			else stuckAgents.add(driver.getId());

			context.getEventsManager().processEvent(
					new PersonStuckEvent(now, driver.getId(), driver.getCurrentLinkId(), driver.getMode()));
			context.getAgentCounter().incLost();
			context.getAgentCounter().decLiving();
		}
		driversWaitingForPassengers.clear();


		for (Queue<MobsimDriverAgent> queue : driversWaitingForCars.values()) {
			for (MobsimAgent driver : queue) {
				if (stuckAgents.contains(driver.getId())) continue;
				stuckAgents.add(driver.getId());

				context.getEventsManager().processEvent(
						new PersonStuckEvent(now, driver.getId(), driver.getCurrentLinkId(), driver.getMode()));
				context.getAgentCounter().incLost();
				context.getAgentCounter().decLiving();
			}
		}
		driversWaitingForCars.clear();
		for (Set<MobsimAgent> passengers : passengersWaitingForCars.values()) {
			for (MobsimAgent passenger : passengers) {
				if (stuckAgents.contains(passenger.getId())) continue;
				else stuckAgents.add(passenger.getId());

				context.getEventsManager().processEvent(
						new PersonStuckEvent(now, passenger.getId(), passenger.getCurrentLinkId(), passenger.getMode()));
				this.context.getAgentCounter().incLost();
				this.context.getAgentCounter().decLiving();
			}
		}
		this.passengersWaitingForCars.clear();

		for (QVehicle veh : this.waitingList) {
			if (stuckAgents.contains(veh.getDriver().getId())) continue;
			else stuckAgents.add(veh.getDriver().getId());

			this.context.getEventsManager().processEvent(
					new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));

			this.context.getEventsManager().processEvent(
					new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.context.getAgentCounter().incLost();
			this.context.getAgentCounter().decLiving();
		}
		this.waitingList.clear();
	}

	void makeVehicleAvailableToNextDriver(QVehicle veh) {

		// this would (presumably) be the place where the "nature" of a vehicle could be changed (in the sense of PAVE), e.g. to
		// a freight vehicle, or to an autonomous vehicle that can be sent around the block or home.  However, this would
		// necessitate some agent-like logic also for the vehicle: vehicle behavior given by driver as long as driver in
		// vehicle; vehicle behavior given by something else afterwards.  Parking search is structurally somewhat similar; how
		// was that implemented?  kai, oct'18

		/*
		 * Insert waiting passengers into vehicle.
		 */
		Id<Vehicle> vehicleId = veh.getId();
		Set<MobsimAgent> passengers = this.passengersWaitingForCars.get(vehicleId);
		if (passengers != null) {
			// Copy set of passengers since otherwise we would modify it concurrently.
			List<MobsimAgent> passengersToHandle = new ArrayList<>(passengers);
			for (MobsimAgent passenger : passengersToHandle) {
				this.unregisterPassengerAgentWaitingForCar(passenger, vehicleId);
				this.insertPassengerIntoVehicle(passenger, vehicleId);
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
			this.letVehicleDepart(veh);
		}
	}

	@Override
	public final void letVehicleDepart(QVehicle vehicle) {
		double now = context.getSimTimer().getTimeOfDay();

		MobsimDriverAgent driver = vehicle.getDriver();
		if (driver == null) throw new RuntimeException("Vehicle cannot depart without a driver!");

		EventsManager eventsManager = context.getEventsManager();
		eventsManager.processEvent(new PersonEntersVehicleEvent(now, driver.getId(), vehicle.getId()));
		this.addDepartingVehicle(vehicle);
	}

	/*
	 * If the vehicle is parked at the current link, insert the passenger,
	 * create an enter event and return true. Otherwise add the agent to
	 * the waiting list and return false.
	 */
	@Override
	public final boolean insertPassengerIntoVehicle(MobsimAgent passenger, Id<Vehicle> vehicleId) {
		double now = context.getSimTimer().getTimeOfDay();

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
			EventsManager eventsManager = context.getEventsManager();
			eventsManager.processEvent(new PersonEntersVehicleEvent(now, passenger.getId(), vehicle.getId()));
			// TODO: allow setting passenger's currentLinkId to null

			return true;
		}
	}

	@Override
	public QVehicle getVehicle(Id<Vehicle> vehicleId) {
		// yyyy ?? does same as getParkedVehicle(...) ??  kai, mar'16

		QVehicle ret = this.parkedVehicles.get(vehicleId);
		return ret;
	}

	@Override
	public final Collection<MobsimVehicle> getAllVehicles() {
		Collection<MobsimVehicle> vehicles = this.getAllNonParkedVehicles();
		vehicles.addAll(this.parkedVehicles.values());
		return vehicles;
	}

	@Override
	public final Map<String, Object> getCustomAttributes() {
		return customAttributes;
	}

	/*package*/ void setNetElementActivationRegistry(NetElementActivationRegistry qSimEngineRunner) {
		this.netElementActivationRegistry = qSimEngineRunner;
	}

	@Override
	public void registerDriverAgentWaitingForCar(final MobsimDriverAgent agent) {
		final Id<Vehicle> vehicleId = agent.getPlannedVehicleId() ;
		Queue<MobsimDriverAgent> queue = driversWaitingForCars.get( vehicleId );

		if ( queue == null ) {
			queue = new LinkedList<>();
			driversWaitingForCars.put( vehicleId , queue );
		}

		queue.add( agent );
	}

	@Override
	public void registerDriverAgentWaitingForPassengers(MobsimDriverAgent agent) {
		driversWaitingForPassengers.put(agent.getId(), agent);
	}

	@Override
	public MobsimAgent unregisterDriverAgentWaitingForPassengers(Id<Person> agentId) {
		return driversWaitingForPassengers.remove(agentId);
	}

	@Override
	public void registerPassengerAgentWaitingForCar(MobsimAgent agent, Id<Vehicle> vehicleId) {
		Set<MobsimAgent> passengers = passengersWaitingForCars.get(vehicleId);
		if (passengers == null) {
			passengers = new LinkedHashSet<>();
			passengersWaitingForCars.put(vehicleId, passengers);
		}
		passengers.add(agent);
	}

	@Override
	public MobsimAgent unregisterPassengerAgentWaitingForCar(MobsimAgent agent, Id<Vehicle> vehicleId) {
		Set<MobsimAgent> passengers = passengersWaitingForCars.get(vehicleId);
		if (passengers != null && passengers.remove(agent)) return agent;
		else return null;
	}

	@Override
	public Set<MobsimAgent> getAgentsWaitingForCar(Id<Vehicle> vehicleId) {
		Set<MobsimAgent> set = passengersWaitingForCars.get(vehicleId);
		if (set != null) return Collections.unmodifiableSet(set);
		else return null;
	}

	@Override
	public Link getLink() {
		return link;
	}

	boolean isActive() {
		return active;
	}

	void setActive(boolean active) {
		this.active = active;
	}

	Queue<QVehicle> getWaitingList() {
		return waitingList;
	}

	TransitQLink getTransitQLink() {
		return transitQLink;
	}

	void setTransitQLink(TransitQLink transitQLink) {
		this.transitQLink = transitQLink;
	}

	/**
	 * The idea here is to keep some control over what the implementations of QLaneI have access to.  And maybe reduce
	 * it over time, so that they become more encapsulated.  kai, feb'18
	 */
	public final class QLinkInternalInterface {

		public QNodeI getToNodeQ() {
			return AbstractQLink.this.toQNode ;
		}
		public Node getToNode() {
			return AbstractQLink.this.link.getToNode() ;
		}

		public double getFreespeed() {
			// yyyy does it make sense to provide the method without time?  kai, feb'18
			return AbstractQLink.this.link.getFreespeed() ;
		}

		public Id<Link> getId() {
			return AbstractQLink.this.link.getId() ;
		}

		public HandleTransitStopResult handleTransitStop(double now, QVehicle veh, TransitDriverAgent driver, Id<Link> linkId) {
			// yy now would not be needed as an argument. kai, feb'18
			// yy linkId would not be needed as an argument. kai, feb'18
			return AbstractQLink.this.transitQLink.handleTransitStop(now, veh, driver, linkId) ;
		}

		public void addParkedVehicle(QVehicle veh) {
			AbstractQLink.this.addParkedVehicle(veh);
		}

		public boolean letVehicleArrive(QVehicle veh) {
			return AbstractQLink.this.letVehicleArrive(veh);
		}

		public void makeVehicleAvailableToNextDriver(QVehicle veh) {
			AbstractQLink.this.makeVehicleAvailableToNextDriver(veh);
		}

		public void activateLink() {
			AbstractQLink.this.activateLink();
		}

		public double getMaximumVelocityFromLinkSpeedCalculator(QVehicle veh, double now) {
			final LinkSpeedCalculator linkSpeedCalculator = AbstractQLink.this.linkSpeedCalculator;
			Gbl.assertNotNull(linkSpeedCalculator);
			return linkSpeedCalculator.getMaximumVelocity(veh, AbstractQLink.this.link, now) ;
		}

		public void setCurrentLinkToVehicle(QVehicle veh) {
			veh.setCurrentLink( AbstractQLink.this.link );
		}

		public QLaneI getAcceptingQLane() {
			return AbstractQLink.this.getAcceptingQLane() ;
		}

		public List<QLaneI> getOfferingQLanes() {
			return AbstractQLink.this.getOfferingQLanes() ;
		}

		public Node getFromNode() {
			return AbstractQLink.this.link.getFromNode() ;
		}

		public double getFreespeed(double now) {
			return AbstractQLink.this.link.getFreespeed(now) ;
		}

		public int getNumberOfLanesAsInt(double now) {
			return NetworkUtils.getNumberOfLanesAsInt(now,AbstractQLink.this.link) ;
		}

		public Link getLink() {
			return AbstractQLink.this.link;
		}
	}
	private final QLinkInternalInterface qLinkInternalInterface = new QLinkInternalInterface() ;
	public final QLinkInternalInterface getInternalInterface() {
		return qLinkInternalInterface ;
	}

}
