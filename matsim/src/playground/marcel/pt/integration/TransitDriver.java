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

package playground.marcel.pt.integration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.facilities.Facility;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.mobsim.queuesim.DriverAgent;
import org.matsim.core.mobsim.queuesim.Simulation;
import org.matsim.core.mobsim.queuesim.SimulationTimer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;

import playground.marcel.pt.interfaces.PassengerAgent;
import playground.marcel.pt.interfaces.TransitVehicle;
import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitRouteStop;

public class TransitDriver implements DriverAgent {

		private final List<Facility> stops;
		private final List<Link> linkRoute;
		private final NetworkRoute carRoute;
		private final double departureTime;

		private TransitVehicle vehicle = null;

		private int nextLinkIndex = 0;
		private Link currentLink = null;
		private final TransitQueueSimulation sim;

		private final Leg currentLeg = new LegImpl(TransportMode.car);
		private final Person dummyPerson;
		
		private final TransitLine transitLine;
		
		public TransitDriver(final TransitLine line, final TransitRoute route, final Departure departure, final TransitQueueSimulation sim) {
			this.transitLine = line;
			this.dummyPerson = new PersonImpl(new IdImpl("ptDrvr_" + departure.getId().toString()));
			this.stops = new ArrayList<Facility>(route.getStops().size());
			for (TransitRouteStop stop : route.getStops()) {
				this.stops.add(stop.getStopFacility());
			}
			this.sim = sim;
			this.departureTime = departure.getDepartureTime();
			this.carRoute = route.getRoute();
			List<Link> links = carRoute.getLinks();
			this.linkRoute = new ArrayList<Link>(2 + links.size());
			this.linkRoute.add(carRoute.getStartLink());
			this.linkRoute.addAll(links);
			this.linkRoute.add(carRoute.getEndLink());
			
			this.currentLeg.setRoute(this.carRoute);
		}

		public void setVehicle(final TransitVehicle vehicle) {
			this.vehicle = vehicle;
		}

		public Link chooseNextLink() {
			if (this.nextLinkIndex < this.linkRoute.size()) {
				return this.linkRoute.get(this.nextLinkIndex);
			}
			return null;
		}

		public void moveOverNode() {
			this.currentLink = this.linkRoute.get(this.nextLinkIndex);
			this.nextLinkIndex++;
			// let's see if we have a stop at that link
			for (Facility stop : this.stops) {
				if (stop.getLink() == this.currentLink) {
					handleStop(stop, SimulationTimer.getTime());
				}
			}
		}

		private void handleStop(final Facility stop, double now) {
			// let passengers get out if they want
			ArrayList<PassengerAgent> passengersLeaving = new ArrayList<PassengerAgent>();
			for (PassengerAgent passenger : this.vehicle.getPassengers()) {
				if (passenger.arriveAtStop(stop)) {
					passengersLeaving.add(passenger);
				}
			}
			for (PassengerAgent passenger : passengersLeaving) {
				this.vehicle.removePassenger(passenger);
				DriverAgent agent = (DriverAgent) passenger;
				System.out.println("passenger exit: agent=" + agent.getPerson().getId() + " facility=" + stop.getId());
				agent.teleportToLink(stop.getLink());
				agent.legEnds(now);
			}

			for (Iterator<DriverAgent> iter = this.sim.agentTracker.getAgentsAtStop(stop).iterator(); iter.hasNext(); ) {
				DriverAgent agent = iter.next();
				PassengerAgent passenger = (PassengerAgent) agent;
				if (passenger.ptLineAvailable(this.transitLine)) {
					this.vehicle.addPassenger(passenger);
					System.out.println("passenger enter: agent=" + agent.getPerson().getId() + " facility=" + stop.getId());
					iter.remove();
				}
			}
		}

		public double getDepartureTime() {
			return this.departureTime;
		}

		public void activityEnds(double now) {
			this.sim.agentDeparts(this, this.currentLeg.getRoute().getStartLink());
		}

		public Leg getCurrentLeg() {
			return this.currentLeg;
		}

		public Link getDestinationLink() {
			return this.currentLeg.getRoute().getEndLink();
		}

		public Person getPerson() {
			return this.dummyPerson;
		}

		public void legEnds(double now) {
			Simulation.decLiving();
		}

		public void teleportToLink(Link link) {
		}
}
