/* *********************************************************************** *
 * project: org.matsim.*
 * TransitivityTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.ivt;

import com.vividsolutions.jts.geom.Point;
import gnu.trove.TDoubleDoubleHashMap;
import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TDoubleObjectIterator;
import gnu.trove.TObjectDoubleHashMap;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.stats.StatsWriter;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.socnetgen.sna.gis.GravityCostFunction;
import org.matsim.contrib.socnetgen.sna.graph.Vertex;
import org.matsim.contrib.socnetgen.sna.graph.analysis.AttributePartition;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.analysis.Accessibility;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.Population2SpatialGraph;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class TransitivityTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Discretizer discretizer = new LinearDiscretizer(1000.0);
		
		SpatialGraph graph = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.003.xml");
		Set<Point> points = new LinkedHashSet<Point>();
		
		for(SpatialVertex v : graph.getVertices()) {
			points.add(v.getPoint());
		}
		
		Accessibility accessibility = new Accessibility(new GravityCostFunction(1.4, 0, CartesianDistanceCalculator.getInstance()));
		TObjectDoubleHashMap<Vertex> accessValues = accessibility.values(graph.getVertices());
		
		AttributePartition partitioner = new AttributePartition(FixedSampleSizeDiscretizer.create(accessValues.getValues(), 1, 2));
		TDoubleObjectHashMap<Set<Vertex>> partitions = partitioner.partition(accessValues);
		TDoubleObjectIterator<Set<Vertex>> it = partitions.iterator();
		for(int i = 0; i < partitions.size(); i++) {
			it.advance();
			Set<Vertex> partition = it.value();
			
			TDoubleDoubleHashMap M_d = new TDoubleDoubleHashMap();
			ProgressLogger.init(partition.size(), 1, 5);
			for(Vertex v : partition) {
				
				Point p1 = ((SpatialVertex) v).getPoint();
				double a = accessValues.get(v);
				for(SpatialVertex w : graph.getVertices()) {
					Point p2 = w.getPoint();
					double d = CartesianDistanceCalculator.getInstance().distance(p1, p2);
					d = discretizer.discretize(d); 
					M_d.adjustOrPutValue(d, 1/a/d, 1/a/d);
				}
				
				ProgressLogger.step();
			}
			ProgressLogger.termiante();
			StatsWriter.writeHistogram(M_d, "d", "m/A", String.format("%1$s/M_A.%2$.4f.txt", "/Users/jillenberger/Work/socialnets/mcmc/output", it.key()));
		}
	}

}
