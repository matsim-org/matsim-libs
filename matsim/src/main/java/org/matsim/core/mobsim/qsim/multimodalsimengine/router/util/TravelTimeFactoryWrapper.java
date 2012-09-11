/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeFactoryWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.multimodalsimengine.router.util;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * Wraps a PersonalizableTravelTime around a TravelTime object.
 */
public class TravelTimeFactoryWrapper implements PersonalizableTravelTimeFactory {

	private final TravelTime travelTime;
	
	public TravelTimeFactoryWrapper(TravelTime travelTime) {
		this.travelTime = new Wrapper(travelTime);
	}
	
	@Override
	public TravelTime createTravelTime() {
		return travelTime;
	}

	private static class Wrapper implements PersonalizableTravelTime {

		private final TravelTime travelTime;
		
		public Wrapper(TravelTime travelTime) {
			this.travelTime = travelTime;
		}

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return this.travelTime.getLinkTravelTime(link, time, person, vehicle);
		}
		
	}
}
