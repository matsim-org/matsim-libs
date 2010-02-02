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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialEdge;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialVertex;
import org.matsim.contrib.sna.snowball.spatial.io.SampledSpatialGraphMLReader;

import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;
import playground.johannes.socialnetworks.graph.analysis.DegreeTask;
import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.graph.spatial.analysis.DistanceTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.SpatialAnalyzerTask;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialGraphProjectionBuilder;

import com.vividsolutions.jts.geom.Geometry;

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
		String graphfile = args[0];
		String output = args[1];
		String zonesfile = args[2];
		String boundaryfile = args[3];
		
		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
		SampledSpatialGraph graph = reader.readGraph(graphfile);
		
		ZoneLayer zones = ZoneLayerSHP.read(zonesfile);
		
		SpatialAnalyzerTask task = new SpatialAnalyzerTask(output, zones);
		task.addTask(new WaveSizeTask(output));
		
		Map<String, Object> analyzers = new HashMap<String, Object>();
		analyzers.put(DegreeTask.class.getCanonicalName(), new SampledDegree());
		analyzers.put(DistanceTask.class.getCanonicalName(), new SampledDistance());
		
		Map<String, Double> stats = GraphAnalyzer.analyze(graph, analyzers, task);
		playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer.writeStats(stats, output + "/stats.txt");
		
		Geometry boundary = FeatureSHP.readFeatures(boundaryfile).iterator().next().getDefaultGeometry();
		SampledSpatialGraph proj = new SampledSpatialGraphProjectionBuilder<SampledSpatialGraph, SampledSpatialVertex, SampledSpatialEdge>().decorate(graph, boundary);
		
		output = output + "/clip/";
		new File(output).mkdirs();
		task = new SpatialAnalyzerTask(output, zones);
		task.addTask(new WaveSizeTask(output));
		stats = GraphAnalyzer.analyze(proj, analyzers, task);
		playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer.writeStats(stats, output + "/stats.txt");
	}

}
