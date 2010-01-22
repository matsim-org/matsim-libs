/* *********************************************************************** *
 * project: org.matsim.*
 * SNGraphMLWriter.java
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
package playground.johannes.socialnetworks.graph.social.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.io.GraphMLWriter;
import org.matsim.core.utils.collections.Tuple;

import playground.johannes.socialnetworks.graph.social.Ego;
import playground.johannes.socialnetworks.graph.social.SocialNetwork;
import playground.johannes.socialnetworks.graph.social.SocialTie;

/**
 * @author illenberger
 *
 */
public class SNGraphMLWriter extends GraphMLWriter {

	private static final String WSPACE = " ";

	@Override
	public void write(Graph graph, String filename) throws IOException {
		if(graph instanceof SocialNetwork)
			super.write(graph, filename);
		else
			throw new ClassCastException("Graph must be of type SocialNetwork.");
	}

	@Override
	protected List<Tuple<String, String>> getEdgeAttributes(Edge e) {
		List<Tuple<String, String>> attrs = super.getEdgeAttributes(e);
		
		SocialTie tie = (SocialTie)e;
		attrs.add(new Tuple<String, String>(SNGraphML.CREATED_TAG, String.valueOf(tie.getCreated())));
		
		int cnt = tie.getUsage().size();
		StringBuffer buffer = new StringBuffer(2 * cnt - 1);
		for(int i = 0; i < cnt; i++) {
			buffer.append(String.valueOf(tie.getUsage().get(i)));
			buffer.append(WSPACE);
		}
		buffer.deleteCharAt(buffer.length() - 1);
		attrs.add(new Tuple<String, String>(SNGraphML.USAGE_TAG, buffer.toString()));
		
		return attrs;
	}

	@Override
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attrs = super.getVertexAttributes(v);
		
		Ego e = (Ego)v;
		attrs.add(new Tuple<String, String>(SNGraphML.PERSON_ID_TAG, e.getPerson().getId().toString()));
		
		return attrs;
	}

	/*
	 * FIXME: Think about that! 
	 */
	public static void writeAnonymousVertices(Set<?> vertices, String filename) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
			for(Object e : vertices) {
				writer.write(((Ego) e).getPerson().getId().toString());
				writer.newLine();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
