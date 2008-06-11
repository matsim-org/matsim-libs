/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeCorrelation.java
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

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * @author illenberger
 *
 */
public class DegreeCorrelation implements GraphStatistic {

	public double run(Graph g) {
		int product = 0;
		int sum = 0;
		int squareSum = 0;
		double edges = 0;
		for (Object e : g.getEdges()) {
			Pair p = ((Edge) e).getEndpoints();
			Vertex v1 = (Vertex) p.getFirst();
			Vertex v2 = (Vertex) p.getSecond();
			
				int d_v1 = v1.degree();
				int d_v2 = v2.degree();


				
				sum += d_v1 + d_v2;
				squareSum += Math.pow(d_v1, 2) + Math.pow(d_v2, 2);
				product += d_v1 * d_v2;
				
				edges ++;

		}
		double M_minus1 = 1 / (double) edges;
		double normSumSquare = Math.pow((M_minus1 * 0.5 * sum), 2);
		double numerator = (M_minus1 * product) - normSumSquare;
		double denumerator = (M_minus1 * 0.5 * squareSum) - normSumSquare;

		return numerator / denumerator;
	}

}
