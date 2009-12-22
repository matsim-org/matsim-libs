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

import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleDoubleIterator;
import gnu.trove.TIntDoubleHashMap;
import gnu.trove.TIntDoubleIterator;
import gnu.trove.TIntIntHashMap;

import org.matsim.core.utils.geometry.CoordUtils;

import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.io.SpatialGraphMLReader;
import playground.johannes.socialnetworks.statistics.Distribution;

/**
 * @author illenberger
 *
 */
public class TotalCostDistribution {

	private static final double beta = 1;
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		SpatialSparseGraph graph = new SpatialGraphMLReader().readGraph("/Volumes/hertz:ils-raid/socialnets/mcmc/runs/run137/output/2000000000/graph.graphml");
		
		TIntDoubleHashMap accValues = new TIntDoubleHashMap();
		TIntIntHashMap counts = new TIntIntHashMap();
		int count = 0;
		double totalsum = 0;
		for(SpatialVertex v_i : graph.getVertices()) {
			double sum = 0;
			for(SpatialVertex v_j : graph.getVertices()) {
				if(v_i != v_j) {
					double d = CoordUtils.calcDistance(v_i.getCoordinate(), v_j.getCoordinate());
					d = Math.ceil(d/1000.0);
					d = Math.max(1, d);
					d = beta * Math.log(d);
					sum += d;
				}
				
			}
			totalsum += 1/sum;
			accValues.adjustOrPutValue(v_i.getEdges().size(), 1/sum, 1/sum);
			counts.adjustOrPutValue(v_i.getEdges().size(), 1, 1);
			
			count++;
			if(count % 1000 == 0)
				System.out.println(count);
		}
		
		TDoubleDoubleHashMap distr = new TDoubleDoubleHashMap();
		TIntDoubleIterator it = accValues.iterator();
//		double normsum = 0;
		for(int i = 0; i < accValues.size(); i++) {
			it.advance();
			int k = it.key();
			double sum = it.value();
			int cnt = counts.get(k);
			
			distr.put(k, (sum * graph.getVertices().size()/totalsum)/(double)cnt);
		}
		
//		TDoubleDoubleIterator it2 = distr.iterator();
//		for(int i = 0; i < accValues.size(); i++) {
//			it2.advance();
//			it2.setValue(normsum/it2.value());
//		}

		Distribution.writeHistogram(distr, "/Users/fearonni/vsp-work/work/socialnets/mcmc/costDistr.txt");
	}

}
