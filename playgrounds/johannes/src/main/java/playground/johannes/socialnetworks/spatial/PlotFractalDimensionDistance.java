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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class PlotFractalDimensionDistance {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		SpatialSparseGraph graph = new Population2SpatialGraph(21781).read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.001.xml");
		
		Distribution dist = new Distribution();
		Distribution cost = new Distribution();
		int n = graph.getVertices().size();
		int cnt = 0;
		
		ArrayList<SpatialVertex> vertices = new ArrayList<SpatialVertex>(graph.getVertices());
		for(int i = 0; i < vertices.size(); i++) {
			for(int j = (i+1); j < vertices.size(); j++) {
				double d = CoordUtils.calcDistance(vertices.get(i).getCoordinate(), vertices.get(j).getCoordinate());
				double c = distance2Cost(d);
				
				dist.add(d);
				cost.add(c);
				
			}
			cnt++;
			if(cnt % 1000 == 0)
				System.out.println(String.format("Processed %1$s of %2$s vertices.", cnt, n));
		}

		Distribution.writeHistogram(dist.absoluteDistribution(1000.0), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/fracDist.txt");
		Distribution.writeHistogram(cost.absoluteDistribution(), "/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/fracCost.txt");
	}
	
	
	private static double distance2Cost(double d) {
		d= Math.ceil(d/1000.0);
		d=Math.max(1, d);
		return 15 * Math.log(d/2.0 + 1);
	}

}
