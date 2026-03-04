/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerAccessEgressImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.qsim.pt;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonContinuesInVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.BoardingDeniedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.util.*;


/**
 * @author dgrether
 */
class PassengerAccessEgressImpl implements PassengerAccessEgress {

	private final InternalInterface internalInterface;
	private final TransitStopAgentTracker agentTracker;
	private final boolean isGeneratingDeniedBoardingEvents;
	/**
	 * These agents are at the stop and relocate to another vehicle.
	 */
	private final Map<Id<TransitStopFacility>, List<PTPassengerAgent>> agentRelocating = new LinkedHashMap<>();
	private final Set<PTPassengerAgent> agentsDeniedToBoard;
	private final Scenario scenario;
	private final EventsManager eventsManager;

	PassengerAccessEgressImpl(InternalInterface internalInterface, TransitStopAgentTracker agentTracker, Scenario scenario, EventsManager eventsManager) {
		this.internalInterface = internalInterface;
		this.agentTracker = agentTracker;
		this.scenario = scenario;
		this.eventsManager = eventsManager;
		this.isGeneratingDeniedBoardingEvents = this.scenario.getConfig().vspExperimental().isGeneratingBoardingDeniedEvents();
		this.agentsDeniedToBoard = isGeneratingDeniedBoardingEvents ? new HashSet<>() : null;
	}

	/**
	 * @return should be 0.0 or 1.0. Values greater than 1.0 may lead to buggy behavior, dependent on TransitStopHandler used
	 */
	/*package*/ double calculateStopTimeAndTriggerBoarding(TransitRoute transitRoute, TransitLine transitLine, final TransitVehicle vehicle,
														   final TransitStopFacility stop, List<TransitRouteStop> stopsToCome, final double now) {

		List<PTPassengerAgent> passengersLeaving = findPassengersLeaving(vehicle, stop);

		// Relocating passengers are only determined at the very last stop.
		List<PTPassengerAgent> passengersRelocating = stopsToCome.isEmpty() ? findPassengersRelocating(vehicle, stop) : List.of();


		int freeCapacity = vehicle.getPassengerCapacity() - vehicle.getPassengers().size() + passengersLeaving.size();

		List<PTPassengerAgent> passengersEntering = findPassengersEntering(transitRoute, transitLine, vehicle, stop, stopsToCome, freeCapacity);

		TransitStopHandler stopHandler = vehicle.getStopHandler();
		double stopTime = stopHandler.handleTransitStop(stop, now, passengersLeaving, passengersEntering, passengersRelocating, this, vehicle);
		if (stopTime == 0.0) { // (de-)boarding is complete when the additional stopTime is 0.0
			if (this.isGeneratingDeniedBoardingEvents) {
				this.fireBoardingDeniedEvents(vehicle, now);
				this.agentsDeniedToBoard.clear();
			}
		}
		return stopTime;
	}

	private void fireBoardingDeniedEvents(TransitVehicle vehicle, double now) {
		Id<Vehicle> vehicleId = vehicle.getId();
		for (PTPassengerAgent agent : this.agentsDeniedToBoard) {
			Id<Person> agentId = agent.getId();
			this.eventsManager.processEvent(
				new BoardingDeniedEvent(now, agentId, vehicleId)
			);
		}
	}


	private List<PTPassengerAgent> findPassengersEntering(TransitRoute transitRoute, TransitLine transitLine, TransitVehicle vehicle,
														  final TransitStopFacility stop, List<TransitRouteStop> stopsToCome, int freeCapacity) {
		ArrayList<PTPassengerAgent> passengersEntering = new ArrayList<>();

		if (this.isGeneratingDeniedBoardingEvents) {

			for (PTPassengerAgent agent : this.agentTracker.getAgentsAtFacility(stop.getId())) {
				if (agent.getEnterTransitRoute(transitLine, transitRoute, stopsToCome, vehicle)) {
					if (freeCapacity >= 1) {
						passengersEntering.add(agent);
						freeCapacity--;
					} else {
						this.agentsDeniedToBoard.add(agent);
					}
				}
			}

		} else {

			for (PTPassengerAgent agent : this.agentTracker.getAgentsAtFacility(stop.getId())) {
				if (freeCapacity == 0) {
					break;
				}
				if (agent.getEnterTransitRoute(transitLine, transitRoute, stopsToCome, vehicle)) {
					passengersEntering.add(agent);
					freeCapacity--;
				}
			}

		}

		return passengersEntering;
	}

	private List<PTPassengerAgent> findPassengersLeaving(TransitVehicle vehicle,
														 final TransitStopFacility stop) {
		List<PTPassengerAgent> passengersLeaving = new ArrayList<>();
		for (PassengerAgent passenger : vehicle.getPassengers()) {
			if (((PTPassengerAgent) passenger).getExitAtStop(stop)) {
				passengersLeaving.add((PTPassengerAgent) passenger);
			}
		}
		return passengersLeaving;
	}

	private List<PTPassengerAgent> findPassengersRelocating(TransitVehicle vehicle, final TransitStopFacility stop) {

		List<PTPassengerAgent> relocatingPassengers = new ArrayList<>();
		for (PassengerAgent passenger : vehicle.getPassengers()) {
			if (((PTPassengerAgent) passenger).getRelocationAtStop(stop)) {
				relocatingPassengers.add((PTPassengerAgent) passenger);
			}
		}

		return relocatingPassengers;
	}

