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
package playground.johannes.socialnetworks.graph.io;

import gnu.trove.TObjectIntHashMap;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.io.GraphML;
import org.matsim.contrib.sna.graph.io.SparseGraphMLReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;



/**
 * Basic class for writing graph objects into GraphML files
 * (http://graphml.graphdrawing.org/). This class treats graphs as undirected
 * and unweighted.
 * 
 * @author illenberger
 * 
 */
public class GraphMLWriter extends MatsimXmlWriter {

	private Graph graph;
	
	private TObjectIntHashMap<Vertex> vertexTmpIds;
	
	private int vertexIdx;

	/**
	 * Writes <tt>graph</tt> into a GraphML file at <tt>filename</tt>.
	 * 
	 * @param graph
	 *            the graph to be written.
	 * @param filename
	 *            the filename for the GraphML file.
	 * @throws IOException
	 */
	public void write(Graph graph, String filename) throws IOException {
		this.graph = graph;
		openFile(filename);
		setPrettyPrint(true);
		
		vertexTmpIds = new TObjectIntHashMap<Vertex>();
		
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
	
	/**
	 * Returns the currently written graph.
	 * 
	 * @return the currently written graph.
	 */
	protected Graph getGraph() {
		return graph;
	}
	
	/**
	 * Returns a list of graph attributes stored in String-String-tuples. The
	 * returned list has one entry:
	 * <ul>
	 * <li>{@link GraphML#EDGEDEFAULT_TAG} - {@link GraphML#UNDIRECTED}
	 * </ul>
	 * 
	 * @return a list of graph attributes stored in String-String-tuples.
	 */
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
			writeStartTag(GraphML.NODE_TAG, getVertexAttributes(v), true);
			vertexIdx++;
		}
	}
	
	/**
	 * Returns a list of vertex attributes stored in String-String-tuples. The
	 * returned list contains one entry:
	 * <ul>
	 * <li>{@link GraphML#ID_TAG} - <tt>vertexIdx</tt>
	 * </ul>
	 * 
	 * @param v
	 *            the vertex the attributes are to be returned.
	 * @return a list of vertex attributes.
	 */
	protected List<Tuple<String, String>> getVertexAttributes(Vertex v) {
		List<Tuple<String, String>> attrs = new LinkedList<Tuple<String,String>>();
		attrs.add(createTuple(GraphML.ID_TAG, vertexIdx));
		return attrs;
	}
	
	private void writeEdges() throws IOException {
		for(Edge e : graph.getEdges()) {
			writeStartTag(GraphML.EDGE_TAG, getEdgeAttributes(e), true);
		}
	}
	
	/**
	 * Returns a list of edge attributes stored in String-String-tuples. The
	 * returned list contains two entries:
	 * <ul>
	 * <li> {@link GraphML#SOURCE_TAG} - <tt>vertexIdx</tt>
	 * <li> {@link GraphML#TARGET_TAG} - <tt>vertexIdx</tt>
	 * </ul>
	 * 
	 * @param e
	 *            the edge the attributes are to be returned.
	 * @return a list of edge attributes.
	 */
	protected List<Tuple<String, String>> getEdgeAttributes(Edge e) {
		List<Tuple<String, String>> attrs = new LinkedList<Tuple<String,String>>();
		Vertex v1 = e.getVertices().getFirst();
		Vertex v2 = e.getVertices().getSecond();
		int idx1 = vertexTmpIds.get(v1);
		int idx2 = vertexTmpIds.get(v2);
		if(idx1 > 0 && idx2 > 0) {
			attrs.add(createTuple(GraphML.SOURCE_TAG, idx1));
			attrs.add(createTuple(GraphML.TARGET_TAG, idx2));
		} else {
			throw new IllegalArgumentException("Orphaned edges are not allowed!");
		}
		return attrs;
	}
	
	public static void main(String[] args) throws IOException {
		SparseGraphMLReader reader = new SparseGraphMLReader();
		Graph g = reader.readGraph(args[0]);
		GraphMLWriter writer = new GraphMLWriter();
		writer.write(g, args[1]);
	}
}
