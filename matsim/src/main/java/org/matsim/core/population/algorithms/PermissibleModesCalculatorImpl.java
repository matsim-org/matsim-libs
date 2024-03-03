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
package org.matsim.core.population.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonUtils;

public final class PermissibleModesCalculatorImpl implements PermissibleModesCalculator {

	private final List<String> availableModes;
	private final List<String> availableModesWithoutCar;
	private final boolean considerCarAvailability;

	@Inject
	public PermissibleModesCalculatorImpl(Config config) {
		this.availableModes = Arrays.asList(config.subtourModeChoice().getModes());

		if (this.availableModes.contains(TransportMode.car)) {
			final List<String> l = new ArrayList<String>(this.availableModes);
			while (l.remove(TransportMode.car)) {
			}
			this.availableModesWithoutCar = Collections.unmodifiableList(l);
		} else {
			this.availableModesWithoutCar = this.availableModes;
		}

		this.considerCarAvailability = config.subtourModeChoice().considerCarAvailability();
	}

	@Override
	public Collection<String> getPermissibleModes(final Plan plan) {
		if (!considerCarAvailability) return availableModes;

		final Person person;
		try {
			person = plan.getPerson();
		}
		catch (ClassCastException e) {
			throw new IllegalArgumentException( "I need a PersonImpl to get car availability" );
		}

		final boolean carAvail =
			!"no".equals( PersonUtils.getLicense(person) ) &&
			!"never".equals( PersonUtils.getCarAvail(person) );

		return carAvail ? availableModes : availableModesWithoutCar;
	}
}
