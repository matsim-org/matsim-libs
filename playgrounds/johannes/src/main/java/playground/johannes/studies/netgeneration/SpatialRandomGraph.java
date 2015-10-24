/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialRandomGraph.java
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
package playground.johannes.studies.netgeneration;

import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.gis.CRSUtils;
import org.matsim.contrib.common.gis.CartesianDistanceCalculator;
import org.matsim.contrib.common.gis.DistanceCalculator;
import org.matsim.contrib.common.util.ProgressLogger;
import org.matsim.contrib.socnetgen.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.socnetgen.sna.graph.analysis.GraphAnalyzer;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.social.io.Population2SocialGraph;
import playground.johannes.socialnetworks.graph.social.io.SocialGraphMLWriter;
import playground.johannes.socialnetworks.graph.spatial.analysis.*;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

import java.io.IOException;
import java.util.*;

/**
 * @author illenberger
 *
 */
public class SpatialRandomGraph {

	private static final Logger logger = Logger.getLogger(SpatialRandomGraph.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String graphfile = args[0];
		String outfile = args[1];
		Random random = new Random(1);
		
		CoordinateReferenceSystem crs = CRSUtils.getCRS(21781);
//		SpatialSparseGraph graph = new Population2SpatialGraph(crs).read(graphfile);
//		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder(graph.getCoordinateReferenceSysten());
		SocialSparseGraph graph = new Population2SocialGraph().read(graphfile, crs);
		SocialSparseGraphBuilder builder = new SocialSparseGraphBuilder(graph.getCoordinateReferenceSysten());
		
		double k_mean = 14.8;
		
		DistanceCalculator calc = new CartesianDistanceCalculator();
		
//		List<SpatialSparseVertex> vertices = new ArrayList<SpatialSparseVertex>(graph.getVertices());
		List<SocialSparseVertex> vertices = new ArrayList<SocialSparseVertex>(graph.getVertices());
		
		double m = 0.5 * k_mean * vertices.size();
		
		logger.info("Calculating sum...");
		ProgressLogger.init((long) (0.5 * vertices.size() * (vertices.size() - 1)), 1, 5);
		double sum = 0;
		for(int i = 0; i < vertices.size(); i++) {
			for(int j = i+1; j < vertices.size(); j++) {
				SpatialVertex v_i = vertices.get(i);
				SpatialVertex v_j = vertices.get(j);
				
				double d = calc.distance(v_i.getPoint(), v_j.getPoint());
				if(d > 0)
					sum +=  Math.pow(d, -1.4);
				
				ProgressLogger.step();
			}
		}
		ProgressLogger.termiante();
		logger.info("Done.");
		
		logger.info(String.format("Insering %1$s edges...", m));
		double c = m / sum;
		
		ProgressLogger.init((long) m, 1, 5);
		int edgecount = 0;
		while(edgecount < m) {
//			SpatialSparseVertex v_i = vertices.get(random.nextInt(vertices.size()));
//			SpatialSparseVertex v_j = vertices.get(random.nextInt(vertices.size()));
			SocialSparseVertex v_i = vertices.get(random.nextInt(vertices.size()));
			SocialSparseVertex v_j = vertices.get(random.nextInt(vertices.size()));
			if(v_i != v_j) {
				double d = calc.distance(v_i.getPoint(), v_j.getPoint());
				double p = c * Math.pow(d, -1.4);
				if(p > random.nextDouble()) {
					if(builder.addEdge(graph, v_i, v_j) != null) {
						edgecount++;
						ProgressLogger.step();
					}
				}
			}
		}
		logger.info("Done.");
//		ProgressLogger.init(vertices.size() * vertices.size() / 2, 1, 5);
//		
//		for(int i = 0; i < vertices.size(); i++) {
//			for(int j = i+1; j < vertices.size(); j++) {
//				SpatialSparseVertex v_i = vertices.get(i);
//				SpatialSparseVertex v_j = vertices.get(j);
//				
//				double d = calc.distance(v_i.getPoint(), v_j.getPoint());
//				double p = c * Math.pow(d, -1.4);
//				if(p > random.nextDouble()) {
//					builder.addEdge(graph, v_i, v_j);
//				}
//				ProgressLogger.step();
//			}
//		}
//		ProgressLogger.termiante();
		
		SocialGraphMLWriter writer = new SocialGraphMLWriter();
		writer.write(graph, outfile + "/graph.graphml");
		
//		SpatialGraphMLReader reader = new SpatialGraphMLReader();
//		SpatialGraph graph = reader.readGraph("/Users/jillenberger/Work/socialnets/mcmc/output/plain/graph.graphml");
		AnalyzerTaskComposite composite = new AnalyzerTaskComposite();
		composite.addTask(new DegreeTask());
		composite.addTask(new EdgeLengthTask());
		
		Accessibility access = new Accessibility(new GravityCostFunction(1.4, 0, new CartesianDistanceCalculator()));
		CachedAccessibility cachedAccess = new CachedAccessibility(access);
		
		composite.addTask(new DegreeAccessibilityTask(cachedAccess));
		composite.addTask(new EdgeLengthAccessibilityTask(cachedAccess));
		composite.addTask(new TransitivityAccessibilityTask(cachedAccess));
		
		AcceptancePropaCategoryTask t = new AcceptancePropaCategoryTask(cachedAccess);
		Set<Point> points = new HashSet<Point>();
		for(SpatialVertex v : graph.getVertices())
			points.add(v.getPoint());
		t.setDestinations(points);
		composite.addTask(t);
		
		GraphAnalyzer.analyze(graph, composite, outfile);
	}

}
