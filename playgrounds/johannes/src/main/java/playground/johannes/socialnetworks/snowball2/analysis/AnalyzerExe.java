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

import org.matsim.contrib.sna.graph.analysis.DegreeTask;
import org.matsim.contrib.sna.graph.analysis.GraphSizeTask;
import org.matsim.contrib.sna.graph.analysis.TransitivityTask;

import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.graph.analysis.AnalyzerTaskComposite;
import playground.johannes.socialnetworks.graph.spatial.analysis.DistanceTask;
import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLReader;
import playground.johannes.socialnetworks.snowball2.spatial.SpatialSampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.spatial.SpatialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SampledSocialGraphMLReader;

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
//		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
//		SampledSpatialGraph graph = reader.readGraph("/Users/jillenberger/Work/work/socialnets/data/ivt2009/graph/graph.ch1903.noisolates.graphml");

		
//		SpatialAnalyzerTask task = new SpatialAnalyzerTask("/Users/jillenberger/Work/work/socialnets/data/ivt2009/analysis/tmp/");
//		GraphAnalyzer.writeStats(GraphAnalyzer.analyze(graph, new SpatialGraphPropertyFactory(), task), "/Users/jillenberger/Work/work/socialnets/data/ivt2009/analysis/tmp/stats.txt");

//		Feature feature = FeatureSHP.readFeatures("/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/gemeindegrenzen2008.zip Folder/g1g08_shp_080606.zip Folder/G1L08.shp").iterator().next();
//		Geometry geometry = feature.getDefaultGeometry();
//		
//		SampledSpatialGraphProjectionBuilder<SampledSpatialGraph, SampledSpatialVertex, SampledSpatialEdge> builder = new SampledSpatialGraphProjectionBuilder<SampledSpatialGraph, SampledSpatialVertex, SampledSpatialEdge>();
//		
//		SpatialGraph graphPrj = builder.decorate(graph, geometry);
//		
		SampledGraphProjMLReader<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> reader = new SampledGraphProjMLReader<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge>(new SampledSocialGraphMLReader());
		reader.setGraphProjectionBuilder(new SpatialSampledGraphProjectionBuilder<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge>());
		SpatialSampledGraphProjection<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> graph = (SpatialSampledGraphProjection<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge>) reader.readGraph("/Users/jillenberger/Work/work/socialnets/data/ivt2009/tmp.graphml");
		
		AnalyzerTaskComposite task = new AnalyzerTaskComposite();
		
		task.addTask(new GraphSizeTask());
		
		DegreeTask dTask = new DegreeTask();
		dTask.setModule(new ObservedDegree());
		task.addTask(dTask);
		
		TransitivityTask tTask = new TransitivityTask();
//		tTask.setModule(new SampledT)
		task.addTask(tTask);
		
		DistanceTask distTask = new DistanceTask();
		distTask.setModule(new SampledDistance());
		task.addTask(distTask);
		
//		Map<String, Object> analyzers = new HashMap<String, Object>();
//		analyzers.put(DegreeTask.class.getCanonicalName(), new SampledDegree());
//		analyzers.put(DistanceTask.class.getCanonicalName(), new SampledDistance());
		
 		GraphAnalyzer.analyze(graph, task);
	}

}
