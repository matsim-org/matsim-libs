/* *********************************************************************** *
 * project: org.matsim.*
 * Degree.java
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
package playground.johannes.snowball2;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import playground.johannes.snowball.Histogram;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class Degree implements VertexStatistic {

	protected Map<Vertex, Integer> values;
	
	public Histogram getHistogram() {
		Histogram histogram = new Histogram(1.0);
		fillHistogram(histogram);
		return histogram;
	}

	public Histogram getHistogram(double min, double max) {
		Histogram histogram = new Histogram(1.0, min, max);
		fillHistogram(histogram);
		return histogram;
	}

	protected void fillHistogram(Histogram histogram) {
		for(Integer i : values.values())
			histogram.add(i);
	}
	
	public double run(Graph g) {
		values = new HashMap<Vertex, Integer>();
		int sum = 0;
		Set<Vertex> vertices = g.getVertices();
		for(Vertex v : vertices) {
			sum += v.degree();
			values.put(v, v.degree());
		}
		return sum/(double)vertices.size();
	}

}
