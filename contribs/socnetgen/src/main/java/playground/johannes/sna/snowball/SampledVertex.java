/* *********************************************************************** *
 * project: org.matsim.*
 * SampledVertex.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.johannes.sna.snowball;

import java.util.List;

import playground.johannes.sna.graph.Vertex;

/**
 * Representation of a snowball sampled vertex.
 * 
 * @author illenberger
 * 
 */
public interface SampledVertex extends Vertex {

	/**
	 * @see {@link Vertex#getEdges()}
	 */
	public List<? extends SampledEdge> getEdges();

	/**
	 * @see {@link Vertex#getNeighbours()}
	 */
	public List<? extends SampledVertex> getNeighbours();

	/**
	 * @see {@link SnowballAttributes#detect(Integer)}
	 */
	public void detect(Integer iteration);

	/**
	 * @see {@link SnowballAttributes#getIterationDeteted()}
	 */
	public Integer getIterationDetected();

	/**
	 * @see {@link SnowballAttributes#isDetected()}
	 */
	public boolean isDetected();

	/**
	 * @see {@link SnowballAttributes#sample(Integer)}
	 */
	public void sample(Integer iteration);

	/**
	 * @see {@link SnowballAttributes#getIterationSampled()}
	 */
	public Integer getIterationSampled();

	/**
	 * @see {@link SnowballAttributes#isSampled()}
	 */
	public boolean isSampled();

	/**
	 * Returns the seed vertex of the component containing this vertex. If there
	 * are multiple seed vertices the closest one is returned.
	 * 
	 * @return the seed vertex of the component containing this vertex.
	 */
	/*
	 * Not sure if this lasts for ever... This is rather pragmatic since we do
	 * not have directed graphs. joh 08/10
	 */
	public SampledVertex getSeed();

	/**
	 * Sets the seed vertex of the component containing the vertex.
	 * 
	 * @param seed
	 *            a vertex.
	 */
	public void setSeed(SampledVertex seed);
}
