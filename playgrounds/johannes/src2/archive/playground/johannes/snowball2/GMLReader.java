/* *********************************************************************** *
 * project: org.matsim.*
 * GMLReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.johannes.snowball2;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.utils.io.IOUtils;

import playground.johannes.socialnets.UserDataKeys;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.io.GraphMLFile;

/**
 * @author illenberger
 *
 */
public class GMLReader {


	private Map<String, Vertex> vertices;
	
//	private Map<String, Edge> edges;
	
	public Graph read(String filename) {
		try {
			BufferedReader reader = IOUtils.getBufferedReader(filename);
			Graph g = new UndirectedSparseGraph();
			vertices = new HashMap<String, Vertex>();
//			edges = new HashMap<String, Edge>();
			
			String line;
			while((line = reader.readLine()) != null) {
				if(line.trim().startsWith("node")) {
					g.addVertex(parseVertex(readBlock(reader)));
				} else if(line.trim().startsWith("edge")) {
					Edge e = parseEdge(readBlock(reader));
					if(e != null)
						g.addEdge(e);
				}
			}
			
			return g;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private String[] readBlock(BufferedReader reader) throws IOException {
		LinkedList<String> lines = new LinkedList<String>();
		String line = reader.readLine();
		line = reader.readLine();
		while(!line.endsWith("]")) {
			line = line.trim();
			lines.add(line);
			line = reader.readLine(); 
		}
		return lines.toArray(new String[lines.size()]);
	}
	
	protected Vertex parseVertex(String[] attrs) {
		Map<String, String> attributes = parseAttributes(attrs);
		Vertex v = newVertex();
		v.setUserDatum(UserDataKeys.X_COORD, Math.random(), UserDataKeys.COPY_ACT);
		v.setUserDatum(UserDataKeys.Y_COORD, Math.random(), UserDataKeys.COPY_ACT);
		String id = attributes.get("id");
		v.setUserDatum(UserDataKeys.ID, id, UserDataKeys.COPY_ACT);
		vertices.put(id, v);
		return v;
	}
	
	protected Edge parseEdge(String[] attrs) {
		Map<String, String> attributes = parseAttributes(attrs);
		Vertex v1 = vertices.get(attributes.get("source"));
		Vertex v2 = vertices.get(attributes.get("target"));
		Edge e = null;
		if(v1 != v2)
			e = newEdge(v1, v2);
		
		return e;
	}
	
	protected Vertex newVertex() {
		return new UndirectedSparseVertex();
	}
	
	protected Edge newEdge(Vertex v1, Vertex v2) {
		return new UndirectedSparseEdge(v1, v2);
	}
	
	private Map<String, String> parseAttributes(String[] attrs) {
		Map<String, String> attributes = new HashMap<String, String>();
		for(String attr : attrs) {
			attr = attr.trim();
			String[] tokens = attr.split(" ");
			attributes.put(tokens[0], tokens[1]);
		}
		return attributes;
	}
	
	public static void main(String args[]) {
		Graph g = new GMLReader().read(args[0]);
		new GraphMLFile().save(g, args[1]);
	}
}
