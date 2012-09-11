/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDecorator.java
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
package playground.johannes.coopsim.pysical;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author illenberger
 *
 */
public class TravelTimeDecorator implements TravelTime {

	private final TravelTime delegate;
	
	private final double ttFactor;
	
	public TravelTimeDecorator(TravelTime delegate, double ttFactor) {
		this.delegate = delegate;
		this.ttFactor = ttFactor;
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		return delegate.getLinkTravelTime(link, time, person, vehicle) * ttFactor;
	}

}
