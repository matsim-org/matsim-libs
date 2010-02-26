/* *********************************************************************** *
 * project: org.matsim.*
 * GraphAnalyzer.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.snowball2.spatial.analysis;

import java.io.IOException;

/**
 * @author illenberger
 *
 */
public class AnalyzerExe {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		String graphfile = args[0];
//		String output = args[1];
//		String zonesfile = args[2];
//		String boundaryfile = args[3];
//		
//		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
//		SampledSpatialGraph graph = reader.readGraph(graphfile);
//		
//		ZoneLayer zones = ZoneLayerSHP.read(zonesfile);
//		
//		SpatialAnalyzerTask task = new SpatialAnalyzerTask(zones);
//		task.setOutputDirectoy(output);
////		task.addTask(new WaveSizeTask(output));
//		
//		Map<String, Object> analyzers = new HashMap<String, Object>();
//		analyzers.put(DegreeTask.class.getCanonicalName(), new SampledDegree());
//		analyzers.put(DistanceTask.class.getCanonicalName(), new SampledDistance());
//		
//		Map<String, Double> stats = GraphAnalyzer.analyze(graph, task);
//		playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer.writeStats(stats, output + "/stats.txt");
//		
//		Geometry boundary = FeatureSHP.readFeatures(boundaryfile).iterator().next().getDefaultGeometry();
//		SampledSpatialGraph proj = new SpatialSampledGraphProjectionBuilder<SampledSpatialGraph, SampledSpatialVertex, SampledSpatialEdge>().decorate(graph, boundary);
//		
//		output = output + "/clip/";
//		new File(output).mkdirs();
//		task = new SpatialAnalyzerTask(zones);
////		task.addTask(new WaveSizeTask(output));
//		task.setOutputDirectoy(output);
////		stats = GraphAnalyzer.analyze(proj, analyzers, task);
//		playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer.writeStats(stats, output + "/stats.txt");
	}

}
