/* *********************************************************************** *
 * project: org.matsim.*
 * ValueImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.tsplanoptimizer.framework;

/**
 * Default generic implementation of a {@link Value}. 
 * It is actually not that nice: it is better to use
 * non-generic implementations.
 * @author thibautd
 */
public class ValueImpl<T> implements Value<T> {
	private T value;

	public ValueImpl(final T value) {
		this.value = value;
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public T setValue(final T newValue) {
		T old = value;
		this.value = newValue;
		return old;
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof Value) {
			Object otherValue = ((Value) other).getValue();
			return value.equals( otherValue );
		}

		return false;
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public Value<T> createClone() {
		return new ValueImpl<T>( value );
	}

	@Override
	public String toString() {
		return "ValueImpl<"+value.getClass().getSimpleName()+">="+value;
	}
}

