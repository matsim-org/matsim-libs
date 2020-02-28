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

import java.util.NoSuchElementException;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.Test;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OptionalTimeTest {
	@Test
	public void test_defined() {
		//defined
		assertThat(OptionalTime.defined(0).seconds()).isEqualTo(0);
		assertThat(OptionalTime.defined(0)).isSameAs(OptionalTime.defined(0));//cached, so the same

		assertThat(OptionalTime.defined(1).seconds()).isEqualTo(1);
		assertThat(OptionalTime.defined(-Double.MAX_VALUE).seconds()).isEqualTo(-Double.MAX_VALUE);
		assertThat(OptionalTime.defined(Double.POSITIVE_INFINITY).seconds()).isEqualTo(Double.POSITIVE_INFINITY);

		//undefined
		assertThatThrownBy(() -> OptionalTime.defined(Time.UNDEFINED_TIME)).isExactlyInstanceOf(
				IllegalArgumentException.class).hasMessage("Undefined time is not allowed");

		//NaN
		assertThatThrownBy(() -> OptionalTime.defined(Double.NaN)).isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessage("NaN time is not allowed");
	}

	@Test
	public void test_undefined() {
		assertThat(OptionalTime.undefined().isUndefined()).isTrue();

		//undefined OptionalTime is cached
		assertThat(OptionalTime.undefined()).isSameAs(OptionalTime.undefined());

		assertThatThrownBy(() -> OptionalTime.undefined().seconds()).isExactlyInstanceOf(NoSuchElementException.class)
				.hasMessage("Undefined time");
	}

	@Test
	public void test_isUndefined() {
		assertThat(OptionalTime.undefined().isUndefined()).isTrue();
		assertThat(OptionalTime.defined(1).isUndefined()).isFalse();
	}

	@Test
	public void test_isDefined() {
		assertThat(OptionalTime.undefined().isDefined()).isFalse();
		assertThat(OptionalTime.defined(1).isDefined()).isTrue();
	}

	@Test
	public void test_orElse() {
		assertThat(OptionalTime.undefined().orElse(0)).isEqualTo(0);
		assertThat(OptionalTime.defined(1).orElse(0)).isEqualTo(1);
	}

	@Test
	public void test_orElseGet() {
		assertThat(OptionalTime.undefined().orElseGet(() -> 0)).isEqualTo(0);
		assertThat(OptionalTime.defined(1).orElseGet(() -> 0)).isEqualTo(1);
	}

	@Test
	public void test_ifDefined() {
		MutableDouble counter = new MutableDouble(0);

		OptionalTime.undefined().ifDefined(counter::add);
		assertThat(counter.doubleValue()).isEqualTo(0);

		OptionalTime.defined(10).ifDefined(counter::add);
		assertThat(counter.doubleValue()).isEqualTo(10);
	}

	@Test
	public void test_ifDefinedOrElse() {
		MutableDouble ifCounter = new MutableDouble(0);
		MutableDouble elseCounter = new MutableDouble(0);

		OptionalTime.undefined().ifDefinedOrElse(ifCounter::add, elseCounter::increment);
		assertThat(ifCounter.doubleValue()).isEqualTo(0);
		assertThat(elseCounter.doubleValue()).isEqualTo(1);

		OptionalTime.defined(10).ifDefinedOrElse(ifCounter::add, elseCounter::increment);
		assertThat(ifCounter.doubleValue()).isEqualTo(10);
		assertThat(elseCounter.doubleValue()).isEqualTo(1);
	}

	@Test
	public void test_stream() {
		assertThat(OptionalTime.undefined().stream()).containsExactly();
		assertThat(OptionalTime.defined(0).stream()).containsExactly(0.);
		assertThat(OptionalTime.defined(10).stream()).containsExactly(10.);
	}

	@Test
	public void test_equals() {
		assertThat(OptionalTime.undefined()).isEqualTo(OptionalTime.undefined());
		assertThat(OptionalTime.undefined()).isNotEqualTo(OptionalTime.defined(0));

		assertThat(OptionalTime.defined(0)).isNotEqualTo(OptionalTime.undefined());
		assertThat(OptionalTime.defined(0)).isEqualTo(OptionalTime.defined(0));
		assertThat(OptionalTime.defined(0)).isNotEqualTo(OptionalTime.defined(1));
	}

	@Test
	public void test_hashCode() {
		assertThat(OptionalTime.undefined()).hasSameHashCodeAs(Time.UNDEFINED_TIME);
		assertThat(OptionalTime.defined(0)).hasSameHashCodeAs(0.);
		assertThat(OptionalTime.defined(-Double.MAX_VALUE)).hasSameHashCodeAs(-Double.MAX_VALUE);
		assertThat(OptionalTime.defined(Double.POSITIVE_INFINITY)).hasSameHashCodeAs(Double.POSITIVE_INFINITY);
	}
}