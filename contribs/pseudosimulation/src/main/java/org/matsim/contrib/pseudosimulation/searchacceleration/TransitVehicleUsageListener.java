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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.Vehicles;

import floetteroed.utilities.TimeDiscretization;

/**
 * Keeps track of when every single passenger enters which transit vehicle.
 * 
 * @author Gunnar Flötteröd
 *
 */
class TransitVehicleUsageListener implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	// -------------------- MEMBERS --------------------

	// TODO redundant; is also encoded in vehicleUsages
	private final TimeDiscretization timeDiscretization;

	private final Population population;

	private final Vehicles transitVehicles;

	// Maps a person on all vehicle-time-slots used by that person.
	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> passengerId2EntryIndicators;

	// Keeps track of when a person last entered a vehicle.
	private final Map<Id<Person>, Double> passengerId2lastEntryTime = new LinkedHashMap<>();

	// Keeps track of each transit vehicle's on-board times.
	private final Map<Id<Vehicle>, Double> vehicleId2sumOfOnboardTimes = new LinkedHashMap<>();

	// Total number of entries per transit vehicle
	private final Map<Id<Vehicle>, Double> vehicleId2entryCnt = new LinkedHashMap<>();

	private final boolean debug = false;
	
	// -------------------- CONSTRUCTION --------------------

	public TransitVehicleUsageListener(final TimeDiscretization timeDiscretization, final Population population,
			final Vehicles transitVehicles,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> passengerId2EntryIndicators) {
		this.timeDiscretization = timeDiscretization;
		this.population = population;
		this.transitVehicles = transitVehicles;
		this.passengerId2EntryIndicators = passengerId2EntryIndicators;
		Logger.getLogger(this.getClass()).info("registered population size is " + population.getPersons().size());
		Logger.getLogger(this.getClass())
				.info("register transit fleet size is " + transitVehicles.getVehicles().size());
	}

	// -------------------- IMPLEMENTATION --------------------

	Map<Id<Person>, SpaceTimeIndicators<Id<?>>> getIndicatorView() {
		return Collections.unmodifiableMap(this.passengerId2EntryIndicators);
	}

	Map<Id<Vehicle>, Double> newTransitWeightView() {

		final Map<Id<Vehicle>, Double> result = new LinkedHashMap<>();
		double usedVehiclesOriginalCapacitySum = 0.0;
		double usedVehiclesAdjustedCapacitySum = 0.0;
		final Set<Id<Vehicle>> unusedVehicleIds = new LinkedHashSet<>();

		for (Id<Vehicle> vehicleId : this.transitVehicles.getVehicles().keySet()) {
			final double entryCnt = this.vehicleId2entryCnt.getOrDefault(vehicleId, 0.0);
			if (entryCnt > 0) {
				final double avgOnboardTime_s = this.vehicleId2sumOfOnboardTimes.get(vehicleId) / entryCnt;
				if (avgOnboardTime_s <= 1e-8) {
					throw new RuntimeException("Average onboard time of vehicle " + vehicleId + " is "
							+ avgOnboardTime_s + " seconds, but " + entryCnt + " passengers have entered the vehicle.");
				}
				final VehicleCapacity capacity = this.transitVehicles.getVehicles().get(vehicleId).getType()
						.getCapacity();
				usedVehiclesOriginalCapacitySum += capacity.getSeats() + capacity.getStandingRoom();
				final double adjustedCapacity = (capacity.getSeats() + capacity.getStandingRoom())
						* (this.timeDiscretization.getBinSize_s() / avgOnboardTime_s);
				usedVehiclesAdjustedCapacitySum += adjustedCapacity;
				result.put(vehicleId, 1.0 / adjustedCapacity);
			} else {
				unusedVehicleIds.add(vehicleId);
			}
		}

		final double capacityFactor = ((result.size() == 0) ? 1.0
				: (usedVehiclesAdjustedCapacitySum / usedVehiclesOriginalCapacitySum));
		for (Id<Vehicle> vehicleId : unusedVehicleIds) {
			final VehicleCapacity capacity = this.transitVehicles.getVehicles().get(vehicleId).getType().getCapacity();
			result.put(vehicleId, 1.0 / (capacityFactor * (capacity.getSeats() + capacity.getStandingRoom())));
		}

		if (this.debug) {
			for (Map.Entry<Id<Vehicle>, Double> entry : result.entrySet()) {
				Logger.getLogger(this.getClass()).info(entry);
			}
		}

		return Collections.unmodifiableMap(result);
	}

	// --------------- IMPLEMENTATION OF EventHandler INTERFACES ---------------

	@Override
	public void reset(int iteration) {
		this.passengerId2EntryIndicators.clear();
		this.passengerId2lastEntryTime.clear();
		this.vehicleId2entryCnt.clear();
		this.vehicleId2sumOfOnboardTimes.clear();
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		final Id<Vehicle> vehicleId = event.getVehicleId();
		final Id<Person> passengerId = event.getPersonId();

		if ((this.transitVehicles.getVehicles().containsKey(vehicleId))
				&& (this.population.getPersons().containsKey(passengerId))) {
			final double time_s = event.getTime();
			this.passengerId2lastEntryTime.put(passengerId, time_s);
			this.vehicleId2entryCnt.put(vehicleId, this.vehicleId2entryCnt.getOrDefault(vehicleId, 0.0) + 1.0);

			if ((time_s >= this.timeDiscretization.getStartTime_s())
					&& (time_s < this.timeDiscretization.getEndTime_s())) {
				SpaceTimeIndicators<Id<?>> indicators = this.passengerId2EntryIndicators.get(passengerId);
				if (indicators == null) {
					indicators = new SpaceTimeIndicators<Id<?>>(this.timeDiscretization.getBinCnt());
					this.passengerId2EntryIndicators.put(passengerId, indicators);
					if (this.debug) {
						Logger.getLogger(this.getClass()).info("passenger " + passengerId + " entered vehicle "
								+ vehicleId + " at time " + time_s + "s.");

					}
				}
				indicators.visit(event.getVehicleId(), this.timeDiscretization.getBin(time_s));
			}
		}
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		final double time_s = event.getTime();
		final Id<Vehicle> vehicleId = event.getVehicleId();
		final Id<Person> passengerId = event.getPersonId();
		if ((this.transitVehicles.getVehicles().containsKey(vehicleId))
				&& (this.population.getPersons().containsKey(passengerId))
				&& (time_s >= this.timeDiscretization.getStartTime_s())
				&& (time_s < this.timeDiscretization.getEndTime_s())) {
			final double onBoardTime_s = time_s - this.passengerId2lastEntryTime.remove(passengerId);
			this.vehicleId2sumOfOnboardTimes.put(event.getVehicleId(),
					this.vehicleId2sumOfOnboardTimes.getOrDefault(vehicleId, 0.0) + onBoardTime_s);
		}
	}
}
