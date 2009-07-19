/* *********************************************************************** *
 * project: org.matsim.*
 * BusDriver.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.queuesim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.basic.v01.events.BasicVehicleArrivesAtFacilityEventImpl;
import org.matsim.core.events.Events;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.Simulation;
import org.matsim.core.mobsim.queuesim.TransitDriverAgent;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitStopFacility;


public class TransitDriver implements TransitDriverAgent {

		private final List<Link> linkRoute;
		private final NetworkRoute carRoute;
		private final double departureTime;

		private TransitVehicle vehicle = null;

		private int nextLinkIndex = 0;
		private final TransitQueueSimulation sim;
		private final TransitStopAgentTracker agentTracker;

		private final LegImpl currentLeg = new LegImpl(TransportMode.car);
		private final PersonImpl dummyPerson;

		private final Iterator<TransitRouteStop> stopIterator;
		private TransitRouteStop nextStop;
		private TransitStopFacility lastHandledStop = null;

		private final TransitLine transitLine;

		public TransitDriver(final TransitLine line, final TransitRoute route, final Departure departure, final TransitStopAgentTracker agentTracker, final TransitQueueSimulation sim) {
			this.transitLine = line;
			this.dummyPerson = new PersonImpl(new IdImpl("ptDrvr_" + line.getId() + "_" + route.getId() + "_" + departure.getId().toString()));
			this.stopIterator = route.getStops().iterator();
			this.nextStop = (this.stopIterator.hasNext() ? this.stopIterator.next() : null);
			this.agentTracker = agentTracker;
			this.sim = sim;
			this.departureTime = departure.getDepartureTime();
			this.carRoute = route.getRoute();
			this.linkRoute = this.carRoute.getLinks();

			this.currentLeg.setRoute(this.carRoute);
		}

		public void setVehicle(final TransitVehicle vehicle) {
			this.vehicle = vehicle;
		}

		public Link chooseNextLink() {
			if (this.nextLinkIndex < this.linkRoute.size()) {
				return this.linkRoute.get(this.nextLinkIndex);
			}
			if (this.nextLinkIndex == this.linkRoute.size()) {
				return this.carRoute.getEndLink();
			}
			return null;
		}

		public void moveOverNode() {
			this.nextLinkIndex++;
		}

		public TransitStopFacility getNextTransitStop() {
			if (this.nextStop == null) {
				return null;
			}
			return this.nextStop.getStopFacility();
		}

		public double handleTransitStop(final TransitStopFacility stop, final double now) {
			if (stop != this.nextStop.getStopFacility()) {
				throw new RuntimeException("Expected different stop.");
			}

			int freeCapacity = this.vehicle.getPassengerCapacity() - this.vehicle.getPassengers().size();
			// find out who wants to get out
			ArrayList<PassengerAgent> passengersLeaving = new ArrayList<PassengerAgent>();
			for (PassengerAgent passenger : this.vehicle.getPassengers()) {
				if (passenger.arriveAtStop(stop)) {
					passengersLeaving.add(passenger);
				}
			}
			freeCapacity += passengersLeaving.size();
			// find out who wants to get in
			ArrayList<PassengerAgent> passengersEntering = new ArrayList<PassengerAgent>();
			for (PassengerAgent agent : this.agentTracker.getAgentsAtStop(stop)) {
				if (freeCapacity == 0) {
					break;
				}
				PassengerAgent passenger = agent;
				if (passenger.ptLineAvailable(this.transitLine)) {
					passengersEntering.add(passenger);
					freeCapacity--;
				}
			}

			// do the handling
			double stopTime = 0.0;
			int cntEgress = passengersLeaving.size();
			int cntAccess = passengersEntering.size();
			if (cntAccess > 0 || cntEgress > 0) {
				Events events = TransitQueueSimulation.getEvents();
				stopTime = cntAccess * 5 + cntEgress * 3;
				if (this.lastHandledStop != stop) {
					stopTime += 10.0; // add fixed amount of time for door-operations and similar stuff
				}
				events.processEvent(new BasicVehicleArrivesAtFacilityEventImpl(now, this.vehicle.getBasicVehicle().getId(), stop.getId()));

				for (PassengerAgent passenger : passengersLeaving) {
					this.vehicle.removePassenger(passenger);
					DriverAgent agent = (DriverAgent) passenger;
					events.processEvent(new PersonLeavesVehicleEvent(now, agent.getPerson(), this.vehicle.getBasicVehicle()));
					agent.teleportToLink(stop.getLink());
					agent.legEnds(now);
				}

				for (PassengerAgent passenger : passengersEntering) {
					this.agentTracker.removeAgentFromStop(passenger, stop);
					this.vehicle.addPassenger(passenger);
					DriverAgent agent = (DriverAgent) passenger;
					events.processEvent(new PersonEntersVehicleEvent(now, agent.getPerson(), this.vehicle.getBasicVehicle()));
				}

				events.processEvent(new BasicVehicleArrivesAtFacilityEventImpl(now + stopTime, this.vehicle.getBasicVehicle().getId(), stop.getId()));
			}
			this.lastHandledStop = stop;
			if (stopTime == 0.0) {
				if (this.stopIterator.hasNext()) {
					this.nextStop = this.stopIterator.next();
				} else {
					this.nextStop = null;
				}
			}
			return stopTime;
		}

		public double getDepartureTime() {
			return this.departureTime;
		}

		public void activityEnds(final double now) {
			this.sim.agentDeparts(this, this.currentLeg.getRoute().getStartLink());
		}

		public LegImpl getCurrentLeg() {
			return this.currentLeg;
		}

		public Link getDestinationLink() {
			return this.currentLeg.getRoute().getEndLink();
		}

		public PersonImpl getPerson() {
			return this.dummyPerson;
		}

		public void legEnds(final double now) {
			Simulation.decLiving();
		}

		public void teleportToLink(final Link link) {
		}
}
