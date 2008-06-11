/* *********************************************************************** *
 * project: org.matsim.*
 * GraphWriterTable.java
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
package playground.johannes.snowball;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.utils.io.IOUtils;

import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

/**
 * @author illenberger
 *
 */
public class GraphWriterTable {

	private Graph g;
	
	private ArrayList<Vertex> vertices;
	
	private ArrayList<Edge> edges;
	
	public GraphWriterTable(Graph g) {
		this.g = g;
		index();
	}
	
	@SuppressWarnings("unchecked")
	private void index() {
		vertices = new ArrayList<Vertex>(g.getVertices());
		edges = new ArrayList<Edge>(g.getEdges());
	}
	
	public void write(String vertexFile, String edgeFile) throws FileNotFoundException, IOException {
		BufferedWriter vertexWriter = IOUtils.getBufferedWriter(vertexFile);
		
		vertexWriter.write("ID");
		vertexWriter.newLine();
		
		Map<Vertex, Integer> indicies = new HashMap<Vertex, Integer>();
		for(int i = 0; i < vertices.size(); i++) {
			vertexWriter.write(String.valueOf(i+1));
			vertexWriter.newLine();
			indicies.put(vertices.get(i), i+1);
		}
		vertexWriter.close();
		
		BufferedWriter edgeWriter = IOUtils.getBufferedWriter(edgeFile);
		
		edgeWriter.write("ID\tFROM\tTO");
		edgeWriter.newLine();
		
		for(int i = 0; i < edges.size(); i++) {
			edgeWriter.write(String.valueOf(i+1));
			edgeWriter.write("\t");
			
			Pair p = edges.get(i).getEndpoints();
			edgeWriter.write(String.valueOf(indicies.get(p.getFirst())));
			edgeWriter.write("\t");
			edgeWriter.write(String.valueOf(indicies.get(p.getSecond())));
			edgeWriter.newLine();
		}
		edgeWriter.close();
	}
	
	public Vertex getVertex(int index) {
		return vertices.get(index);
	}
	
	public Edge getEdge(int index) {
		return edges.get(index);	
	}
}
