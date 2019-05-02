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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class BeanValidationConfigConsistencyCheckerTest {

	@Test
	public void checkConsistency() {
		Assertions.assertThat(getViolations(new Config())).isEmpty();

		Config config = ConfigUtils.createConfig();
		Assertions.assertThat(getViolations(config)).isEmpty();

		{
			config.qsim().setFlowCapFactor(0);
			Set<ConstraintViolation<?>> violations = getViolations(config);
			Assertions.assertThat(violations).hasSize(1);
			assertViolation(violations.iterator().next(), "flowCapFactor", Positive.class);
			config.qsim().setFlowCapFactor(1);
		}
		{
			config.qsim().setSnapshotPeriod(-1);
			Set<ConstraintViolation<?>> violations = getViolations(config);
			Assertions.assertThat(violations).hasSize(1);
			assertViolation(violations.iterator().next(), "snapshotPeriod", PositiveOrZero.class);
			config.qsim().setSnapshotPeriod(0);
		}
	}

	private Set<ConstraintViolation<?>> getViolations(Config config) {
		try {
			new BeanValidationConfigConsistencyChecker().checkConsistency(config);
			return Collections.emptySet();
		} catch (ConstraintViolationException e) {
			return e.getConstraintViolations();
		}
	}

	private void assertViolation(ConstraintViolation<?> violation, String path,
			Class<? extends Annotation> annotationType) {
		Assertions.assertThat(violation.getPropertyPath().toString()).isEqualTo(path);
		Assertions.assertThat(violation.getConstraintDescriptor().getAnnotation().annotationType())
				.isEqualTo(annotationType);
	}
}
