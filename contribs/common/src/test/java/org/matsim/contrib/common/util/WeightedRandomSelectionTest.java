/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package org.matsim.contrib.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author michalm
 */
public class WeightedRandomSelectionTest {
	private final double EPSILON = 1e-15;
	private final MutableDouble randomDouble = new MutableDouble();
	private WeightedRandomSelection<String> weightedRandomSelection;

	@BeforeEach
	public void init() {
		weightedRandomSelection = new WeightedRandomSelection<>(new Random() {
			@Override
			public double nextDouble(double from, double to) {
				return randomDouble.doubleValue();
			}
		});
	}

	@Test
	void testIncorrectInput() {
		assertIncorrectWeight(-Double.MIN_VALUE);
		assertIncorrectWeight(-1);
		assertIncorrectWeight(-Double.MAX_VALUE);
		assertIncorrectWeight(Double.NEGATIVE_INFINITY);
		assertIncorrectWeight(Double.POSITIVE_INFINITY);
		assertIncorrectWeight(Double.NaN);

		weightedRandomSelection.add("A", 0);
		weightedRandomSelection.add("B", Double.MAX_VALUE);

		assertThatThrownBy(() -> weightedRandomSelection.add(null, Double.MAX_VALUE))//
				.isExactlyInstanceOf(ArithmeticException.class)//
				.hasMessage("Total weight is infinite");
	}

	private void assertIncorrectWeight(double weight) {
		assertThatThrownBy(() -> weightedRandomSelection.add(null, weight))//
				.isExactlyInstanceOf(IllegalArgumentException.class)//
				.hasMessage("Weight must be non-negative and finite");
	}

	@Test
	void testEmptyList() {
		assertThat(weightedRandomSelection.size()).isEqualTo(0);
		assertThatThrownBy(() -> weightedRandomSelection.select())//
				.isExactlyInstanceOf(IllegalStateException.class)//
				.hasMessage("No entries in the list to select from");
	}

	@Test
	void testZeroTotalWeight() {
		weightedRandomSelection.add("A", 0.);
		weightedRandomSelection.add("B", 0.);
		assertThatThrownBy(() -> weightedRandomSelection.select())//
				.isExactlyInstanceOf(IllegalStateException.class)//
				.hasMessage("Total weight is not positive");
	}

	@Test
	void testSingleValueList() {
		weightedRandomSelection.add("A", 1);
		assertThat(weightedRandomSelection.size()).isEqualTo(1);

		assertSelectedValue(0, "A");
		assertSelectedValue(0.5, "A");
		assertSelectedValue(1, "A");

		assertIndexOutOfBoundsException(1 + EPSILON, 1);
	}

	@Test
	void testThreeValuesList() {
		weightedRandomSelection.add("A", 1);
		weightedRandomSelection.add("B", 0.5);
		weightedRandomSelection.add("C", 1.5);
		assertThat(weightedRandomSelection.size()).isEqualTo(3);

		assertSelectedValue(0, "A");
		assertSelectedValue(0.5, "A");
		assertSelectedValue(1, "A");
		assertSelectedValue(1 + EPSILON, "B");
		assertSelectedValue(1.5, "B");
		assertSelectedValue(1.5 + EPSILON, "C");
		assertSelectedValue(2, "C");
		assertSelectedValue(2.5, "C");
		assertSelectedValue(3, "C");

		assertIndexOutOfBoundsException(3 + EPSILON, 3);
	}

	private void assertSelectedValue(double nextDouble, String expected) {
		randomDouble.setValue(nextDouble);
		assertThat(weightedRandomSelection.select()).isEqualTo(expected);
	}

	private void assertIndexOutOfBoundsException(double nextDouble, int index) {
		randomDouble.setValue(nextDouble);
		assertThatThrownBy(() -> weightedRandomSelection.select())//
				.isExactlyInstanceOf(IndexOutOfBoundsException.class);
	}
}
