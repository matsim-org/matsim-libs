/* *********************************************************************** *
 * project: org.matsim.*
 * SampledVertex.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.snowball;

import java.util.List;

import org.matsim.contrib.sna.graph.SparseVertex;

import playground.johannes.socialnetworks.graph.VertexDecorator;

/**
 * @author illenberger
 *
 */
public class SampledVertex extends SparseVertex {

	private int iterationSampled;
	
	private int iterationDetected;
	
	private boolean isNonResponding;
	
	private boolean isRequested;
	
	private VertexDecorator<SampledVertex> projection;
	
	private double sampleProbability;
	
	private double normalizedWeight;
	
	protected SampledVertex() {
		reset();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SampledEdge> getEdges() {
		return (List<? extends SampledEdge>) super.getEdges();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<? extends SampledVertex> getNeighbours() {
		return (List<? extends SampledVertex>) super.getNeighbours();
	}

	public void detect(int iteration) {
		iterationDetected = iteration;
	}
	
	public boolean isDetected() {
		if(iterationDetected > -1)
			return true;
		else
			return false;
	}
	
	public int getIterationDetected() {
		return iterationDetected;
	}
	
	public void sample(int iteration) {
		iterationSampled = iteration;
	}
	
	public boolean isSampled() {
		if(iterationSampled > -1)
			return true;
		else
			return false;
	}
	
	public int getIterationSampled() {
		return iterationSampled;
	}
	
	public boolean isAnonymous() {
		if(isDetected() && !isSampled())
			return true;
		else
			return false;
	}
	
	void setIsNonResponding(boolean flag) {
		isNonResponding = flag;
	}
	
	public boolean isNonResponding() {
		return isNonResponding;
	}
	
	void setIsRequested(boolean flag) {
		this.isRequested = flag;
	}
	
	public boolean isRequested() {
		return isRequested;
	}
	
	void setProjection(VertexDecorator<SampledVertex> projection) {
		this.projection = projection;
	}
	
	public VertexDecorator<SampledVertex> getProjection() {
		return projection;
	}
	
	void setNormalizedWeight(double w) {
		normalizedWeight = w;
	}
	
	public double getNormalizedWeight() {
		return normalizedWeight;
	}
	
	void setSampleProbability(double p) {
		sampleProbability = p;
	}
	
	public double getSampleProbability() {
		return sampleProbability;
	}
	
	void reset() {
		iterationSampled = Integer.MIN_VALUE;
		iterationDetected = Integer.MIN_VALUE;
		isNonResponding = false;
		projection = null;
		normalizedWeight = 1;
		sampleProbability = 1;
	}
}
