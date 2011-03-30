/* *********************************************************************** *
 * project: org.matsim.*
 * VertexTXTWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009.graph.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.matsim.contrib.sna.snowball.analysis.SnowballPartitions;

import playground.johannes.socialnetworks.graph.social.SocialVertex;
import playground.johannes.socialnetworks.snowball2.social.SocialSampledGraphProjection;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseEdge;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseGraph;
import playground.johannes.socialnetworks.survey.ivt2009.graph.SocialSparseVertex;

/**
 * @author illenberger
 *
 */
public class VertexTXTWriter {

	public void write(Set<? extends SocialVertex> vertices, String filename) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
		
		writer.write("id\tgender\tdegree");
		writer.newLine();
		
		for(SocialVertex vertex : vertices) {
			String id = vertex.getPerson().getId().toString();
			String gender = vertex.getPerson().getPerson().getSex();
			int k = vertex.getNeighbours().size();
			
			if(gender != null) {
				writer.write(id);
				writer.write("\t");
				writer.write(gender);
				writer.write("\t");
				writer.write(String.valueOf(k));
				writer.newLine();
			}
		}
		
		writer.close();
	}
	
	public static void main(String args[]) throws IOException {
		SocialSampledGraphProjection<SocialSparseGraph,SocialSparseVertex,SocialSparseEdge> graph = GraphReaderFacade.read("/Users/jillenberger/Work/socialnets/data/ivt2009/01-2011/graph/graph.graphml");
		
		Set vertices = SnowballPartitions.createSampledPartition(graph.getVertices());
		
		VertexTXTWriter writer = new VertexTXTWriter();
		writer.write(vertices, "/Users/jillenberger/Work/socialnets/data/ivt2009/01-2011/graph/egos.txt");
	}
}
