/* *********************************************************************** *
 * project: org.matsim.*
 * DegreeCorrelationSampled.java
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
import edu.uci.ics.jung.utils.Pair;

/**
 * @author illenberger
 * 
 */
public class DegreeCorrelationSampled implements GraphStatistic {

	public double run(Graph g) {
		if (g instanceof SampledGraph) {
			int product = 0;
			int sum = 0;
			int squareSum = 0;
			double edges = 0;
			for (Object e : g.getEdges()) {
				Pair p = ((Edge) e).getEndpoints();
				SampledVertex v1 = (SampledVertex) p.getFirst();
				SampledVertex v2 = (SampledVertex) p.getSecond();

				if (!v1.isAnonymous() && !v2.isAnonymous()) {
					int d_v1 = v1.degree();
					int d_v2 = v2.degree();

					sum += d_v1 + d_v2;
					squareSum += Math.pow(d_v1, 2) + Math.pow(d_v2, 2);
					product += d_v1 * d_v2;

					edges += 1;
				}
			}
			double M_minus1 = 1 / (double) edges;
			double normSumSquare = Math.pow((M_minus1 * 0.5 * sum), 2);
			double numerator = (M_minus1 * product) - normSumSquare;
			double denumerator = (M_minus1 * 0.5 * squareSum) - normSumSquare;

			return numerator / denumerator;
		} else {
			throw new IllegalArgumentException(
					"Graph must be instance of SampledGraph!");
		}
	}
}
