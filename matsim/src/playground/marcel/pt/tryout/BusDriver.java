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

import java.util.List;

import org.matsim.facilities.Facility;
import org.matsim.network.Link;

import playground.marcel.pt.interfaces.DriverAgent;
import playground.marcel.pt.interfaces.PassengerAgent;
import playground.marcel.pt.interfaces.Vehicle;

public class BusDriver implements DriverAgent {

		private final List<Facility> stops;
		private final List<Link> linkRoute;
		private final double departureTime;

		private Vehicle vehicle = null;

		private int nextLinkIndex = 0;

		public BusDriver(final List<Facility> stops, final List<Link> linkRoute, final double departureTime) {
			this.stops = stops;
			this.linkRoute = linkRoute;
			this.departureTime = departureTime;
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

		public void enterNextLink() {
			this.nextLinkIndex++;
		}

		public void leaveLink(final Link link) {

		}

		public void enterLink(final Link link) {
			// let's see if we have a stop at that link
			for (Facility stop : this.stops) {
				if (stop.getLink() == link) {
					handleStop(stop);
				}
			}
		}

		private void handleStop(final Facility stop) {
			// let passengers get out if they want
			for (PassengerAgent passenger : this.vehicle.getPassengers()) {
				if (passenger.arriveAtStop(stop)) {
					this.vehicle.removePassenger(passenger);
				}
			}

			// let passengers get in if they want
			// TODO [MR]
			// how to get to the list of people waiting there...?
		}

}
