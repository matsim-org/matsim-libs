/* *********************************************************************** *
 * project: org.matsim.*
 * Objective.java
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

package playground.andreas.optimization;

import java.util.TreeMap;


/**
 * Interface for objective functions for use with SimplexOptimization.
 *
 */
public interface Objective {
	/**
	 * returns a point in the parameter space. the point must be valid and can
	 * be randomly generated or just have default values. Complex objective
	 * function can define a subclass of ParamPoint to store additional needed
	 * values and return here an object of that sublass.
	 *
	 * @return a valid point in the parameter space
	 */
	public ParamPoint getNewParamPoint();

	/**
	 * calculates a response on how good the parameter values in <pre>p</pre> fulfill
	 * the objective. Returned values should be zero or higher. Lower values score
	 * better than higher values.
	 *
	 * @param p the parameter-settings for which the objective should be calculated
	 * @return a non-negative number
	 */
	public double getResponse(ParamPoint p);

	/**
	 * return an initial point in the parameter space of the objective.
	 * The returned point must be valid, see @see isValidParamPoint
	 * All returned initial parameters must be linear independent.
	 *
	 * @param index the number of the n-th initial parameter settings to be generated
	 * @return the index-th initial parameter settings
	 */
	public ParamPoint getInitialParamPoint(int index);

	public void setInitParamPoint(ParamPoint p, int i);

	/**
	 * verifies that the parameter given in <pre>p</pre> are valid in the objective's
	 * parameter space
	 *
	 * @param p a point in the parameter space
	 * @return true if <pre>p</pre> is valid, false otherwise
	 */
	public boolean isValidParamPoint(ParamPoint p);


	/**
	 * generates a map with pairs of param-name and corresponding param-value.
	 * So it basically gives the values in ParamPoint p a descriptive label.
	 *
	 * @param p the values to create a map for
	 * @return map containing pairs of param-name and param-value
	 */
	public TreeMap<String, Double> getParamMap(ParamPoint p);

}
