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
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.johannes.socialnetworks.graph.spatial.SpatialEdge;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseEdge;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraphBuilder;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseVertex;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.spatial.ZoneLayer;

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
		SpatialSparseGraph socialnet = reader.readGraph(args[0]);
		
		ZoneLayer layer = ZoneLayer.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/gemeindegrenzen2008.zip Folder/g1g08_shp_080606.zip Folder/G1L08.shp");
		SpatialSparseGraphBuilder builder = new SpatialSparseGraphBuilder();
		List<SpatialVertex> remove = new ArrayList<SpatialVertex>();
		for(SpatialVertex v : socialnet.getVertices()) {
			if(layer.getZone(v.getCoordinate()) == null) {
				List<SpatialEdge> edges = new ArrayList((List<SpatialEdge>) v.getEdges());
				for(SpatialEdge e : edges)
					builder.removeEdge(socialnet, (SpatialSparseEdge) e);
				
				remove.add(v);
//				builder.removeVertex(socialnet, (SpatialSparseVertex) v);
			}
		}
		
		for(SpatialVertex v : remove)
			builder.removeVertex(socialnet, (SpatialSparseVertex) v);
		
		KMLWriter writer = new KMLWriter();
		
		KMLDegreeStyle vertexStyle = new KMLDegreeStyle(writer.getVertexIconLink());
		vertexStyle.setLogscale(true);
		
		KMLVertexDescriptor descriptor = new KMLVertexDescriptor(socialnet);
		
		writer.setVertexStyle(vertexStyle);
		writer.setVertexDescriptor(descriptor);
		writer.setCoordinateTransformation(new CH1903LV03toWGS84());
		writer.setDrawEdges(true);
		writer.write(socialnet, args[1]);
	}
}
