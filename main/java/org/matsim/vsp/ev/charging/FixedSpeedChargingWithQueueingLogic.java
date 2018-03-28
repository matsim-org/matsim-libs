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

package org.matsim.vsp.ev.charging;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;
import org.matsim.vsp.ev.data.Charger;
import org.matsim.vsp.ev.data.ElectricVehicle;

import com.google.common.collect.Maps;

public class FixedSpeedChargingWithQueueingLogic implements ChargingLogic {
	private final ChargingStrategy chargingStrategy;
	protected final Charger charger;
	protected final Map<Id<Vehicle>, ElectricVehicle> pluggedVehicles;
	protected final Queue<ElectricVehicle> queuedVehicles = new LinkedList<>();

	private EventsManager eventsManager;

	public FixedSpeedChargingWithQueueingLogic(Charger charger, ChargingStrategy chargingStrategy) {
		this.charger = charger;
		this.chargingStrategy = chargingStrategy;
		pluggedVehicles = Maps.newHashMapWithExpectedSize(charger.getPlugs());
	}

	@Override
	public void initEventsHandling(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}

	@Override
	public void chargeVehicles(double chargePeriod, double now) {
		Iterator<ElectricVehicle> evIter = pluggedVehicles.values().iterator();
		while (evIter.hasNext()) {
			ElectricVehicle ev = evIter.next();
			// with fast charging, we charge around 4% of SOC per minute,
			// so when updating SOC every 10 seconds, SOC increases by less then 1%
			chargingStrategy.chargeVehicle(ev, chargePeriod);

			if (chargingStrategy.isChargingCompleted(ev)) {
				evIter.remove();
				eventsManager.processEvent(new ChargingEndEvent(now, charger.getId(), ev.getId()));
				notifyChargingEnded(ev, now);
			}
		}

		int fromQueuedToPluggedCount = Math.min(queuedVehicles.size(), charger.getPlugs() - pluggedVehicles.size());
		for (int i = 0; i < fromQueuedToPluggedCount; i++) {
			plugVehicle(queuedVehicles.poll(), now);
		}
	}

	@Override
	public void addVehicle(ElectricVehicle ev, double now) {
		if (pluggedVehicles.size() < charger.getPlugs()) {
			plugVehicle(ev, now);
		} else {
			queueVehicle(ev, now);
		}
	}

	@Override
	public void removeVehicle(ElectricVehicle ev, double now) {
		if (pluggedVehicles.remove(ev.getId()) != null) {// successfully removed
			eventsManager.processEvent(new ChargingEndEvent(now, charger.getId(), ev.getId()));
			notifyChargingEnded(ev, now);

			if (!queuedVehicles.isEmpty()) {
				plugVehicle(queuedVehicles.poll(), now);
			}
		} else if (!queuedVehicles.remove(ev)) {// neither plugged nor queued
			throw new IllegalArgumentException(
					"Vehicle: " + ev.getId() + " is neither queued nor plugged at charger: " + charger.getId());
		}
	}

	private void queueVehicle(ElectricVehicle ev, double now) {
		queuedVehicles.add(ev);
		notifyVehicleQueued(ev, now);
	}

	private void plugVehicle(ElectricVehicle ev, double now) {
		pluggedVehicles.put(ev.getId(), ev);
		eventsManager.processEvent(new ChargingStartEvent(now, charger.getId(), ev.getId()));
		notifyChargingStarted(ev, now);
	}

	// meant for overriding
	protected void notifyVehicleQueued(ElectricVehicle ev, double now) {
	}

	// meant for overriding
	protected void notifyChargingStarted(ElectricVehicle ev, double now) {
	}

	// meant for overriding
	protected void notifyChargingEnded(ElectricVehicle ev, double now) {
	}

	@Override
	public boolean isPlugged(ElectricVehicle ev) {
		return pluggedVehicles.containsKey(ev.getId());
	}

	@Override
	public void reset() {
		queuedVehicles.clear();
		pluggedVehicles.clear();
	}
}
