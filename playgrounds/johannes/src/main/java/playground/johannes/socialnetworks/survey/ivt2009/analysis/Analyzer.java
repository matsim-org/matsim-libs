/* *********************************************************************** *
 * project: org.matsim.*
 * Analyzer.java
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.io.File;
import java.io.IOException;

import org.geotools.feature.Feature;

import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.graph.spatial.analysis.GraphClippingFilter;
import playground.johannes.socialnetworks.snowball2.SampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.SampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLReader;
import playground.johannes.socialnetworks.snowball2.spatial.SpatialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SampledSocialVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SampledSocialGraphMLReader;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author illenberger
 *
 */
public class Analyzer {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SampledGraphProjMLReader<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> reader =
			new SampledGraphProjMLReader<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge>(new SampledSocialGraphMLReader());
		
		reader.setGraphProjectionBuilder(new SpatialSampledGraphProjectionBuilder<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge>());
		
		SampledGraphProjection<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> graph = reader.readGraph(args[0]);
		/*
		 * analyze the complete graph
		 */
		String output = args[1];
		analyze(graph, output);
		/*
		 * analyze the swiss clipping
		 */
		Feature feature = FeatureSHP.readFeatures("/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/zones/G1L08.shp").iterator().next();
		Geometry geometry = feature.getDefaultGeometry();
		geometry.setSRID(21781);
		GraphFilter filter = new GraphClippingFilter(new SampledSocialGraphBuilder(graph.getDelegate().getCoordinateReferenceSysten()), geometry);
		filter.apply(graph.getDelegate());
		SampledGraphProjectionBuilder<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> builder = new SampledGraphProjectionBuilder<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge>();
		builder.synchronize(graph);
//		SampledGraphProjection<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> clippedGraph = (SampledGraphProjection<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge>) filter.apply(graph);
		
		output = output+"/clip/";
		new File(output).mkdirs();
		analyze(graph, output);
		
	}

	private static void analyze(SampledGraphProjection<SampledSocialGraph, SampledSocialVertex, SampledSocialEdge> graph, String output) {
		ObservedAnalyzerTask task = new ObservedAnalyzerTask();
		task.setOutputDirectoy(output);
		
		try {
			GraphAnalyzer.writeStats(GraphAnalyzer.analyze(graph, task), output + "/stats.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
