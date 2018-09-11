/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.pseudosimulation.searchacceleration.utils.SetUtils;
import org.matsim.vehicles.Vehicles;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class SlotUsageListener implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler,
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	// -------------------- MEMBERS --------------------

	private final AccelerationConfigGroup accelerationConfig;

	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2indicators;

	private final PrivateTrafficLinkUsageListener privateTrafficLinkUsageListener;

	private final TransitVehicleUsageListener transitVehicleUsageListener;

	// -------------------- CONSTRUCTION --------------------

	SlotUsageListener(final Population population, final Vehicles transitVehicles,
			final AccelerationConfigGroup accelerationConfig) {
		if (!SetUtils.disjoint(population.getPersons().keySet(), transitVehicles.getVehicles().keySet())) {
			throw new RuntimeException("Population ids and transit vehicle ids are not disjoint.");
		}
		this.accelerationConfig = accelerationConfig;
		this.personId2indicators = new ConcurrentHashMap<>(); // Shared by different listeners.
		this.privateTrafficLinkUsageListener = new PrivateTrafficLinkUsageListener(
				this.accelerationConfig.getTimeDiscretization(), population, this.personId2indicators);
		this.transitVehicleUsageListener = new TransitVehicleUsageListener(
				this.accelerationConfig.getTimeDiscretization(), population, transitVehicles, this.personId2indicators);
	}

	// -------------------- CONTENT ACCESS --------------------

	Map<Id<Person>, SpaceTimeIndicators<Id<?>>> getNewIndicatorView() {
		// Need a deep copy because of subsequent (pSim) resets.
		return Collections.unmodifiableMap(new LinkedHashMap<>(this.personId2indicators));
	}

	Map<Id<?>, Double> getWeightView() {
		final Map<Id<?>, Double> result = new LinkedHashMap<>(this.accelerationConfig.getLinkWeightView());
		result.putAll(this.transitVehicleUsageListener.newTransitWeightView());
		return result;
	}

	// -------------------- IMPLEMENTATION OF *EventHandler --------------------

	@Override
	public void reset(final int iteration) {
		this.privateTrafficLinkUsageListener.reset(iteration);
		this.transitVehicleUsageListener.reset(iteration);
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		this.privateTrafficLinkUsageListener.handleEvent(event);
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		this.privateTrafficLinkUsageListener.handleEvent(event);
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		this.transitVehicleUsageListener.handleEvent(event);
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		this.transitVehicleUsageListener.handleEvent(event);
	}
}
