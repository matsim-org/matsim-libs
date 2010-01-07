/* *********************************************************************** *
 * project: org.matsim.*
 * KMLWriter.java
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
package playground.johannes.socialnetworks.spatial;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

/**
 * @author illenberger
 *
 */
public class CostAnalysis {

	private static final double beta = 1;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpatialSparseGraph graph = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.005.xml");
		
		List<SpatialVertex> vertices = new ArrayList<SpatialVertex>(graph.getVertices());
		int N = vertices.size();
		
		double costsum = 0;
		for(int i = 0; i < N; i++) {
			for(int j = (i+1); j < N; j++) {
				costsum += distance2Cost(descretize(calcDistance(vertices.get(i), vertices.get(j))));
			}
		}

		System.out.println(costsum);
	}
	
	private static double calcDistance(SpatialVertex i, SpatialVertex j) {
		Coord c_i = i.getCoordinate();
		Coord c_j = j.getCoordinate();
		
		return CoordUtils.calcDistance(c_i, c_j);
	}

	
	private static double distance2Cost(double d) {
		return beta * Math.log(d);

	}
	
	private static double descretize(double d) {
		d = Math.ceil(d/1000.0);
		d = Math.max(1, d);
		return d;
	}

}
