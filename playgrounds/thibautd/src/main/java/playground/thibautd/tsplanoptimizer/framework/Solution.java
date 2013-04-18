/* *********************************************************************** *
 * project: org.matsim.*
 * Solution.java
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

import java.util.List;

/**
 * Represents an object to optimize (usually a plan),
 * under the form of a sequence of values.
 * The clone method must copy the representation.
 * <br>
 * It must implement equals and hashCode
 * @author thibautd
 */
public interface Solution<T> {
	/**
	 * Gives access to the representation, under the form of an ordered
	 * list of values. The values must be internal references (modifying
	 * them modifies the represented plan), the list may not (elements
	 * should not be added or removed).
	 *
	 * @return the internal representation.
	 */
	public List<? extends Value> getGenotype();

	/**
	 * Returns the plan represented by this solution.
	 * It must <b>not</b> be modified by the moves. Moves acting directly on the plan
	 * should use representations with Value\<Plan\> values.
	 * <br>
	 * However, to avoid both side effects and high computational cost,
	 * it is recomended to just copy the reference to the plan to optimize,
	 * and to modify its properties according to the representation in this
	 * method.
	 *
	 * @return the plan, which may be a newly created plan or a modification
	 * of a plan used at initialisation (in both case, it should be indicated in
	 * the documentation)
	 */
	public T getPhenotype();

	/**
	 * Clones the solution.
	 * @return a deep copy (i.e. modifying the copy should not affect the copied
	 * in any sense)
	 */
	public Solution<T> createClone();
}

