package org.matsim.contrib.transEnergySim.events;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

public abstract class ChargingEventManager extends EventManager<EVSimEngineEventHandler> {
	protected EventManagerDelegate delegate;
	HashMap<Id<Person>, Id<Vehicle>> personToVehicleMapping;

	//TODO: create global method for mapping person to vehicle. it should be able to overwrite it (e.g. one to one mapping of ids).
	public ChargingEventManager(HashMap<Id<Person>, Id<Vehicle>> personToVehicleMapping, Controler controler,
			HashSet<String> travelModeFilter) {
		this.personToVehicleMapping = personToVehicleMapping;
		delegate = new EventManagerDelegate(this, personToVehicleMapping, travelModeFilter);
		controler.getEvents().addHandler(delegate);
	}

	public void processVehicleDepartureEvent(double time, Id<Vehicle> vehicleId) {
		for (EVSimEngineEventHandler handler : handlers) {
			handler.handleVehicleDepartureEvent(time, vehicleId);
		}
	}

	public void processVehicleArrivalEvent(double time, Id<Vehicle> vehicleId) {
		for (EVSimEngineEventHandler handler : handlers) {
			handler.handleVehicleArrivalEvent(time, vehicleId);
		}
	}

	public void processTimeStep(double time) {
		for (EVSimEngineEventHandler handler : handlers) {
			handler.handleTimeStep(time);
			handler.processTimeStep(time);
		}
	}
	
	private class EventManagerDelegate implements PersonArrivalEventHandler, PersonDepartureEventHandler, MobsimEngine {

		private ChargingEventManager chargingEventManager;
		private HashMap<Id<Person>, Id<Vehicle>> personToVehicleMapping;
		private HashSet<String> travelModeFilter;

		public EventManagerDelegate(ChargingEventManager chargingEventManager, HashMap<Id<Person>, Id<Vehicle>> personToVehicleMapping,
				HashSet<String> travelModeFilter) {
			this.chargingEventManager = chargingEventManager;
			this.personToVehicleMapping = personToVehicleMapping;
			this.travelModeFilter = travelModeFilter;
		}

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub

		}

		@Override
		public void doSimStep(double time) {
			chargingEventManager.processTimeStep(time);
		}

		@Override
		public void onPrepareSim() {
			// TODO Auto-generated method stub

		}

		@Override
		public void afterSim() {
			// TODO Auto-generated method stub

		}

		@Override
		public void setInternalInterface(InternalInterface internalInterface) {
			// TODO Auto-generated method stub

		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			Id<Vehicle> vehicleId = getVehicleId(event.getPersonId());
			
			if (travelModeFilter.contains(event.getLegMode())) {
				chargingEventManager.processVehicleDepartureEvent(event.getTime(), vehicleId);
			}
		}

		private Id<Vehicle> getVehicleId(Id<Person> id) {
			Id<Vehicle> vehicleId;
			if (personToVehicleMapping==null){
				vehicleId=Id.create(id, Vehicle.class);
			} else {
				vehicleId=personToVehicleMapping.get(id);
			}
			return vehicleId;
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			Id<Vehicle> vehicleId = getVehicleId(event.getPersonId());
			
			if (travelModeFilter.contains(event.getLegMode())) {
				chargingEventManager.processVehicleArrivalEvent(event.getTime(), vehicleId);
			}
		}

	}

}