	@Override
	public boolean handlePassengerEntering(PTPassengerAgent passenger, TransitVehicle vehicle, Id<TransitStopFacility> fromStopFacilityId, double time) {
		boolean handled = vehicle.addPassenger(passenger);
		if (handled) {
			this.agentTracker.removeAgentFromStop(passenger, fromStopFacilityId);
			this.internalInterface.unregisterAdditionalAgentOnLink(passenger.getId(), passenger.getCurrentLinkId());
			passenger.setVehicle(vehicle);

			var driver = vehicle.getDriver();

			var e = new PersonEntersPtVehicleEvent(time, passenger.getId(), vehicle.getId(), driver.getTransitLine().getId(), driver.getTransitRoute().getId());
			eventsManager.processEvent(e);
		}
		return handled;
	}

	@Override
	public boolean handlePassengerLeaving(PTPassengerAgent passenger, TransitVehicle vehicle, Id<Link> toLinkId, double time) {
		boolean handled = vehicle.removePassenger(passenger);
		if (handled) {
			passenger.setVehicle(null);
			var driver = vehicle.getDriver();
			var e = new PersonLeavesPtVehicleEvent(time, passenger.getId(), vehicle.getId(), driver.getTransitLine().getId(), driver.getTransitRoute().getId());
			eventsManager.processEvent(e);

			// from here on works only if PassengerAgent can be cast into MobsimAgent ... but this is how it was before.
			// kai, sep'12

			MobsimAgent agent = (MobsimAgent) passenger;
			agent.notifyArrivalOnLinkByNonNetworkMode(toLinkId);
			agent.endLegAndComputeNextState(time);
			this.internalInterface.arrangeNextAgentState(agent);
			// (cannot set trEngine to TransitQSimEngine because there are tests where this will not work. kai, dec'11)
		}
		return handled;
	}

	@Override
	public void handlePassengerRelocating(PTPassengerAgent passenger, TransitVehicle vehicle, Id<TransitStopFacility> stopFacilityId, double time) {

		boolean handled = vehicle.removePassenger(passenger);
		if (handled) {
			// Store passengers wanting to relocate at the stop facility
			agentRelocating.computeIfAbsent(stopFacilityId, _ -> new ArrayList<>()).add(passenger);

		} else
			throw new IllegalStateException("Agent " + passenger.getId() + " was not removed from vehicle " + vehicle.getId() + " when relocating.");
	}

	@Override
	public void relocatePassengers(TransitDriverAgentImpl vehicle, List<ChainedDeparture> departures, double time) {

		TransitRouteStop stop = vehicle.getTransitRoute().getStops().getLast();
		List<PTPassengerAgent> passengers = agentRelocating.getOrDefault(stop.getStopFacility().getId(), List.of());

		for (ChainedDeparture chain : departures) {
			TransitRoute route = scenario.getTransitSchedule().getTransitLines()
				.get(chain.getChainedTransitLineId())
				.getRoutes().get(chain.getChainedRouteId());

			Departure departure = route.getDepartures().get(chain.getChainedDepartureId());

			Id<Vehicle> newVehicle = departure.getVehicleId();

			boolean sameVehicle = newVehicle.equals(vehicle.getVehicle().getId());

			// The next vehicle is waiting for its activity to end so that it can start departing
			var nextVehicle = internalInterface.getMobsim().getVehicles().get(newVehicle);
			// The code I found below assumes that we have a transit driver. This means we can also assume that we have a TransitVehicle. janek feb' 26
			assert nextVehicle instanceof TransitVehicle : "Expected TransitVehicle, but got " + nextVehicle.getClass().getSimpleName();
			var nextTransitVehicle = (TransitVehicle) nextVehicle;
			var nextDriver = nextTransitVehicle.getDriver();

			if (!sameVehicle) {

				int left = agentTracker.trackVehicleArrival(chain.getChainedDepartureId());
				// Depart if all required trains for this departure have arrived
				if (left <= 0) {
					// It would be nicer to have this functionality in the TransitDriver interface. Yet, I don't want to refactor everything here,
					// so we leave it as is for now. janek feb' 26
					//AbstractTransitDriverAgent driver = (AbstractTransitDriverAgent) nextTransitVehicle.getDriver();
					((AbstractTransitDriverAgent) nextDriver).setReadyForDeparture(time);
					internalInterface.getMobsim().rescheduleActivityEnd(nextDriver);
				}
			}

			Iterator<PTPassengerAgent> it = passengers.iterator();

			while (it.hasNext()) {

				PTPassengerAgent passenger = it.next();

				// Use the chained departure if it contains a stop the passenger uses as an exit stop
				if (route.getStops().stream().map(TransitRouteStop::getStopFacility).noneMatch(passenger::getExitAtStop))
					continue;


				eventsManager.processEvent(new PersonContinuesInVehicleEvent(time, passenger.getId(), vehicle.getVehicle().getId(), newVehicle,
					route.getStops().getFirst().getStopFacility().getId(),
					nextDriver.getTransitLine().getId(),
					nextDriver.getTransitRoute().getId()
				));

				nextTransitVehicle.addPassenger(passenger);
				passenger.setVehicle(nextTransitVehicle);
				it.remove();
			}
		}

		if (!passengers.isEmpty()) {
			throw new IllegalStateException("There are still passengers at stop " + stop.getStopFacility().getId() + " that were not relocated to the next vehicle: " + passengers);
		}
	}
}
