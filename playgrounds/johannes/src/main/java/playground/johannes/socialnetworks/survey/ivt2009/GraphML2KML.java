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

import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialGraph;
import org.matsim.contrib.sna.snowball.spatial.io.SampledSpatialGraphMLReader;

import playground.johannes.socialnetworks.snowball2.spatial.io.KMLSampledComponents;
import playground.johannes.socialnetworks.snowball2.spatial.io.KMLSnowballDescriptor;



/**
 * @author illenberger
 *
 */
public class GraphML2KML {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SampledSpatialGraphMLReader reader = new SampledSpatialGraphMLReader();
		SampledSpatialGraph graph = reader.readGraph(args[0]);

		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		KMLSampledComponents components = new KMLSampledComponents();
		writer.setKmlPartitition(components);
		KMLIconVertexStyle vertexStyle = new KMLIconVertexStyle(graph);
		vertexStyle.setVertexColorizer(components);
		writer.addKMZWriterListener(vertexStyle);
		writer.setKmlVertexStyle(vertexStyle);
		writer.setKmlVertexDetail(new KMLSnowballDescriptor());
		
		writer.write(graph, args[1]);
	}

}
