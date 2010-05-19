/* *********************************************************************** *
 * project: org.matsim.*
 * GraphClipping.java
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

import java.io.IOException;

import org.geotools.feature.Feature;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;

import com.vividsolutions.jts.geom.Geometry;

import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.graph.analysis.GraphFilter;
import playground.johannes.socialnetworks.graph.social.io.SocialGraphMLWriter;
import playground.johannes.socialnetworks.graph.spatial.analysis.GraphClippingFilter;
import playground.johannes.socialnetworks.snowball2.SampledGraphProjection;
import playground.johannes.socialnetworks.snowball2.SampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLReader;
import playground.johannes.socialnetworks.snowball2.io.SampledGraphProjMLWriter;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraphBuilder;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.SocialSparseGraphMLReader;

/**
 * @author illenberger
 *
 */
public class GraphClipping {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> reader =
			new SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(new SocialSparseGraphMLReader());
		
		reader.setGraphProjectionBuilder(new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>());
		
		SampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = reader.readGraph(args[0]);
		
		Feature feature = FeatureSHP.readFeatures("/Users/jillenberger/Work/work/socialnets/data/schweiz/complete/zones/G1L08.shp").iterator().next();
		Geometry geometry = feature.getDefaultGeometry();
		geometry.setSRID(21781);
		GraphFilter<SpatialGraph> filter = new GraphClippingFilter(new SocialSparseGraphBuilder(graph.getDelegate().getCoordinateReferenceSysten()), geometry);
		filter.apply(graph.getDelegate());
		SampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> builder = new SampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>();
		builder.synchronize(graph);

		SampledGraphProjMLWriter writer = new SampledGraphProjMLWriter(new SocialGraphMLWriter());
		writer.write(graph, args[1]);
	}

}
