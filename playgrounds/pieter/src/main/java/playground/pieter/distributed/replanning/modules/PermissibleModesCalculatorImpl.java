/* *********************************************************************** *
 * project: org.matsim.*
 * PermissibleModesCalculatorImpl.java
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
package playground.pieter.distributed.replanning.modules;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.algorithms.PermissibleModesCalculator;
import playground.pieter.distributed.plans.PersonForPlanGenomes;

import java.util.*;

public class PermissibleModesCalculatorImpl implements PermissibleModesCalculator {
	private final List<String> availableModes;
	private final List<String> availableModesWithoutCar;
	private final boolean considerCarAvailability;

	public PermissibleModesCalculatorImpl(
            final String[] availableModes,
            final boolean considerCarAvailability) {
		this.availableModes = Arrays.asList(availableModes);

		if ( this.availableModes.contains( TransportMode.car ) ) {
			final List<String> l = new ArrayList<String>( this.availableModes );
			while ( l.remove( TransportMode.car ) ) {}
			this.availableModesWithoutCar = Collections.unmodifiableList( l );
		}
		else {
			this.availableModesWithoutCar = this.availableModes;
		}

		this.considerCarAvailability = considerCarAvailability;
	}

	@Override
	public Collection<String> getPermissibleModes(final Plan plan) {
		if (!considerCarAvailability) return availableModes; 

		final PersonForPlanGenomes person;
		try {
			person = (PersonForPlanGenomes) plan.getPerson();
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException( "I need a PersonImpl to get car availability" );
		}

		final boolean carAvail =
			!"no".equals( person.getLicense() ) &&
			!"never".equals( person.getCarAvail() );

		return carAvail ? availableModes : availableModesWithoutCar;
	}
}
