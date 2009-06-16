/* *********************************************************************** *
 * project: org.matsim.*
 * GraphML2KML.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.socialnetworks.graph.spatial.io;

import java.io.IOException;

import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;

/**
 * @author illenberger
 *
 */
public class GraphML2KMLDegree {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SpatialGraphMLReader reader = new SpatialGraphMLReader();
		SpatialGraph socialnet = reader.readGraph(args[0]);
		
		KMLWriter writer = new KMLWriter();
		
		KMLDegreeStyle vertexStyle = new KMLDegreeStyle(writer.getVertexIconLink());
		vertexStyle.setLogscale(true);
		
		KMLVertexDescriptor descriptor = new KMLVertexDescriptor(socialnet);
		
		writer.setVertexStyle(vertexStyle);
		writer.setVertexDescriptor(descriptor);
		writer.setCoordinateTransformation(new CH1903LV03toWGS84());
		writer.setDrawEdges(false);
		writer.write(socialnet, args[1]);
	}
}
