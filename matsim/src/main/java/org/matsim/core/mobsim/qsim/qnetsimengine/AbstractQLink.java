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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;

/**
 * QLinkInternalI is the interface; this here is an abstract class that contains implementation
 * of non-traffic-dynamics "infrastructure" provided by the link, such as parking or the logic for transit.
 *
 * @author nagel
 *
 */
abstract class AbstractQLink extends QLinkI {

	public enum HandleTransitStopResult {
		continue_driving, rehandle, accepted

	}

	private static final Logger log = Logger.getLogger(AbstractQLink.class);

	final Link link;

	final QNetwork network;

	// joint implementation for Customizable
	private final Map<String, Object> customAttributes = new HashMap<>();

	private final Map<Id<Vehicle>, QVehicle> parkedVehicles = new LinkedHashMap<>(10);

	private final Map<Id<Person>, MobsimAgent> additionalAgentsOnLink = new LinkedHashMap<>();

	private final Map<Id<Vehicle>, Queue<MobsimDriverAgent>> driversWaitingForCars = new LinkedHashMap<>();

	private final Map<Id<Person>, MobsimDriverAgent> driversWaitingForPassengers = new LinkedHashMap<>();

	// vehicleId
	private final Map<Id<Vehicle>, Set<MobsimAgent>> passengersWaitingForCars = new LinkedHashMap<>();

	/**
	 * All vehicles from parkingList move to the waitingList as soon as their time
	 * has come. They are then filled into the vehQueue, depending on free space
	 * in the vehQueue
	 */
	/*package*/ final Queue<QVehicle> waitingList = new LinkedList<>();

	private NetElementActivator netElementActivator;

	/*package*/ final boolean insertingWaitingVehiclesBeforeDrivingVehicles;

	/**
	 * A list containing all transit vehicles that are at a stop but not
	 * blocking other traffic on the lane.
	 */

	boolean active = false;

	TransitQLink transitQLink;

	AbstractQLink(Link link, QNetwork network) {
		this.link = link ;
		this.network = network;
		this.insertingWaitingVehiclesBeforeDrivingVehicles =
				network.simEngine.getMobsim().getScenario().getConfig().qsim().isInsertingWaitingVehiclesBeforeDrivingVehicles() ;
	}

	/** 
	 * Links are active while (see checkForActivity()): () vehicles move on it; () vehicles wait to enter; () vehicles wait at the transit stop.
	 * Once all of those have left the link, the link is no longer active.  It then needs to be activated from the outside, which is done by
	 * this method.
	 * <br>
	 * seems ok as public interface function. kai, aug'15
	 */
	void activateLink() {
		if (!this.active) {
			netElementActivator.activateLink(this);
			this.active = true;
		}
	}
	private static int wrnCnt = 0 ;
	@Override
	/*package*/ final void addParkedVehicle(MobsimVehicle vehicle) {
		QVehicle qveh = (QVehicle) vehicle; // cast ok: when it gets here, it needs to be a qvehicle to work.
		
		if ( this.parkedVehicles.put(qveh.getId(), qveh) != null ) {
			if ( wrnCnt < 1 ) {
				wrnCnt++ ;
				log.warn( "existing vehicle on link was just overwritten by other vehicle with same ID.  Not clear what this means.  Continuing anyways ...") ;
				log.warn( Gbl.ONLYONCE ) ;
			}
		}
		qveh.setCurrentLink(this.link);
	}
	
	/* package */ final void letVehicleArrive(QVehicle qveh) {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();;
		this.network.simEngine.getMobsim().getEventsManager().processEvent(new VehicleLeavesTrafficEvent(now , qveh.getDriver().getId(), 
				this.link.getId(), qveh.getId(), qveh.getDriver().getMode(), 1.0 ) ) ;
		
		this.network.simEngine.letVehicleArrive(qveh);
	}

	@Override
	/*package*/ final QVehicle removeParkedVehicle(Id<Vehicle> vehicleId) {
		return this.parkedVehicles.remove(vehicleId);
	}

