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

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.fleet.ElectricVehicle;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.core.api.experimental.events.EventsManager;

import com.google.common.base.Preconditions;

public class ChargingWithQueueingLogic implements ChargingLogic {
	private final ChargerSpecification charger;
	private final ChargingStrategy chargingStrategy;
	private final EventsManager eventsManager;

	private final Map<Id<ElectricVehicle>, ElectricVehicle> pluggedVehicles = new LinkedHashMap<>();
	private final Queue<ElectricVehicle> queuedVehicles = new LinkedList<>();
	private final Map<Id<ElectricVehicle>, ChargingListener> listeners = new LinkedHashMap<>();

	public ChargingWithQueueingLogic(ChargerSpecification charger, ChargingStrategy chargingStrategy,
			EventsManager eventsManager) {
		this.chargingStrategy = Objects.requireNonNull(chargingStrategy);
		this.charger = Objects.requireNonNull(charger);
		this.eventsManager = Objects.requireNonNull(eventsManager);
	}

	@Override
	public void chargeVehicles(double chargePeriod, double now) {
		Iterator<ElectricVehicle> evIter = pluggedVehicles.values().iterator();
		while (evIter.hasNext()) {
			ElectricVehicle ev = evIter.next();
			// with fast charging, we charge around 4% of SOC per minute,
			// so when updating SOC every 10 seconds, SOC increases by less then 1%
			ev.getBattery().changeSoc(ev.getChargingPower().calcChargingPower(charger) * chargePeriod);

			if (chargingStrategy.isChargingCompleted(ev)) {
				evIter.remove();
				eventsManager.processEvent(
						new ChargingEndEvent(now, charger.getId(), ev.getId(), ev.getBattery().getSoc()));
				listeners.remove(ev.getId()).notifyChargingEnded(ev, now);
			}
		}

		int queuedToPluggedCount = Math.min(queuedVehicles.size(), charger.getPlugCount() - pluggedVehicles.size());
		for (int i = 0; i < queuedToPluggedCount; i++) {
			plugVehicle(queuedVehicles.poll(), now);
		}
	}

	@Override
	public void addVehicle(ElectricVehicle ev, double now) {
		addVehicle(ev, new ChargingListener() {}, now);
	}

	@Override
	public void addVehicle(ElectricVehicle ev, ChargingListener chargingListener, double now) {
		listeners.put(ev.getId(), chargingListener);
		if (pluggedVehicles.size() < charger.getPlugCount()) {
			plugVehicle(ev, now);
		} else {
			queueVehicle(ev, now);
		}
	}

	@Override
	public void removeVehicle(ElectricVehicle ev, double now) {
		if (pluggedVehicles.remove(ev.getId()) != null) {// successfully removed
			eventsManager.processEvent(
					new ChargingEndEvent(now, charger.getId(), ev.getId(), ev.getBattery().getSoc()));
			listeners.remove(ev.getId()).notifyChargingEnded(ev, now);

			if (!queuedVehicles.isEmpty()) {
				plugVehicle(queuedVehicles.poll(), now);
			}
		} else {
			// make sure ev was in the queue
			Preconditions.checkState(queuedVehicles.remove(ev),
					"Vehicle (%s) is neither queued nor plugged at charger (%s)", ev.getId(), charger.getId());
			eventsManager.processEvent(new QuitQueueAtChargerEvent(now, charger.getId(), ev.getId()));
		}
	}

	private void queueVehicle(ElectricVehicle ev, double now) {
		queuedVehicles.add(ev);
		eventsManager.processEvent(new QueuedAtChargerEvent(now, charger.getId(), ev.getId()));
		listeners.get(ev.getId()).notifyVehicleQueued(ev, now);
	}

	private void plugVehicle(ElectricVehicle ev, double now) {
		if (pluggedVehicles.put(ev.getId(), ev) != null) {
			throw new IllegalArgumentException();
		}
		eventsManager.processEvent(new ChargingStartEvent(now, charger.getId(), ev.getId(), charger.getChargerType(),
				ev.getBattery().getSoc()));
		listeners.get(ev.getId()).notifyChargingStarted(ev, now);
	}

	private final Collection<ElectricVehicle> unmodifiablePluggedVehicles = Collections.unmodifiableCollection(
			pluggedVehicles.values());

	@Override
	public Collection<ElectricVehicle> getPluggedVehicles() {
		return unmodifiablePluggedVehicles;
	}

	private final Collection<ElectricVehicle> unmodifiableQueuedVehicles = Collections.unmodifiableCollection(
			queuedVehicles);

	@Override
	public Collection<ElectricVehicle> getQueuedVehicles() {
		return unmodifiableQueuedVehicles;
	}

	@Override
	public ChargingStrategy getChargingStrategy() {
		return chargingStrategy;
	}
}
