/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.core.config.consistency;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

/**
 * @author Michal Maciejewski (michalm)
 */
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
				messages.add((messages.size() + 1)
						+ ") "
						+ group.getClass().getName()
						+ "(name="
						+ group.getName()
						+ ")."
						+ v.getPropertyPath()
						+ ": "
						+ v.getMessage());
			}
		}

		if (!violations.isEmpty()) {
			String message = messages.size() + " error(s) found in the config:\n" + messages.stream()
					.collect(Collectors.joining("\n"));
			throw new ConstraintViolationException(message, violations);
		}
	}
}
