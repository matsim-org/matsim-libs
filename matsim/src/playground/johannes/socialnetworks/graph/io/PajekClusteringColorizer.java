/* *********************************************************************** *
 * project: org.matsim.*
 * PajekClusteringColorizer.java
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
package playground.johannes.socialnetworks.graph.io;

import gnu.trove.TObjectDoubleHashMap;

import org.apache.commons.math.stat.StatUtils;

import playground.johannes.socialnetworks.graph.Edge;
import playground.johannes.socialnetworks.graph.Graph;
import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class PajekClusteringColorizer<V extends Vertex, E extends Edge> extends PajekColorizer<V, E> {

	private double c_min;
	
	private double c_max;
	
	private TObjectDoubleHashMap<V> clustering;
	
	public PajekClusteringColorizer(Graph g) {
		super();
		clustering = (TObjectDoubleHashMap<V>) GraphStatistics.localClusteringCoefficients(g);		
		c_min = StatUtils.min(clustering.getValues());
		c_max = StatUtils.max(clustering.getValues());
	}
	
	public String getEdgeColor(E e) {
		return getColor(-1);
	}

	public String getVertexFillColor(V ego) {
		double c = clustering.get(ego);
		double color = (c - c_min) / (c_max - c_min);
		return getColor(color);
	}

}
