/* *********************************************************************** *
 * project: org.matsim.*
 * SampledVertexDecorator.java
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
package playground.johannes.sna.snowball;

import java.util.List;

import playground.johannes.sna.graph.Vertex;
import playground.johannes.sna.graph.VertexDecorator;

/**
 * A decorated class that implements SampeldVertex.
 * 
 * @author illenberger
 * 
 */
public class SampledVertexDecorator<V extends Vertex> extends VertexDecorator<V> implements SampledVertex {

	private SnowballAttributes attributes = new SnowballAttributes();

	private SampledVertexDecorator<V> seed;

	/**
	 * Creates a new decorator for <tt>delegate</tt>.
	 * 
	 * @param delegate
	 *            the vertex to be decorated.
	 */
	protected SampledVertexDecorator(V delegate) {
		super(delegate);
	}

	/**
	 * @see {@link SnowballAttributes#detect(Integer)}
	 */
	@Override
	public void detect(Integer iteration) {
		attributes.detect(iteration);
	}

	/**
	 * @see {@link SnowballAttributes#getIterationDeteted()}
	 */
	@Override
	public Integer getIterationDetected() {
		return attributes.getIterationDeteted();
	}

	/**
	 * @see {@link SnowballAttributes#getIterationSampled()}
	 */
	@Override
	public Integer getIterationSampled() {
		return attributes.getIterationSampled();
	}

	/**
	 * @see {@link SnowballAttributes#isDetected()}
	 */
	@Override
	public boolean isDetected() {
		return attributes.isDetected();
	}

	/**
	 * @see {@link SnowballAttributes#isSampled()}
	 */
	@Override
	public boolean isSampled() {
		return attributes.isSampled();
	}

	/**
	 * @see {@link SnowballAttributes#sample(Integer)}
	 */
	@Override
	public void sample(Integer iteration) {
		attributes.sample(iteration);
	}

	/**
	 * @see {@link VertexDecorator#getEdges()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SampledEdgeDecorator<?>> getEdges() {
		return (List<? extends SampledEdgeDecorator<?>>) super.getEdges();
	}

	/**
	 * @see {@link VertexDecorator#getNeighbours()}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SampledVertexDecorator<V>> getNeighbours() {
		return (List<? extends SampledVertexDecorator<V>>) super.getNeighbours();
	}

	/**
	 * @see {@link SampledVertex#getSeed()}
	 */
	@Override
	public SampledVertexDecorator<V> getSeed() {
		return seed;
	}

	/**
	 * Sets the seed vertex.
	 * 
	 * @param seed the seed vertex.
	 */
	@SuppressWarnings("unchecked")
	public void setSeed(SampledVertex seed) {
		this.seed = (SampledVertexDecorator<V>) seed;
	}

}
