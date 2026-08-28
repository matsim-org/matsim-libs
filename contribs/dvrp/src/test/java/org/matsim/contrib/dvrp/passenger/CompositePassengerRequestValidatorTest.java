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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * @author nkuehnel / MOIA
 */
public class CompositePassengerRequestValidatorTest {

	private final PassengerRequest request = mock(PassengerRequest.class);

	@Test
	void testAllValidatorsAcceptTheRequest() {
		var validator = new CompositePassengerRequestValidator(r -> Set.of(), r -> Set.of());
		assertThat(validator.validateRequest(request)).isEmpty();
	}

	@Test
	void testCausesOfAllValidatorsAreUnited() {
		var validator = new CompositePassengerRequestValidator(r -> Set.of("cause_a"),
				r -> Set.of("cause_b", "cause_a"));
		assertThat(validator.validateRequest(request)).containsExactlyInAnyOrder("cause_a", "cause_b");
	}

	@Test
	void testWithoutValidatorsEverythingIsAccepted() {
		var validator = new CompositePassengerRequestValidator(List.of());
		assertThat(validator.validateRequest(request)).isEmpty();
	}
}
