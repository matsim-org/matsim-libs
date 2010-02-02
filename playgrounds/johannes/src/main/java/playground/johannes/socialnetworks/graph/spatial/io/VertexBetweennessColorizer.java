/* *********************************************************************** *
 * project: org.matsim.*
 * VertexBetweennessColorizer.java
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
package playground.johannes.socialnetworks.graph.spatial.io;

import gnu.trove.TObjectDoubleIterator;

import java.awt.Color;

import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.io.Colorizable;

import playground.johannes.socialnetworks.graph.GraphStatistics;
import playground.johannes.socialnetworks.graph.GraphStatistics.GraphDistance;

/**
 * @author illenberger
 *
 */
public class VertexBetweennessColorizer implements Colorizable<Vertex> {

	private Vertex vertex = null;
	
	public VertexBetweennessColorizer(Graph graph) {
		GraphDistance dist = GraphStatistics.centrality(graph);
		
		double max = 0;
		
		TObjectDoubleIterator<Vertex> it = dist.getVertexBetweennees().iterator();
		for(int i = 0; i < dist.getVertexBetweennees().size(); i++) {
			it.advance();
			if(it.value() > max) {
				max = it.value();
				vertex = it.key();
			}
		}
		System.err.println("Max betweenness is " + max);
	}
	
	@Override
	public Color getColor(Vertex object) {
		if(object == vertex)
			return Color.RED;
		else
			return Color.WHITE;
	}

}
