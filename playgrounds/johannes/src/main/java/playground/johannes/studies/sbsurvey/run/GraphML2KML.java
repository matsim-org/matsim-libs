/* *********************************************************************** *
 * project: org.matsim.*
 * GraphML2KML.java
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
package playground.johannes.studies.sbsurvey.run;


import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseEdge;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseGraph;
import org.matsim.contrib.socnetgen.sna.graph.social.SocialSparseVertex;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.KMLObjectDetailComposite;
import org.matsim.contrib.socnetgen.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.socnetgen.sna.snowball.io.SampledGraphProjMLReader;
import org.matsim.contrib.socnetgen.sna.snowball.social.SocialSampledGraphProjection;
import org.matsim.contrib.socnetgen.sna.snowball.social.SocialSampledGraphProjectionBuilder;
import playground.johannes.studies.sbsurvey.io.KMLSampledComponents;
import playground.johannes.studies.sbsurvey.io.KMLSnowballDescriptor;
import playground.johannes.studies.sbsurvey.io.KMLVertexId;
import playground.johannes.studies.sbsurvey.io.SocialSparseGraphMLReader;



/**
 * @author illenberger
 *
 */
public class GraphML2KML {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> reader =
			new SampledGraphProjMLReader<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>(new SocialSparseGraphMLReader());
		
		reader.setGraphProjectionBuilder(new SocialSampledGraphProjectionBuilder<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>());
		
		SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge> graph = (SocialSampledGraphProjection<SocialSparseGraph, SocialSparseVertex, SocialSparseEdge>) reader.readGraph(args[0]);

		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		KMLSampledComponents components = new KMLSampledComponents();
		writer.setKmlPartitition(components);
		KMLIconVertexStyle vertexStyle = new KMLIconVertexStyle(graph);
		vertexStyle.setVertexColorizer(components);
		writer.addKMZWriterListener(vertexStyle);
		writer.setKmlVertexStyle(vertexStyle);
		
		KMLObjectDetailComposite detail = new KMLObjectDetailComposite();
		detail.addObjectDetail(new KMLSnowballDescriptor());
		detail.addObjectDetail(new KMLVertexId());
		writer.setKmlVertexDetail(detail);
		
		writer.write(graph, args[1]);
	}

}
