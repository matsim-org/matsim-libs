/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.ev.charging;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

public class ChargingWithQueueingLogic implements ChargingLogic {
	protected final ChargerSpecification charger;
	private final EventsManager eventsManager;
	private final ChargingPriority priority;
	private final ChargerPower chargerPower;

	private final Map<Id<Vehicle>, ChargingVehicle> pluggedVehicles = new LinkedHashMap<>();
	private final Queue<ChargingVehicle> queuedVehicles = new LinkedList<>();
	private final Queue<ChargingVehicle> arrivingVehicles = new LinkedBlockingQueue<>();
	private final Map<Id<Vehicle>, ChargingListener> listeners = new LinkedHashMap<>();

	public ChargingWithQueueingLogic(ChargerSpecification charger,  EventsManager eventsManager, ChargingPriority priority, ChargerPower chargerPower) {
		this.charger = Objects.requireNonNull(charger);
		this.eventsManager = Objects.requireNonNull(eventsManager);
		this.priority = priority;
		this.chargerPower = chargerPower;
	}

	@Override
	public void chargeVehicles(double chargePeriod, double now) {
		Iterator<ChargingVehicle> cvIter = pluggedVehicles.values().iterator();
		
		chargerPower.update(now);
		
		while (cvIter.hasNext()) {
			ChargingVehicle cv = cvIter.next();

			// how much the vehicle would like to charge
			double requestedEnergy = cv.strategy().calcRemainingEnergyToCharge();

			// current state of the vehicle
			double currentVehicleCharge = cv.ev().getBattery().getCharge();

			// maximum power with which the vehicle can charge
			double chargingPower = cv.ev().getChargingPower().calcChargingPower(charger);
			
			// with fast charging, we charge around 4% of SOC per minute,
			// so when updating SOC every 10 seconds, SOC increases by less then 1%
			double chargedEnergy = chargingPower * chargePeriod;

			// limited by requested energy
			chargedEnergy = Math.min(chargedEnergy, requestedEnergy);

			// limited by available energy that can be given by charger in that period
			chargedEnergy = Math.min(chargedEnergy, chargerPower.calcMaximumEnergyToCharge(now, cv.ev()));

			// limited by battery capacity
			chargedEnergy = Math.min(chargedEnergy, cv.ev().getBattery().getCapacity() - currentVehicleCharge);

			double updatedCharge = currentVehicleCharge + chargedEnergy; // cannot exceed battery capacity
			cv.ev().getBattery().setCharge(updatedCharge);

			eventsManager.processEvent(new EnergyChargedEvent(now, charger.getId(), cv.ev().getId(), chargedEnergy, updatedCharge));
			chargerPower.consumeEnergy(chargedEnergy);
			
			if (cv.strategy().isChargingCompleted()) {
				cvIter.remove();
				eventsManager.processEvent(new ChargingEndEvent(now, charger.getId(), cv.ev().getId(), cv.ev().getBattery().getCharge()));
				chargerPower.unplugVehicle(now, cv.ev());
				listeners.remove(cv.ev().getId()).notifyChargingEnded(cv.ev(), now);
			}
		}

		var queuedVehiclesIter = queuedVehicles.iterator();
		while (queuedVehiclesIter.hasNext() && pluggedVehicles.size() < charger.getPlugCount()) {
			var cv = queuedVehiclesIter.next();
			if (plugVehicle(cv, now)) {
				queuedVehiclesIter.remove();
			}
		}

		var arrivingVehiclesIter = arrivingVehicles.iterator();
		while (arrivingVehiclesIter.hasNext()) {
			var cv = arrivingVehiclesIter.next();
			if (pluggedVehicles.size() >= charger.getPlugCount() || !plugVehicle(cv, now)) {
				queueVehicle(cv, now);
			}
		}
		arrivingVehicles.clear();
	}

	@Override
	public void addVehicle(ElectricVehicle ev, ChargingStrategy strategy, double now) {
		addVehicle(ev, strategy, new ChargingListener() {
		}, now);
	}

