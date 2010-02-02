/* *********************************************************************** *
 * project: org.matsim.*
 * AnalyzerExe.java
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
package playground.johannes.socialnetworks.snowball2.analysis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.feature.Feature;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialEdge;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialVertex;
import org.matsim.contrib.sna.snowball.spatial.io.SampledSpatialGraphMLReader;

import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.graph.analysis.DegreeTask;
import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.analysis.StandardAnalyzerTask;
import playground.johannes.socialnetworks.graph.spatial.analysis.DistanceTask;
import playground.johannes.socialnetworks.snowball2.spatial.SampledSpatialGraphProjectionBuilder;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.SampledDegree;
import playground.johannes.socialnetworks.snowball2.spatial.analysis.SampledDistance;

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
		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
		SampledSpatialGraph graph = reader.readGraph("/Users/jillenberger/Work/work/socialnets/data/ivt2009/graph/graph.ch1903.noisolates.graphml");

		
//		SpatialAnalyzerTask task = new SpatialAnalyzerTask("/Users/jillenberger/Work/work/socialnets/data/ivt2009/analysis/tmp/");
//		GraphAnalyzer.writeStats(GraphAnalyzer.analyze(graph, new SpatialGraphPropertyFactory(), task), "/Users/jillenberger/Work/work/socialnets/data/ivt2009/analysis/tmp/stats.txt");

		Feature feature = FeatureSHP.readFeatures("/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/gemeindegrenzen2008.zip Folder/g1g08_shp_080606.zip Folder/G1L08.shp").iterator().next();
		Geometry geometry = feature.getDefaultGeometry();
		
		SampledSpatialGraphProjectionBuilder<SampledSpatialGraph, SampledSpatialVertex, SampledSpatialEdge> builder = new SampledSpatialGraphProjectionBuilder<SampledSpatialGraph, SampledSpatialVertex, SampledSpatialEdge>();
		
		SpatialGraph graphPrj = builder.decorate(graph, geometry);
		
		GraphAnalyzerTaskComposite task = new StandardAnalyzerTask(null);
		task.addTask(new DistanceTask(null));
		
		Map<String, Object> analyzers = new HashMap<String, Object>();
		analyzers.put(DegreeTask.class.getCanonicalName(), new SampledDegree());
		analyzers.put(DistanceTask.class.getCanonicalName(), new SampledDistance());
		
 		GraphAnalyzer.analyze(graphPrj, analyzers, task);
	}

}
