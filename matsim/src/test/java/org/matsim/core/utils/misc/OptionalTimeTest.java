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

import static org.assertj.core.api.Assertions.*;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.jupiter.api.Test;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OptionalTimeTest {
	@Test
	void test_defined_seconds() {
		//defined
		assertThat(OptionalTime.defined(0).seconds()).isEqualTo(0);
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
	void test_undefined_seconds() {
		assertThat(OptionalTime.undefined().isUndefined()).isTrue();

		assertThatThrownBy(() -> OptionalTime.undefined().seconds()).isExactlyInstanceOf(NoSuchElementException.class)
				.hasMessage("Undefined time");
	}

	@Test
	void test_cachedValues() {
		//currently 0 and undefined are cached
		assertThat(OptionalTime.defined(0)).isSameAs(OptionalTime.defined(0));
		assertThat(OptionalTime.undefined()).isSameAs(OptionalTime.undefined());
	}

	@Test
	void test_isUndefined() {
		assertThat(OptionalTime.undefined().isUndefined()).isTrue();
		assertThat(OptionalTime.defined(1).isUndefined()).isFalse();
	}

	@Test
	void test_isDefined() {
		assertThat(OptionalTime.undefined().isDefined()).isFalse();
		assertThat(OptionalTime.defined(1).isDefined()).isTrue();
	}

	@Test
	void test_orElse() {
		assertThat(OptionalTime.undefined().orElse(0)).isEqualTo(0);
		assertThat(OptionalTime.defined(1).orElse(0)).isEqualTo(1);
	}

	@Test
	void test_orElseGet() {
		assertThat(OptionalTime.undefined().orElseGet(() -> 0)).isEqualTo(0);
		assertThat(OptionalTime.defined(1).orElseGet(() -> 0)).isEqualTo(1);
	}

	@Test
	void test_orElseThrow() {
		assertThatThrownBy(() -> OptionalTime.undefined()
				.orElseThrow(() -> new IllegalStateException("Undefined time error"))).isExactlyInstanceOf(
				IllegalStateException.class).hasMessage("Undefined time error");

		assertThatCode(() -> OptionalTime.defined(1)
				.orElseThrow(() -> new IllegalStateException("Undefined time error"))).doesNotThrowAnyException();
	}

	@Test
	void test_ifDefined() {
		MutableDouble counter = new MutableDouble(0);

		OptionalTime.undefined().ifDefined(counter::add);
		assertThat(counter.doubleValue()).isEqualTo(0);

		OptionalTime.defined(10).ifDefined(counter::add);
		assertThat(counter.doubleValue()).isEqualTo(10);
	}

	@Test
	void test_ifDefinedOrElse() {
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
	void test_stream() {
		assertThat(OptionalTime.undefined().stream()).containsExactly();
		assertThat(OptionalTime.defined(0).stream()).containsExactly(0.);
		assertThat(OptionalTime.defined(10).stream()).containsExactly(10.);
	}

	@Test
	void test_or_OptionalTime() {
		assertThat(OptionalTime.undefined().or(OptionalTime.undefined()).isUndefined()).isTrue();
		assertThat(OptionalTime.undefined().or(OptionalTime.defined(3)).seconds()).isEqualTo(3);
		assertThat(OptionalTime.defined(1).or(OptionalTime.undefined()).seconds()).isEqualTo(1);
		assertThat(OptionalTime.defined(1).or(OptionalTime.defined(2)).seconds()).isEqualTo(1);

		assertThatThrownBy(() -> OptionalTime.undefined().or((OptionalTime)null)).isExactlyInstanceOf(
				NullPointerException.class);
	}

	@Test
	void test_or_OptionalTimeSupplier() {
		assertThat(OptionalTime.undefined().or(OptionalTime::undefined).isUndefined()).isTrue();
		assertThat(OptionalTime.undefined().or(() -> OptionalTime.defined(3)).seconds()).isEqualTo(3);
		assertThat(OptionalTime.defined(1).or(OptionalTime::undefined).seconds()).isEqualTo(1);
		assertThat(OptionalTime.defined(1).or(() -> OptionalTime.defined(2)).seconds()).isEqualTo(1);

		assertThatThrownBy(() -> OptionalTime.undefined().or((Supplier<OptionalTime>)null)).isExactlyInstanceOf(
				NullPointerException.class);
		assertThatThrownBy(() -> OptionalTime.undefined().or(() -> null)).isExactlyInstanceOf(
				NullPointerException.class);
	}

	@Test
	void test_equals() {
		assertThat(OptionalTime.undefined()).isEqualTo(OptionalTime.undefined());
		assertThat(OptionalTime.undefined()).isNotEqualTo(OptionalTime.defined(0));

		assertThat(OptionalTime.defined(0)).isNotEqualTo(OptionalTime.undefined());
		assertThat(OptionalTime.defined(0)).isEqualTo(OptionalTime.defined(0));
		assertThat(OptionalTime.defined(0)).isNotEqualTo(OptionalTime.defined(1));
	}

	@Test
	void test_hashCode() {
		assertThat(OptionalTime.undefined()).hasSameHashCodeAs(Time.UNDEFINED_TIME);
		assertThat(OptionalTime.defined(0)).hasSameHashCodeAs(0.);
		assertThat(OptionalTime.defined(-Double.MAX_VALUE)).hasSameHashCodeAs(-Double.MAX_VALUE);
		assertThat(OptionalTime.defined(Double.POSITIVE_INFINITY)).hasSameHashCodeAs(Double.POSITIVE_INFINITY);
	}
}