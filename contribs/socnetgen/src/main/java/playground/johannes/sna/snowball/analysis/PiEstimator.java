/* *********************************************************************** *
 * project: org.matsim.*
 * Estimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.sna.snowball.analysis;

import playground.johannes.sna.snowball.SampledGraph;
import playground.johannes.sna.snowball.SampledVertex;

/**
 * A ProbabilityEstimator estimates the inclusion probability of a vertex in a
 * snowball sample.
 * 
 * @author illenberger
 * 
 */
public interface PiEstimator {

	/**
	 * Notifies the estimator that the sample has changed.
	 * 
	 * @param graph
	 *            the sampled snwoball graph
	 */
	public void update(SampledGraph graph);

	/**
	 * Returns the inclusion probability of a vertex in the snowball sample.
	 * 
	 * @param vertex
	 *            a sampled vertex
	 * @return the inclusion probability of a vertex in the snowball sample.
	 */
	public double probability(SampledVertex vertex);
	
	/**
	 * Returns the inclusion probability of a vertex in the snowball sample.
	 * 
	 * @param vertex
	 *            a sampled vertex
	 * @param iteration
	 *            the snowball iteration
	 * 
	 * @return the inclusion probability of a vertex in the snowball sample of
	 *         iteration <tt>iteration</tt>.
	 */
	public double probability(SampledVertex vertex, int iteration);

}
