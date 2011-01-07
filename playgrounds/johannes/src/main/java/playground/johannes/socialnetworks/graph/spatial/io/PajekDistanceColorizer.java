/* *********************************************************************** *
 * project: org.matsim.*
 * PajekDistanceColorizer.java
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph.spatial.io;

import gnu.trove.TObjectDoubleHashMap;

import org.matsim.contrib.sna.graph.spatial.SpatialSparseEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.graph.io.PajekColorizer;
import playground.johannes.socialnetworks.graph.spatial.analysis.Distance;

/**
 * @author illenberger
 *
 */
public class PajekDistanceColorizer extends PajekColorizer<SpatialSparseVertex, SpatialSparseEdge> {

	private double d_min;
	
	private double d_max;
	
	private TObjectDoubleHashMap<SpatialVertex> d_mean;
	
	private boolean logScale = false;
	
	public PajekDistanceColorizer(SpatialSparseGraph graph, boolean logScale) {
		super();
		setLogScale(logScale);
		d_mean = new Distance().vertexMean(graph.getVertices());
		
		d_min = Double.MAX_VALUE;
		d_max = Double.MIN_VALUE;
		for(double value : d_mean.getValues()) {
			d_min = Math.min(value, d_min); 
			d_max = Math.max(value, d_max);
		}
	}
	
	public void setLogScale(boolean flag) {
		logScale = flag;
	}
	
	@Override
	public String getEdgeColor(SpatialSparseEdge e) {
		return getColor(-1);
	}

	@Override
	public String getVertexFillColor(SpatialSparseVertex v) {
		double color = -1;
		if(logScale) {
			double min = Math.log(d_min + 1);
			double max = Math.log(d_max + 1);
			color = (Math.log(d_mean.get(v)+ 1) - min) / (max - min);
		} else {
			color = (d_mean.get(v) - d_min)/(d_max - d_min);
		}
		
		return getColor(color);
	}

}
