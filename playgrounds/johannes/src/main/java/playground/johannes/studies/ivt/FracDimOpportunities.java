/* *********************************************************************** *
 * project: org.matsim.*
 * FracDimOpportunities.java
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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import gnu.trove.TDoubleDoubleHashMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.Histogram;
import org.matsim.contrib.common.stats.TXTWriter;
import org.opengis.feature.simple.SimpleFeature;
import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.graph.spatial.SpatialSparseGraph;
import playground.johannes.sna.graph.spatial.SpatialVertex;
import playground.johannes.sna.snowball.analysis.SnowballPartitions;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.GraphReaderFacade;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class FracDimOpportunities {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String targetsFile = "/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.001.xml";
		String chborderFile = "/Users/jillenberger/Work/socialnets/data/schweiz/complete/zones/G1L08.shp";
		String graphFile = "/Users/jillenberger/Work/socialnets/data/ivt2009/11-2011/graph/graph.graphml";
		String outFile = "/Users/jillenberger/Work/phd/doc/tex/ch3/fig/data/fdim.txt";
		
		SpatialSparseGraph targetGraph = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read(targetsFile);
		List<Point> targetPoints = new ArrayList<Point>(targetGraph.getVertices().size());
		for(SpatialVertex v : targetGraph.getVertices()) {	
			targetPoints.add(v.getPoint());
		}

		SimpleFeature feature = FeatureSHP.readFeatures(chborderFile).iterator().next();
		Geometry geometry = (Geometry) feature.getDefaultGeometry();
		geometry.setSRID(21781);
		
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = GraphReaderFacade.read(graphFile);
		graph.getDelegate().transformToCRS(CRSUtils.getCRS(21781));
		Set<? extends SpatialVertex> egos = SnowballPartitions.createSampledPartition(graph.getVertices());
		List<Point> startPoints = new ArrayList<Point>(egos.size());
		for(SpatialVertex v : egos) {
			if(v.getPoint() != null) {
				if(geometry.contains(v.getPoint()))
					startPoints.add(v.getPoint());
			}
		}
		
		DescriptiveStatistics stats = new DescriptiveStatistics();
		
		DistanceCalculator dCalc = new CartesianDistanceCalculator();
		
		int N = (int) (startPoints.size() * targetPoints.size());
		ProgressLogger.init(N, 1, 5);
		
		for(int i = 0; i < startPoints.size(); i++) {
			Point p1 = startPoints.get(i);
		
			for(int j = 0; j < targetPoints.size(); j++) {
				Point p2 = targetPoints.get(j);
				double d = dCalc.distance(p1, p2);
				if(d > 0)
					stats.addValue(d);
				ProgressLogger.step();
			}
		}
		System.out.println("Creating histograms...");
		TDoubleDoubleHashMap hist = Histogram.createHistogram(stats, FixedSampleSizeDiscretizer.create(stats.getValues(), 100, 500), true);
		Histogram.normalize(hist);
		TXTWriter.writeMap(hist, "d", "p", outFile);
	}

}
