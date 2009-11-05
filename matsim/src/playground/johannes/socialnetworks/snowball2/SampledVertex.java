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
package playground.johannes.socialnetworks.snowball2;

import java.util.List;

import org.matsim.contrib.sna.graph.Vertex;


/**
 * @author illenberger
 *
 */
public interface SampledVertex extends Vertex {

	public List<? extends SampledEdge> getEdges();
	
	public List<? extends Vertex> getNeighbours();
	
	public void detect(int iteration);
	
	public int getIterationDetected();
	
	public boolean isDetected();
	
	public void sample(int iteration);
	
	public int getIterationSampled();
	
	public boolean isSampled();
	
}
