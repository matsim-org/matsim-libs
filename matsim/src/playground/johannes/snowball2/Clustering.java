/* *********************************************************************** *
 * project: org.matsim.*
 * Clustering.java
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

import java.util.Map;

import playground.johannes.snowball.Histogram;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.statistics.GraphStatistics;

/**
 * @author illenberger
 *
 */
public class Clustering implements VertexStatistic {

	protected Map<Vertex, Double> values;
	
	public Histogram getHistogram() {
		Histogram histogram = new Histogram(100);
		for(Double d : values.values())
			histogram.add(d);
		return histogram;
	}

	public Histogram getHistogram(double min, double max) {
		Histogram histogram = new Histogram(100, min, max);
		for(Double d : values.values())
			histogram.add(d);
		return histogram;
	}

	public double run(Graph g) {
		values = GraphStatistics.clusteringCoefficients(g);
		
		double sum = 0;
		for(Vertex v : values.keySet()) {
			if(v.degree() == 1)
				values.put(v, 0.0);
			
			sum += values.get(v);
		}
		
		return sum/(double)values.size();
	}

}
