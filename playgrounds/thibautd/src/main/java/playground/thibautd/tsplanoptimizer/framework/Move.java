/* *********************************************************************** *
 * project: org.matsim.*
 * Move.java
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
 * Represents a Tabu Search transformation.
 * Implementations must provide the equals and hashCode methods.
 *
 * @author thibautd
 */
public interface Move {

	/**
	 * Creates a new Solution, corresponding to a start position to which the move is applied.
	 * @param solution the solution to start from. The instance must not be modified!
	 * @return a <b>new</b> instance representing the modified solution.
	 */
	public <T> Solution<T> apply( Solution<T> solution );

	/**
	 * Gives access to the reverse move. The reverse move <tt>-m</tt> for a given move
	 * <tt>m</tt>  is the move such that, applying <tt>m</tt> and than <tt>-m</tt>
	 * lets the solution unchanged.
	 * 
	 * If it is not applicable, the method should throw an {@link UnsupportedOperationException}.
	 *
	 * @return the reverse move
	 */
	public Move getReverseMove();
}

