/* *********************************************************************** *
 * project: org.matsim.*
 * FitnessFunction.java
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
 * Scores a solution
 * @author thibautd
 */
public interface FitnessFunction<T> extends Listener<T> {
	/**
	 * Computes the score
	 * @param solution the solution to score
	 * @return the score
	 */
	public double computeFitnessValue( Solution<? extends T> solution );
}

