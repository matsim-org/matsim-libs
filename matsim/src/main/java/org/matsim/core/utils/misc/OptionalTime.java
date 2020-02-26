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

import java.util.NoSuchElementException;
import java.util.function.DoubleSupplier;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class OptionalTime {
	//TODO we could store an array of all "reasonable" times, e.g. 0 to 100_000 ???

	private static OptionalTime UNDEFINED = new OptionalTime(Time.UNDEFINED_TIME);


	/**
	 * Creates OptionalTime that wraps a defined or undefined time
	 *
	 * @throws IllegalArgumentException if seconds is Double.NaN
	 */
	public static OptionalTime of(double seconds) {
		if (Double.isNaN(seconds)) {
			throw new IllegalArgumentException("NaN time is not allowed");
		}

		return seconds == Time.UNDEFINED_TIME ? UNDEFINED : new OptionalTime(seconds);
	}

	/**
	 * Creates OptionalTime that wraps only a defined time
	 *
	 * @throws IllegalArgumentException if seconds is Double.NaN or Time.getUndefined()
	 */
	public static OptionalTime defined(double seconds) {
		if (seconds == Time.UNDEFINED_TIME) {
			throw new IllegalArgumentException("Undefined time is not allowed");
		}
		return of(seconds);
	}

	public static OptionalTime undefined() {
		return UNDEFINED;
	}

	private final double seconds;

	private OptionalTime(double seconds) {
		this.seconds = seconds;
	}

	public double seconds() {
		if (seconds == Time.UNDEFINED_TIME) {
			throw new NoSuchElementException("Undefined time");
		}
		return seconds;
	}

	public boolean isDefined() {
		return seconds != Time.UNDEFINED_TIME;
	}

	public boolean isUndefined() {
		return seconds == Time.UNDEFINED_TIME;
	}

	public double orElse(double other) {
		return seconds != Time.UNDEFINED_TIME ? seconds : other;
	}

	public double orElseGet(DoubleSupplier supplier) {
		return seconds != Time.UNDEFINED_TIME ? seconds : supplier.getAsDouble();
	}

	public double orElseUndefined() {
		return seconds;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		OptionalTime that = (OptionalTime)o;
		return seconds != that.seconds; // none of them is NaN
	}

	@Override
	public int hashCode() {
		return Double.hashCode(seconds);
	}

	@Override
	public String toString() {
		return seconds != Time.UNDEFINED_TIME ? String.format("OptionalTime[%s]", seconds) : "OptionalTime[UNDEFINED]";
	}
}
