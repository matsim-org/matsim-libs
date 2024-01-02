/* *********************************************************************** *
 * project: org.matsim.*
 * Tuple.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.collections;

import java.io.Serializable;

/**
 * A Tuple stores two values (a "pair") and respects their order.
 * This generic class implements a commonly used data structure which is not present in
 * the current collection framework. Although it could be simulated with a List containing
 * two Objects, this implementation offers type safety and maximizes convenience for programmers.
 *
 * @author dgrether
 *
 * @param <A>
 * @param <B>
 */
public final class Tuple<A, B> implements Serializable {
	private static final long serialVersionUID = 1L;

	public static <A, B> Tuple<A, B> of(final A first, final B second) {
		return new Tuple<>(first, second);
	}

	/**
	 * First entry of the tuple
	 */
	private final A first;
	/**
	 * Second entry of the tuple
	 */
	private final B second;
	/**
	 * Creates a new tuple with the two entries.
	 * @param first
	 * @param second
	 */
	public Tuple(final A first, final B second) {
		this.first = first;
		this.second = second;
	}

	public A getFirst() {
		return this.first;
	}

	public B getSecond() {
		return this.second;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object other) {
		if (!(other instanceof Tuple)) return false;
		Tuple o = (Tuple) other;
		if (this.first != null && this.second != null && o.first != null && o.second != null) {
			return (this.first.equals(o.first) && this.second.equals(o.second));
		}
		boolean firstEquals = (this.first == null) && (o.first == null);
		boolean secondEquals = (this.second == null) && (o.second == null);
		if (!firstEquals && this.first != null && o.first != null) {
			firstEquals = this.first.equals(o.first);
		}
		if (!secondEquals && this.second != null && o.second != null) {
			secondEquals = this.second.equals(o.second);
		}
		return firstEquals && secondEquals;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return (this.first == null ? 0 : this.first.hashCode()) +
				(this.second == null ? 0 : this.second.hashCode());
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder(50);
		buffer.append("[Tuple: [First: " );
		buffer.append(this.first.toString());
		buffer.append("], [Second: ");
		buffer.append(this.second.toString());
		buffer.append("]]");
		return buffer.toString();
	}

}
