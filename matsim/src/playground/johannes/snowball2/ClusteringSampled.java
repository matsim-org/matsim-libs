/* *********************************************************************** *
 * project: org.matsim.*
 * ClusteringSampled.java
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

import java.util.LinkedList;
import java.util.List;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;

/**
 * @author illenberger
 * 
 */
public class ClusteringSampled extends Clustering {

	@Override
	public double run(Graph g) {
		if (g instanceof SampledGraph) {
			super.run(g);

			List<SampledVertex> remove = new LinkedList<SampledVertex>();
			for (Vertex v : values.keySet()) {
				if (((SampledVertex) v).isAnonymous())
					remove.add((SampledVertex) v);
			}
			for (SampledVertex v : remove)
				values.remove(v);

			double sum = 0;
//			double wsum
			for (Double d : values.values())
				sum += d;

			return sum / (double) values.size();
		} else {
			throw new IllegalArgumentException(
					"Graph must be instance of SampledGraph!");
		}
	}

}