	@Override
	public void addVehicle(ElectricVehicle ev, ChargingStrategy strategy, ChargingListener chargingListener, double now) {
		arrivingVehicles.add(new ChargingVehicle(ev, strategy));
		listeners.put(ev.getId(), chargingListener);
	}

	@Override
	public void removeVehicle(ElectricVehicle ev, double now) {
		if (pluggedVehicles.remove(ev.getId()) != null) {// successfully removed
			eventsManager.processEvent(new ChargingEndEvent(now, charger.getId(), ev.getId(), ev.getBattery().getCharge()));
			listeners.remove(ev.getId()).notifyChargingEnded(ev, now);

			var queuedVehiclesIter = queuedVehicles.iterator();
			while (queuedVehiclesIter.hasNext()) {
				var queuedVehicle = queuedVehiclesIter.next();
				if (plugVehicle(queuedVehicle, now)) {
					queuedVehiclesIter.remove();
					break;
				}
			}
		} else {
			var queuedVehiclesIter = queuedVehicles.iterator();
			while (queuedVehiclesIter.hasNext()) {
				var queuedVehicle = queuedVehiclesIter.next();

				if (queuedVehicle.ev() == ev) {
					queuedVehiclesIter.remove();
					eventsManager.processEvent(new QuitQueueAtChargerEvent(now, charger.getId(), ev.getId()));
					return; // found the vehicle
				}
			}

			throw new IllegalStateException(String.format("Vehicle (%s) is neither queued nor plugged at charger (%s)", ev.getId(),
				charger.getId()));
		}
	}

	private void queueVehicle(ChargingVehicle cv, double now) {
		queuedVehicles.add(cv);
		eventsManager.processEvent(new QueuedAtChargerEvent(now, charger.getId(), cv.ev().getId()));
		listeners.get(cv.ev().getId()).notifyVehicleQueued(cv.ev(), now);
	}

	private boolean plugVehicle(ChargingVehicle cv, double now) {
		assert pluggedVehicles.size() < charger.getPlugCount();

		if (!priority.requestPlugNext(cv, now)) {
			return false;
		}

		if (pluggedVehicles.put(cv.ev().getId(), cv) != null) {
			throw new IllegalArgumentException();
		}

		eventsManager.processEvent(new ChargingStartEvent(now, charger.getId(), cv.ev().getId(), cv.ev().getBattery().getCharge()));
		chargerPower.plugVehicle(now, cv.ev());
		listeners.get(cv.ev().getId()).notifyChargingStarted(cv.ev(), now);

		return true;
	}

	private final Collection<ChargingVehicle> unmodifiablePluggedVehicles = Collections.unmodifiableCollection(pluggedVehicles.values());

	@Override
	public Collection<ChargingVehicle> getPluggedVehicles() {
		return unmodifiablePluggedVehicles;
	}

	private final Collection<ChargingVehicle> unmodifiableQueuedVehicles = Collections.unmodifiableCollection(queuedVehicles);

	@Override
	public Collection<ChargingVehicle> getQueuedVehicles() {
		return unmodifiableQueuedVehicles;
	}

	static public class Factory implements ChargingLogic.Factory {
		private final EventsManager eventsManager;
		private final ChargingPriority.Factory chargingPriorityFactory;
		private final ChargerPower.Factory chargerPowerFactory;

		public Factory(EventsManager eventsManager, ChargingPriority.Factory chargingPriorityFactory, ChargerPower.Factory chargerPowerFactory) {
			this.eventsManager = eventsManager;
			this.chargingPriorityFactory = chargingPriorityFactory;
			this.chargerPowerFactory = chargerPowerFactory;
		}

		@Override
		public ChargingLogic create(ChargerSpecification charger) {
			return new ChargingWithQueueingLogic(charger,  eventsManager, chargingPriorityFactory.create(charger), chargerPowerFactory.create(charger));
		}
	}
}
