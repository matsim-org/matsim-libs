/* *********************************************************************** *
 * project: org.matsim.*
 * GraphMLWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.johannes.graph.io;

import gnu.trove.TObjectIntHashMap;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.matsim.utils.collections.Tuple;
import org.matsim.writer.MatsimXmlWriter;

import playground.johannes.graph.Edge;
import playground.johannes.graph.Graph;
import playground.johannes.graph.Vertex;

/**
 * @author illenberger
 *
 */
public class GraphMLWriter extends MatsimXmlWriter {

	private Graph graph;
	
	private TObjectIntHashMap<Vertex> vertexTmpIds;
	
	private int vertexIdx;
	
	public void write(Graph graph, String filename) throws IOException {
		this.graph = graph;
		openFile(filename);
		setPrettyPrint(true);
		
		writeXmlHead();
		
		int indent = 0;
		setIndentationLevel(indent++);
		List<Tuple<String, String>> attrs = new LinkedList<Tuple<String,String>>();
		attrs.add(createTuple(GraphML.XMLNS_TAG, GraphML.XMLNS_URL));
		writeStartTag(GraphML.GRAPHML_TAG, attrs);
		
		setIndentationLevel(indent++);
		writeStartTag(GraphML.GRAPH_TAG, getGraphAttributes());
		
		setIndentationLevel(indent);
		writeVertices();
		writeEdges();
		
		setIndentationLevel(--indent);
		writeEndTag(GraphML.GRAPH_TAG);
		
		setIndentationLevel(--indent);
		writeEndTag(GraphML.GRAPHML_TAG);
		
		close();
	}
	
	protected List<Tuple<String, String>> getGraphAttributes() {
		List<Tuple<String, String>> attrs = new LinkedList<Tuple<String,String>>();
		attrs.add(createTuple(GraphML.EDGEDEFAULT_TAG, GraphML.UNDIRECTED));
		return attrs;
	}
	
	private void writeVertices() throws IOException {
		/*
		 * Start indexing with 1 since 0 is returned if no mapping is found.
		 */
		vertexIdx = 1;
		for(Vertex v : graph.getVertices()) {
			vertexTmpIds.put(v, vertexIdx);
			writeStartTag(AbstractGraphMLReader.NODE_TAG, getVertexAttributes(v), true);
			vertexIdx++;
		}
	}
	
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attrs = new LinkedList<Tuple<String,String>>();
		attrs.add(createTuple(AbstractGraphMLReader.ID_TAG, vertexIdx));
		return attrs;
	}
	
	private void writeEdges() throws IOException {
		for(Edge e : graph.getEdges()) {
			writeStartTag(AbstractGraphMLReader.EDGE_TAG, getEdgeAttributes(e), true);
		}
	}
	
	protected List<Tuple<String, String>> getEdgeAttributes(Edge e) {
		List<Tuple<String, String>> attrs = new LinkedList<Tuple<String,String>>();
		Vertex v1 = e.getVertices().getFirst();
		Vertex v2 = e.getVertices().getSecond();
		int idx1 = vertexTmpIds.get(v1);
		int idx2 = vertexTmpIds.get(v2);
		if(idx1 > 0 && idx2 > 0) {
			attrs.add(createTuple(AbstractGraphMLReader.SOURCE_TAG, idx1));
			attrs.add(createTuple(AbstractGraphMLReader.TARGET_TAG, idx2));
		} else {
			throw new IllegalArgumentException("Orphaned edges are not allowed!");
		}
		return attrs;
	}
}
