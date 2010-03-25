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
package playground.johannes.socialnetworks.survey.ivt2009;

import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.KMLObjectDetailComposite;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;

import playground.johannes.socialnetworks.snowball2.spatial.io.KMLSampledComponents;
import playground.johannes.socialnetworks.snowball2.spatial.io.KMLSnowballDescriptor;
import playground.johannes.socialnetworks.survey.ivt2009.graph.io.KMLVertexId;



/**
 * @author illenberger
 *
 */
public class GraphML2KML {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph graph = reader.readGraph(args[0]);

		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		KMLSampledComponents components = new KMLSampledComponents();
		writer.setKmlPartitition(components);
		KMLIconVertexStyle vertexStyle = new KMLIconVertexStyle(graph);
		vertexStyle.setVertexColorizer(components);
		writer.addKMZWriterListener(vertexStyle);
		writer.setKmlVertexStyle(vertexStyle);
		
		KMLObjectDetailComposite<SpatialVertex> detail = new KMLObjectDetailComposite<SpatialVertex>();
		detail.addObjectDetail(new KMLSnowballDescriptor());
		detail.addObjectDetail(new KMLVertexId());
		writer.setKmlVertexDetail(detail);
		
		writer.write(graph, args[1]);
	}

}
