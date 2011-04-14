/* *********************************************************************** *
 * project: org.matsim.*
 * RateCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.replanning.modules;

/**
 * Interface for all classes allowing dynamic rates for several operators at the
 * same time, based on fitness information.
 * @author thibautd
 */
public interface RateCalculator {
	/**
	 * @return an array containing the rates for the different operators.
	 */
	public double[] getRates();

	/**
	 * Notify the result of an operation.
	 */
	public void addResult(
			int operatorIndex,
			double fitnessParent,
			double fitnessChild);

	/**
	 * Notify the end of the iteration.
	 */
	public void iterationIsOver();
}

