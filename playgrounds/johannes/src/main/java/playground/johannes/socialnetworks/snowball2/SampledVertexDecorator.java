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
package playground.johannes.socialnetworks.snowball2;

import java.util.List;

import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.VertexDecorator;
import org.matsim.contrib.sna.snowball.SampledVertex;
import org.matsim.contrib.sna.snowball.SnowballAttributes;

/**
 * @author illenberger
 *
 */
public class SampledVertexDecorator<V extends Vertex> extends VertexDecorator<V> implements
		SampledVertex {

	private SnowballAttributes attributes = new SnowballAttributes();
	
	private SampledVertexDecorator<V> seed;
	
	protected SampledVertexDecorator(V delegate) {
		super(delegate);
	}

	@Override
	public void detect(int iteration) {
		attributes.detect(iteration);
	}

	@Override
	public int getIterationDetected() {
		return attributes.getIterationDeteted();
	}

	@Override
	public int getIterationSampled() {
		return attributes.getIterationSampled();
	}

	@Override
	public boolean isDetected() {
		return attributes.isDetected();
	}

	@Override
	public boolean isSampled() {
		return attributes.isSampled();
	}

	@Override
	public void sample(int iteration) {
		attributes.sample(iteration);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SampledEdgeDecorator<?>> getEdges() {
		return (List<? extends SampledEdgeDecorator<?>>) super.getEdges();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SampledVertexDecorator<V>> getNeighbours() {
		return (List<? extends SampledVertexDecorator<V>>) super.getNeighbours();
	}

	@Override
	public SampledVertexDecorator<V> getSeed() {
		return seed;
	}
	
	public void setSeed(SampledVertexDecorator<V> seed) {
		this.seed = seed;
	}

}