	@Override
	/*package*/ QVehicle getParkedVehicle(Id<Vehicle> vehicleId) {
		return this.parkedVehicles.get(vehicleId);
	}

	private final void addDepartingVehicle(MobsimVehicle mvehicle) {
		QVehicle vehicle = (QVehicle) mvehicle;
		this.waitingList.add(vehicle);
		vehicle.setCurrentLink(this.getLink());
		this.activateLink();
	}

	@Override
	/*package*/ void registerAdditionalAgentOnLink(MobsimAgent planAgent) {
		this.additionalAgentsOnLink.put(planAgent.getId(), planAgent);
	}

	@Override
	/*package*/ MobsimAgent unregisterAdditionalAgentOnLink(Id<Person> mobsimAgentId) {
		return this.additionalAgentsOnLink.remove(mobsimAgentId);
	}

	@Override
	/*package*/ Collection<MobsimAgent> getAdditionalAgentsOnLink() {
		return Collections.unmodifiableCollection( this.additionalAgentsOnLink.values());
	}
	


	@Override
	void clearVehicles() {
		double now = this.network.simEngine.getMobsim().getSimTimer().getTimeOfDay();

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

				this.network.simEngine.getMobsim().getEventsManager().processEvent(
						new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
				
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
					new VehicleAbortsEvent(now, veh.getId(), veh.getCurrentLink().getId()));
			
			this.network.simEngine.getMobsim().getEventsManager().processEvent(
					new PersonStuckEvent(now, veh.getDriver().getId(), veh.getCurrentLink().getId(), veh.getDriver().getMode()));
			this.network.simEngine.getMobsim().getAgentCounter().incLost();
			this.network.simEngine.getMobsim().getAgentCounter().decLiving();
		}
		this.waitingList.clear();
	}

	void makeVehicleAvailableToNextDriver(QVehicle veh, double now) {

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

	@Override
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
	@Override
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

	@Override
	/*package*/ QVehicle getVehicle(Id<Vehicle> vehicleId) {
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

	/*package*/ void setNetElementActivator(NetElementActivator qSimEngineRunner) {
		this.netElementActivator = qSimEngineRunner;
	}

	@Override
	/*package*/ void registerDriverAgentWaitingForCar(final MobsimDriverAgent agent) {
		final Id<Vehicle> vehicleId = agent.getPlannedVehicleId() ;
		Queue<MobsimDriverAgent> queue = driversWaitingForCars.get( vehicleId );

		if ( queue == null ) {
			queue = new LinkedList<>();
			driversWaitingForCars.put( vehicleId , queue );
		}

		queue.add( agent );
	}

	@Override
	/*package*/ void registerDriverAgentWaitingForPassengers(MobsimDriverAgent agent) {
		driversWaitingForPassengers.put(agent.getId(), agent);
	}

	@Override
	/*package*/ MobsimAgent unregisterDriverAgentWaitingForPassengers(Id<Person> agentId) {
		return driversWaitingForPassengers.remove(agentId);
	}

	@Override
	/*package*/ void registerPassengerAgentWaitingForCar(MobsimAgent agent, Id<Vehicle> vehicleId) {
		Set<MobsimAgent> passengers = passengersWaitingForCars.get(vehicleId);
		if (passengers == null) {
			passengers = new LinkedHashSet<>();
			passengersWaitingForCars.put(vehicleId, passengers);
		}
		passengers.add(agent);
	}

	@Override
	/*package*/ MobsimAgent unregisterPassengerAgentWaitingForCar(MobsimAgent agent, Id<Vehicle> vehicleId) {
		Set<MobsimAgent> passengers = passengersWaitingForCars.get(vehicleId);
		if (passengers != null && passengers.remove(agent)) return agent;
		else return null;
	}

	@Override
	/*package*/ Set<MobsimAgent> getAgentsWaitingForCar(Id<Vehicle> vehicleId) {
		Set<MobsimAgent> set = passengersWaitingForCars.get(vehicleId);
		if (set != null) return Collections.unmodifiableSet(set);
		else return null;
	}

}
