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

import java.util.Map;

import cern.jet.stat.Descriptive;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.statistics.DegreeDistributions;

/**
 * @author illenberger
 *
 */
public class GraphStatistics {

	
	static public double meanDegree(Graph g) {
		return Descriptive.mean(DegreeDistributions.getDegreeValues(g.getVertices()));
	}
	
	static public double meanClusterCoefficient(Graph g) {
		Map coefficients = edu.uci.ics.jung.statistics.GraphStatistics.clusteringCoefficients(g);
		double sum = 0;
		for(Object d : coefficients.values())
			sum += (Double)d;
		
		return sum/(double)coefficients.size();
	}
}
