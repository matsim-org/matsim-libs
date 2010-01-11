/* *********************************************************************** *
 * project: org.matsim.*
 * PopNorm.java
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
package playground.johannes.socialnetworks.survey.ivt2009;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialSparseVertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialSparseGraph;
import org.matsim.contrib.sna.snowball.spatial.io.SampledSpatialGraphMLReader;
import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraphStatistics;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

/**
 * @author illenberger
 *
 */
public class PopNorm {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException {
		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
		SampledSpatialSparseGraph graph = reader.readGraph("/Users/fearonni/vsp-work/work/socialnets/mcmc/graph.graphml");
		
		Population2SpatialGraph pop2graph = new Population2SpatialGraph(CRSUtils.getCRS(21781));
		SpatialSparseGraph g2 = pop2graph.read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/plans/plans.1.xml");
//		SpatialGraph g2 = pop2graph.read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/zrh100km/plans/plans.10.xml");
		
		Set egos = graph.getVertices();//SnowballPartitions.createSampledPartition(graph.getVertices());
		double bounds[] = g2.getBounds();
		int counter = 0;
//		Distribution distr = new Distribution();
//		for(Object ego : egos) {
//			SpatialVertex v = (SpatialVertex)ego;
//			Coord c1 = v.getCoordinate();
//			
//			if(c1.getX() >= bounds[0] && c1.getX() <= bounds[2] && c1.getY() >= bounds[1] && c1.getY() <= bounds[3]) {
//			for(SpatialVertex v2 : g2.getVertices()) {
//				Coord c2 = v2.getCoordinate();
//				
//				distr.add(CoordUtils.calcDistance(c1, c2));
//			}
//			counter++;
//			System.out.println("Processed " +counter+" egos...");
//			} else {
//				System.err.println("Ego not in bounds");
//			}
//		}

//		Distribution distr = SpatialGraphStatistics.normalizedEdgeLengthDistribution(egos, g2, 1000);
//		Distribution.writeHistogram(distr.absoluteDistribution(1000), "/Users/fearonni/vsp-work/work/socialnets/mcmc/distance.norm.txt");
//		Distribution.writeHistogram(distr.absoluteDistributionLog2(1000), "/Users/fearonni/vsp-work/work/socialnets/mcmc/distance.normlog2.txt");
	} 

}
