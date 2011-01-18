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
package playground.johannes.socialnetworks.graph.spatial.io;

import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.io.KMLIconVertexStyle;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphKMLWriter;
import org.matsim.contrib.sna.graph.spatial.io.SpatialGraphMLReader;
import org.matsim.contrib.sna.graph.spatial.io.VertexDegreeColorizer;

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
		SpatialGraph graph = reader.readGraph("/Users/jillenberger/Work/socialnets/mcmc/output/500000000/graph.graphml");
		
		SpatialGraphKMLWriter writer = new SpatialGraphKMLWriter();
		VertexDegreeColorizer colorizer = new VertexDegreeColorizer(graph);
		colorizer.setLogscale(true);
		KMLIconVertexStyle style = new KMLIconVertexStyle(graph);
		style.setVertexColorizer(colorizer);
		writer.setKmlVertexStyle(style);
		writer.setDrawEdges(false);
		writer.addKMZWriterListener(style);
		writer.write(graph, "/Users/jillenberger/Work/socialnets/mcmc/output/500000000/graph.kmz");
		
		

	}

}
