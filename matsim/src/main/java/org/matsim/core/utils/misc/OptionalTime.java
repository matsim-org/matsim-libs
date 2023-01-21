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
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public final class OptionalTime {
	//TODO we could store an array of all "reasonable" times, e.g. 0 to 100_000 ???

	//cached values:
	private static final OptionalTime UNDEFINED = new OptionalTime(Time.UNDEFINED_TIME);
	private static final OptionalTime TIME_0 = new OptionalTime(0);

	public static void assertDefined(double seconds) {
		if (seconds == Time.UNDEFINED_TIME) {
			throw new IllegalArgumentException("Undefined time is not allowed");
		} else if (Double.isNaN(seconds)) {
			throw new IllegalArgumentException("NaN time is not allowed");
		}
	}

	/**
	 * Creates OptionalTime that wraps only a defined time
	 *
	 * @throws IllegalArgumentException if seconds is Double.NaN or Time.getUndefined()
	 */
	public static OptionalTime defined(double seconds) {
		if (seconds == 0) {
			return TIME_0;
		}
		assertDefined(seconds);
		return new OptionalTime(seconds);
	}

	public static OptionalTime undefined() {
		return UNDEFINED;
	}

	public static OptionalTime zeroSeconds() {
		return TIME_0;
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

	public <X extends Throwable> double orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
		if (seconds == Time.UNDEFINED_TIME) {
			throw exceptionSupplier.get();
		}
		return seconds;
	}

	public void ifDefined(DoubleConsumer action) {
		if (seconds != Time.UNDEFINED_TIME) {
			action.accept(seconds);
		}
	}

	public void ifDefinedOrElse(DoubleConsumer action, Runnable undefinedAction) {
		if (seconds != Time.UNDEFINED_TIME) {
			action.accept(seconds);
		} else {
			undefinedAction.run();
		}
	}

	public DoubleStream stream() {
		return seconds != Time.UNDEFINED_TIME ? DoubleStream.of(seconds) : DoubleStream.empty();
	}

	public OptionalTime or(OptionalTime optionalTime) {
		Preconditions.checkNotNull(optionalTime);
		return seconds != Time.UNDEFINED_TIME ? this : optionalTime;
	}

	public OptionalTime or(Supplier<OptionalTime> supplier) {
		Preconditions.checkNotNull(supplier);
		return seconds != Time.UNDEFINED_TIME ? this : Preconditions.checkNotNull(supplier.get());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof OptionalTime)) {
			return false;
		}
		return seconds == ((OptionalTime)o).seconds; // none of them is NaN
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
