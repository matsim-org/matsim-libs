/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.misc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OptionalTimesTest {
	@Test
	public void requireDefined() {
		assertThatThrownBy(() -> OptionalTimes.requireDefined(OptionalTime.undefined())).isExactlyInstanceOf(
				IllegalArgumentException.class).hasMessage("Time must be defined");

		assertThat(OptionalTimes.requireDefined(OptionalTime.defined(1)).seconds()).isEqualTo(1);
	}
	
	@Test
	public void testAdd() {
		assertThat(OptionalTimes.add(OptionalTime.undefined(), OptionalTime.undefined()).isUndefined()).isEqualTo(true);
		assertThat(OptionalTimes.add(OptionalTime.undefined(), OptionalTime.defined(10)).isUndefined()).isEqualTo(true);
		assertThat(OptionalTimes.add(OptionalTime.defined(7.25), OptionalTime.undefined()).isUndefined()).isEqualTo(true);
		assertThat(OptionalTimes.add(OptionalTime.defined(65), OptionalTime.defined(12)).seconds()).isEqualTo(77);
		assertThat(OptionalTimes.add(OptionalTime.defined(-85), OptionalTime.defined(0.6)).seconds()).isEqualTo(-84.4);
	}
	
	@Test
	public void testSubtract() {
		assertThat(OptionalTimes.subtract(OptionalTime.undefined(), OptionalTime.undefined()).isUndefined()).isEqualTo(true);
		assertThat(OptionalTimes.subtract(OptionalTime.undefined(), OptionalTime.defined(10)).isUndefined()).isEqualTo(true);
		assertThat(OptionalTimes.subtract(OptionalTime.defined(7.25), OptionalTime.undefined()).isUndefined()).isEqualTo(true);
		assertThat(OptionalTimes.subtract(OptionalTime.defined(65), OptionalTime.defined(12)).seconds()).isEqualTo(53);
		assertThat(OptionalTimes.subtract(OptionalTime.defined(-85), OptionalTime.defined(0.6)).seconds()).isEqualTo(-85.6);
	}
}