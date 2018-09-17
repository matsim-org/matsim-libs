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
package org.matsim.contrib.pseudosimulation.searchacceleration.listeners;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import floetteroed.utilities.Tuple;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class FifoTransitPerformance
		implements TransitDriverStartsEventHandler, AgentWaitingForPtEventHandler, VehicleArrivesAtFacilityEventHandler,
		VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, VehicleLeavesTrafficEventHandler {

	// -------------------- Composed identifier class --------------------

	private static class LineRouteOtherId<T extends Identifiable<T>> {

		private final Id<TransitLine> lineId;
		private final Id<TransitRoute> routeId;
		private final Id<T> otherId;

		private LineRouteOtherId(final Id<TransitLine> lineId, final Id<TransitRoute> routeId,
				final Id<T> xyzIdentifier) {
			this.lineId = lineId;
			this.routeId = routeId;
			this.otherId = xyzIdentifier;
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof LineRouteOtherId) {
				final LineRouteOtherId<?> other = (LineRouteOtherId<?>) obj;
				return (this.lineId.equals(other.lineId) && this.routeId.equals(other.routeId)
						&& this.otherId.equals(other.otherId));
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			// same recipe as in AbstractList
			int hashCode = 1;
			hashCode = 31 * hashCode + this.lineId.hashCode();
			hashCode = 31 * hashCode + this.routeId.hashCode();
			hashCode = 31 * hashCode + this.otherId.hashCode();
			return hashCode;
		}
	}

	// -------------------- REFERENCES --------------------

	private final MobSimSwitcher mobsimSwitcher;

	private final Population population;

	private final Vehicles transitVehicles;

	private final TransitSchedule transitSchedule;

	// This one contains the actual result.
	private final Map<LineRouteOtherId<TransitStopFacility>, NextAvailableDepartures> lineRouteStop2nextAvailableDepartures = new LinkedHashMap<>();

	// -------------------- TEMPORARY MEMBERS --------------------

	// Keeps track of what a vehicle is up to.
	private final Map<Id<Vehicle>, LineRouteOtherId<Departure>> vehicle2lineRouteDeparture = new LinkedHashMap<>();

	// Keeps track of who is waiting since when at which stop.
	private Map<Id<TransitStopFacility>, Map<Id<Person>, Double>> facility2person2startWaitingTime_s = new LinkedHashMap<>();

	// Keeps track of which vehicle is currently at which stop.
	private final Map<Id<Vehicle>, Id<TransitStopFacility>> vehicle2stopfacility = new LinkedHashMap<>();

	// Keeps track of waiting times of passengers boarding at the current stop.
	private Map<Id<Vehicle>, List<Double>> vehicle2startWaitingTimes_s = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	/**
	 * Schedule-based initialization.
	 */
	public FifoTransitPerformance(final MobSimSwitcher mobsimSwitcher, final Population population,
			final Vehicles transitVehicles, final TransitSchedule transitSchedule) {
		this.mobsimSwitcher = mobsimSwitcher;
		this.population = population;
		this.transitVehicles = transitVehicles;
		this.transitSchedule = transitSchedule;
		this.resetToSchedule();
	}

	private void resetToSchedule() {
		this.lineRouteStop2nextAvailableDepartures.clear();
		for (TransitLine line : this.transitSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					this.lineRouteStop2nextAvailableDepartures
							.put(new LineRouteOtherId<TransitStopFacility>(line.getId(), route.getId(),
									stop.getStopFacility().getId()), new NextAvailableDepartures(route, stop));
				}
			}
		}
	}

	// -------------------- RESULT ACCESS --------------------

	public Tuple<Departure, Double> getNextDepartureAndTime_s(final Id<TransitLine> lineId, final TransitRoute route,
			final Id<TransitStopFacility> stopId, final double time_s) {
		final Tuple<Id<Departure>, Double> departureIdAndTime_s = this.lineRouteStop2nextAvailableDepartures
				.get(new LineRouteOtherId<>(lineId, route.getId(), stopId))
				.getNextAvailableDepartureIdAndTime_s(time_s);
		if (departureIdAndTime_s == null) {
			return null;
		} else {
			return new Tuple<>(route.getDepartures().get(departureIdAndTime_s.getA()), departureIdAndTime_s.getB());
		}
	}

	// -------------------- IMPLEMENTATION OF EVENT HANDLERS --------------------

	@Override
	public void reset(final int iteration) {
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.resetToSchedule();
			this.vehicle2lineRouteDeparture.clear();
			this.facility2person2startWaitingTime_s.clear();
			this.vehicle2stopfacility.clear();
			this.vehicle2startWaitingTimes_s.clear();
		}
	}

	@Override
	public void handleEvent(final TransitDriverStartsEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()
				&& this.transitVehicles.getVehicles().containsKey(event.getVehicleId())) {
			this.vehicle2lineRouteDeparture.put(event.getVehicleId(), new LineRouteOtherId<Departure>(
					event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId()));
		}
	}

	@Override
	public void handleEvent(final AgentWaitingForPtEvent event) {
		if (this.mobsimSwitcher.isQSimIteration() && this.population.getPersons().containsKey(event.getPersonId())) {
			Map<Id<Person>, Double> person2startWaitingTime_s = this.facility2person2startWaitingTime_s
					.get(event.getWaitingAtStopId());
			if (person2startWaitingTime_s == null) {
				person2startWaitingTime_s = new LinkedHashMap<>();
				this.facility2person2startWaitingTime_s.put(event.getWaitingAtStopId(), person2startWaitingTime_s);
			}
			person2startWaitingTime_s.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()
				&& this.transitVehicles.getVehicles().containsKey(event.getVehicleId())) {
			this.vehicle2stopfacility.put(event.getVehicleId(), event.getFacilityId());
		}
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()
				&& this.transitVehicles.getVehicles().containsKey(event.getVehicleId())
				&& this.population.getPersons().keySet().contains(event.getPersonId())) {
			List<Double> allStartWaitingTimes_s = this.vehicle2startWaitingTimes_s.get(event.getVehicleId());
			if (allStartWaitingTimes_s == null) {
				allStartWaitingTimes_s = new LinkedList<>();
				this.vehicle2startWaitingTimes_s.put(event.getVehicleId(), allStartWaitingTimes_s);
			}
			final Id<TransitStopFacility> facilityId = this.vehicle2stopfacility.get(event.getVehicleId());
			allStartWaitingTimes_s
					.add(this.facility2person2startWaitingTime_s.get(facilityId).remove(event.getPersonId()));
		}
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()
				&& this.transitVehicles.getVehicles().containsKey(event.getVehicleId())) {
			final List<Double> allArrivalTimes_s = this.vehicle2startWaitingTimes_s.get(event.getVehicleId());
			if (allArrivalTimes_s != null) {
				final LineRouteOtherId<Departure> lineRouteDeparture = this.vehicle2lineRouteDeparture
						.get(event.getVehicleId());
				final NextAvailableDepartures nextAvailableDepartures = this.lineRouteStop2nextAvailableDepartures
						.get(new LineRouteOtherId<TransitStopFacility>(lineRouteDeparture.lineId,
								lineRouteDeparture.routeId, event.getFacilityId()));
				for (Double arrivalTime_s : allArrivalTimes_s) {
					nextAvailableDepartures.adjustToRealizedDeparture(arrivalTime_s, lineRouteDeparture.otherId);
				}
			}
			this.vehicle2startWaitingTimes_s.remove(event.getVehicleId());
			this.vehicle2stopfacility.remove(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(final VehicleLeavesTrafficEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()
				&& this.transitVehicles.getVehicles().containsKey(event.getVehicleId())) {
			this.vehicle2lineRouteDeparture.remove(event.getVehicleId());
		}
	}
}
