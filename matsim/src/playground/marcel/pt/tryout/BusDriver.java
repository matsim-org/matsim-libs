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

package playground.marcel.pt.tryout;

import java.util.ArrayList;
import java.util.List;

import org.matsim.facilities.Facility;
import org.matsim.network.Link;
import org.matsim.population.Person;
import org.matsim.population.routes.CarRoute;

import playground.marcel.pt.interfaces.DriverAgent;
import playground.marcel.pt.interfaces.PassengerAgent;
import playground.marcel.pt.interfaces.Vehicle;
import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.utils.FacilityVisitors;

public class BusDriver implements DriverAgent {

		private final List<Facility> stops;
		private final List<Link> linkRoute;
		private final double departureTime;

		private Vehicle vehicle = null;

		private int nextLinkIndex = 0;
		private Link currentLink = null;
		private FacilityVisitors facilityVisitors = null;

		public BusDriver(final List<Facility> stops, final List<Link> linkRoute, final double departureTime) {
			this.stops = stops;
			this.linkRoute = linkRoute;
			this.departureTime = departureTime;
		}

		public BusDriver(final TransitRoute route, final Departure departure) {
			this.stops = new ArrayList<Facility>(route.getStops().size());
			for (TransitRouteStop stop : route.getStops()) {
				this.stops.add(stop.getStopFacility());
			}
			CarRoute carRoute = (CarRoute) route.getRoute();
			List<Link> links = carRoute.getLinks();
			this.linkRoute = new ArrayList<Link>(2 + links.size());
			this.linkRoute.add(carRoute.getStartLink());
			this.linkRoute.addAll(links);
			this.linkRoute.add(carRoute.getEndLink());
			this.departureTime = departure.getDepartureTime();
		}

		public void setFacilityVisitorObserver(final FacilityVisitors fv) {
			this.facilityVisitors  = fv;
		}

		public void setVehicle(final Vehicle vehicle) {
			this.vehicle = vehicle;
		}

		public Link chooseNextLink() {
			if (this.nextLinkIndex < this.linkRoute.size()) {
				return this.linkRoute.get(this.nextLinkIndex);
			}
			return null;
		}

		public void leaveCurrentLink() {
			this.currentLink = null;
		}

		public void enterNextLink() {
			this.currentLink = this.linkRoute.get(this.nextLinkIndex);
			this.nextLinkIndex++;
			// let's see if we have a stop at that link
			for (Facility stop : this.stops) {
				if (stop.getLink() == this.currentLink) {
					handleStop(stop);
				}
			}
		}

		private void handleStop(final Facility stop) {
			// let passengers get out if they want
			ArrayList<PassengerAgent> passengersLeaving = new ArrayList<PassengerAgent>();
			for (PassengerAgent passenger : this.vehicle.getPassengers()) {
				if (passenger.arriveAtStop(stop)) {
					passengersLeaving.add(passenger);
				}
			}
			for (PassengerAgent passenger : passengersLeaving) {
				this.vehicle.removePassenger(passenger);
				// TODO [MR] add person to facility
			}

			if (this.facilityVisitors != null) {
				List<Person> people = this.facilityVisitors.getVisitors(stop, "pt_interaction");
				for (Person person : people) {
					if (((PassengerAgent) person).ptLineAvailable()) {
						this.vehicle.addPassenger((PassengerAgent) person);
						// TODO [MR] remove person from facility
					}
				}
			}
		}

}
