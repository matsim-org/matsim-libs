/* *********************************************************************** *
 * project: org.matsim.*
 * Matingimpl.java
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
package playground.thibautd.agentsmating.logitbasedmating.basic;

import java.util.ArrayList;
import java.util.List;

import playground.thibautd.agentsmating.logitbasedmating.framework.Mating;
import playground.thibautd.agentsmating.logitbasedmating.framework.TripRequest;

/**
 * Default implementation of a mating
 * @author thibautd
 */
public class MatingImpl implements Mating {
	private final TripRequest driver;
	private final List<TripRequest> passengers;

	public MatingImpl(
			final TripRequest driver,
			final List<TripRequest> passengers) {
		this.driver = driver;
		this.passengers = passengers;
	}

	/**
	 * creates a mating with only one passenger
	 */
	public MatingImpl(
			final TripRequest driver,
			final TripRequest passenger) {
		this.driver = driver;
		this.passengers = new ArrayList<TripRequest>();
		passengers.add(passenger);
	}

	@Override
	public TripRequest getDriver() {
		return driver;
	}

	@Override
	public List<TripRequest> getPassengers() {
		return passengers;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"={ "+
			"driver="+driver+" , passengers="+passengers+" }";
	}
}

