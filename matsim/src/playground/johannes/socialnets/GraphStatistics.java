/* *********************************************************************** *
 * project: org.matsim.*
 * GraphStatistics.java
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
package playground.johannes.socialnets;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.geotools.util.MapEntry;

import cern.jet.stat.Descriptive;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.statistics.DegreeDistributions;

/**
 * @author illenberger
 *
 */
public class GraphStatistics {

	static public double meanDegree(Graph g) {
		return Descriptive.mean(DegreeDistributions.getDegreeValues(g.getVertices()));
	}
	
	static public double meanDegreeSampled(Graph g) {
		Set<Vertex> vertices = new HashSet<Vertex>();
		for(Object v : g.getVertices()) {
			Boolean bool = (Boolean)((Vertex)v).getUserDatum(UserDataKeys.PARTICIPATE_KEY);
			if(bool != null && bool == true) {
				vertices.add((Vertex) v);
			}
		}
		return Descriptive.mean(DegreeDistributions.getDegreeValues(vertices));
	}
	
	static public double meanClusterCoefficient(Graph g) {
		Map coefficients = edu.uci.ics.jung.statistics.GraphStatistics.clusteringCoefficients(g);
		double sum = 0;
		for(Object d : coefficients.values())
			sum += (Double)d;
		
		return sum/(double)coefficients.size();
	}
	
	static public double meanClusterCoefficientSampled(Graph g) {
		Map<Vertex, Double> coefficients = edu.uci.ics.jung.statistics.GraphStatistics.clusteringCoefficients(g);
		double sum = 0;
		int count = 0;
		for(Entry<Vertex, Double> m : coefficients.entrySet()) {
			Vertex v = m.getKey();
			Boolean b = (Boolean) v.getUserDatum(UserDataKeys.PARTICIPATE_KEY); 
			if(b != null && b.booleanValue() == true) {
				sum += m.getValue();
				count++;
			}
		}
		return sum/(double)count;
	}
}
