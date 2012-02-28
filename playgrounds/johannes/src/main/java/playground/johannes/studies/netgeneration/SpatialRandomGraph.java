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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.johannes.sna.gis.CRSUtils;
import playground.johannes.sna.graph.analysis.DegreeTask;
import playground.johannes.sna.graph.analysis.GraphAnalyzer;
import playground.johannes.sna.graph.spatial.SpatialGraph;
import playground.johannes.sna.graph.spatial.SpatialSparseGraph;
import playground.johannes.sna.graph.spatial.SpatialSparseGraphBuilder;
import playground.johannes.sna.graph.spatial.SpatialSparseVertex;
import playground.johannes.sna.graph.spatial.SpatialVertex;
import playground.johannes.sna.graph.spatial.io.SpatialGraphMLReader;
import playground.johannes.sna.graph.spatial.io.SpatialGraphMLWriter;
import playground.johannes.sna.util.ProgressLogger;
import playground.johannes.socialnetworks.gis.CartesianDistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.GravityCostFunction;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.spatial.analysis.AcceptanceProbabilityTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.AccessibilityTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.DegreeAccessibilityTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.EdgeLengthTask;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

/**
 * @author illenberger
 *
 */
public class SpatialRandomGraph {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Random random = new Random(1);
		
		CoordinateReferenceSystem crs = CRSUtils.getCRS(21781);
		SpatialSparseGraph graph = new Population2SpatialGraph(crs).read("/Users/jillenberger/Work/socialnets/data/schweiz/complete/plans/plans.0.001.xml");
		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder(graph.getCoordinateReferenceSysten());
		
		double k_mean = 14.8;
		
		DistanceCalculator calc = new CartesianDistanceCalculator();
		
		List<SpatialSparseVertex> vertices = new ArrayList<SpatialSparseVertex>(graph.getVertices());
		
		ProgressLogger.init(vertices.size() * vertices.size() / 2, 1, 5);
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
		
		double m = 0.5 * k_mean * vertices.size();
		double c = m / sum;
		
		ProgressLogger.init((int) m, 1, 5);
		int edgecount = 0;
		while(edgecount < m) {
			SpatialSparseVertex v_i = vertices.get(random.nextInt(vertices.size()));
			SpatialSparseVertex v_j = vertices.get(random.nextInt(vertices.size()));
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
		
		SpatialGraphMLWriter writer = new SpatialGraphMLWriter();
		writer.write(graph, "/Users/jillenberger/Work/socialnets/mcmc/output/graph.graphml");
		
//		SpatialGraphMLReader reader = new SpatialGraphMLReader();
//		SpatialGraph graph = reader.readGraph("/Users/jillenberger/Work/socialnets/mcmc/output/plain/graph.graphml");
		AnalyzerTaskComposite composite = new AnalyzerTaskComposite();
		composite.addTask(new DegreeTask());
		composite.addTask(new EdgeLengthTask());
//		composite.addTask(new AccessibilityTask(new GravityCostFunction(1.4, 0, CartesianDistanceCalculator.getInstance())));
//		composite.addTask(new AcceptanceProbabilityTask());
		composite.addTask(new DegreeAccessibilityTask(new GravityCostFunction(1.4, 0, CartesianDistanceCalculator.getInstance())));
		
		GraphAnalyzer.analyze(graph, composite, "/Users/jillenberger/Work/socialnets/mcmc/output/");
	}

}
