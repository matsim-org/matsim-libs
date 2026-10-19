/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.passenger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Combines several {@link PassengerRequestValidator}s into one, returning the union of their causes. Thread safety
 * follows from the delegates, which are required to be thread-safe themselves.
 *
 * @author nkuehnel / MOIA
 */
public class CompositePassengerRequestValidator implements PassengerRequestValidator {

	private final List<PassengerRequestValidator> validators;

	public CompositePassengerRequestValidator(PassengerRequestValidator... validators) {
		this(Arrays.asList(validators));
	}

	public CompositePassengerRequestValidator(List<PassengerRequestValidator> validators) {
		this.validators = List.copyOf(validators);
	}

	@Override
	public Set<String> validateRequest(PassengerRequest request) {
		Set<String> causes = new HashSet<>();
		for (PassengerRequestValidator validator : validators) {
			causes.addAll(validator.validateRequest(request));
		}
		return causes;
	}
}
