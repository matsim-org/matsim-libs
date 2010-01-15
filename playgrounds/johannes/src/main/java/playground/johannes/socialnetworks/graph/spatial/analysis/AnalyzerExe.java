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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import java.io.IOException;

import org.geotools.feature.Feature;
import org.matsim.contrib.sna.graph.spatial.SpatialEdge;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;

import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.graph.analysis.GraphAnalyzer;
import playground.johannes.socialnetworks.graph.spatial.SpatialGraphProjectionBuilder;

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
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph("/Users/jillenberger/Work/work/socialnets/data/ivt2009/graph/graph.graphml");
		
		SpatialAnalyzerTask task = new SpatialAnalyzerTask("/Users/jillenberger/Work/work/socialnets/data/ivt2009/analysis/tmp/");
		GraphAnalyzer.writeStats(GraphAnalyzer.analyze(graph, null, task), "/Users/jillenberger/Work/work/socialnets/data/ivt2009/analysis/tmp/stats.txt");

		Feature feature = FeatureSHP.readFeatures("/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/gemeindegrenzen2008.zip Folder/g1g08_shp_080606.zip Folder/G1L08.shp").iterator().next();
		Geometry geometry = feature.getDefaultGeometry();
		
		SpatialGraphProjectionBuilder<SpatialGraph, SpatialVertex, SpatialEdge> builder = new SpatialGraphProjectionBuilder<SpatialGraph, SpatialVertex, SpatialEdge>();
		
		SpatialGraph graphPrj = builder.decorate(graph, geometry);
		
		GraphAnalyzer.analyze(graphPrj, null, new SpatialAnalyzerTask(null));
	}

}
