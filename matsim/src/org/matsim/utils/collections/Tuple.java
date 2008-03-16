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

package org.matsim.utils.collections;

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
public class Tuple<A extends Object, B extends Object> {
	/**
	 * First entry of the tuple
	 */
	private A first;
	/**
	 * Second entry of the tuple
	 */
	private B second;
	/**
	 * Creates a new tuple with the two entries.
	 * @param first
	 * @param second
	 */
	public Tuple(A first, B second) {
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
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Tuple)) return false;
		return this.first.equals(((Tuple)other).first) && this.second.equals(((Tuple)other).second);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.first.hashCode() + this.second.hashCode();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("[Tuple: [First: " );
		buffer.append(this.first.toString());
		buffer.append("], [Second: ");
		buffer.append(this.second.toString());
		buffer.append("]]");
		return buffer.toString();
	}

}
