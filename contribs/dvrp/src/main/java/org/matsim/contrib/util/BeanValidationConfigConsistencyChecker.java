/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.consistency.ConfigConsistencyChecker;

public class BeanValidationConfigConsistencyChecker implements ConfigConsistencyChecker {
	private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Override
	public void checkConsistency(Config config) {
		Set<ConstraintViolation<ConfigGroup>> violations = new HashSet<>();
		List<String> messages = new ArrayList<>();

		for (ConfigGroup group : config.getModules().values()) {
			Set<ConstraintViolation<ConfigGroup>> groupViolations = validator.validate(group);
			violations.addAll(groupViolations);
			for (ConstraintViolation<ConfigGroup> v : groupViolations) {
				messages.add(group.getName() + "." + v.getPropertyPath() + ": " + v.getMessage());
			}
		}

		if (!violations.isEmpty()) {
			String message = "Errors in config:\n" + messages.stream().collect(Collectors.joining("\n"));
			throw new ConstraintViolationException(message, violations);
		}
	}
}
