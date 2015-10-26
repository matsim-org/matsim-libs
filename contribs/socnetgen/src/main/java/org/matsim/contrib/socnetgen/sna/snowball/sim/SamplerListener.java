/* *********************************************************************** *
 * project: org.matsim.*
 * TerminationCondition.java
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
package org.matsim.contrib.socnetgen.sna.snowball.sim;

import org.matsim.contrib.socnetgen.sna.snowball.SampledVertexDecorator;

/**
 * Representation of an object that "listens" to the sampling process and is
 * able to terminate the snowball.
 * 
 * @author illenberger
 * 
 */
public interface SamplerListener {

	/**
	 * Called before a vertex is sampled.
	 * 
	 * @param sampler
	 *            A snowball sampler.
	 * @param vertex
	 *            The vertex to be sampled.
	 * @return <tt>true</tt> if the sampling process should continue,
	 *         <tt>false</tt> otherwise.
	 */
	public boolean beforeSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex);

	/**
	 * Called after a vertex has been sampled.
	 * 
	 * @param sampler
	 *            A snowball sampler.
	 * @param vertex
	 *            The last sampled vertex.
	 * @return <tt>true</tt> if the sampling process should continue,
	 *         <tt>false</tt> otherwise.
	 */
	public boolean afterSampling(Sampler<?, ?, ?> sampler, SampledVertexDecorator<?> vertex);

	/**
	 * Called after the sampling process has been terminated.
	 * 
	 * @param sampler
	 *            A snowball sampler.
	 */
	public void endSampling(Sampler<?, ?, ?> sampler);

}
