/* *********************************************************************** *
 * project: org.matsim.*
 * Value.java
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
 * Represents an individual value in a solution.
 * It should be cloneable, so that modifying the value
 * of the clone does not modify the cloned.
 * Clone could be needed by some "operators" (tabu checker mainly),
 * to test or remember the result of a move without modifying the base
 * solution.
 * It is also used to remember the best solution so far.
 * <br>
 * It must implement equals and hashCode
 *
 * @author thibautd
 */
public interface Value {
	/**
	 * returns the value.
	 * @return the value.
	 */
	public Object getValue();

	/**
	 * Sets the value.
	 * @param newValue the new value
	 * @return the old value.
	 * @throws IllegalArgumentException if the value is not allowed (for example,
	 * a double value may be bounded, or the new value may be checked for its type)
	 */
	public Object setValue(Object newValue);

	public Value createClone();
}
