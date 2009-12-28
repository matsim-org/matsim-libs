/* *********************************************************************** *
 * project: org.matsim.*
 * PajekWriter.java
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
package playground.johannes.socialnetworks.graph.io;

import gnu.trove.TObjectIntHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.core.utils.collections.Tuple;


/**
 * @author illenberger
 *
 */
public class PajekWriter<G extends Graph, V extends Vertex, E extends Edge> {

	private static final String NEW_LINE = "\r\n";
	
	private static final String WHITESPACE = " ";
	
	private static final String QUOTE = "\"";
	
	private static final String ZERO = "0";
	
	private TObjectIntHashMap<V> pajekVertexIds;
	
	public void write(G g, String file) throws IOException {
		write(g, new EmptyAttributes(), file);
	}
	
	@SuppressWarnings("unchecked")
	public void write(G g, PajekAttributes<V, E> attrs, String file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		writer.write("*Vertices ");
		writer.write(String.valueOf(g.getVertices().size()));
		writer.write(NEW_LINE);
		
		pajekVertexIds = new TObjectIntHashMap<V>();
		int counter = 0;
		for(Vertex v : g.getVertices()) {
			counter++;
			pajekVertexIds.put((V) v, counter);
			/*
			 * Pajek id
			 */
			writer.write(String.valueOf(counter));
			writer.write(WHITESPACE);
			/*
			 * Label
			 */
			writer.write(QUOTE);
			writer.write(getVertexLabel((V) v));
			writer.write(QUOTE);
			writer.write(WHITESPACE);
			/*
			 * Coordinates
			 */
			writer.write(getVertexX((V)v));
			writer.write(WHITESPACE);
			writer.write(getVertexY((V)v));
			writer.write(WHITESPACE);
			/*
			 * Vertex shape
			 */
			writer.write(getVertexShape((V)v));
			/*
			 * Vertex attributes
			 */
			for(String att : attrs.getVertexAttributes()) {
				writer.write(WHITESPACE);
				writer.write(att);
				writer.write(WHITESPACE);
				writer.write(attrs.getVertexValue((V)v, att));
			}
			
			writer.write(NEW_LINE);
		}
		
		writer.write("*Edges ");
		writer.write(String.valueOf(g.getEdges().size()));
		writer.write(NEW_LINE);
		
		for(Edge e : g.getEdges()) {
			Tuple<V, V> t = (Tuple<V, V>) e.getVertices();				
			
			writer.write(String.valueOf(pajekVertexIds.get(t.getFirst())));
			writer.write(WHITESPACE);
			writer.write(String.valueOf(pajekVertexIds.get(t.getSecond())));
			/*
			 * Edge attributes
			 */
			for(String att : attrs.getEdgeAttributes()) {
				writer.write(WHITESPACE);
				writer.write(att);
				writer.write(WHITESPACE);
				writer.write(attrs.getEdgeValue((E) e, att));
			}
			
			writer.write(NEW_LINE);
		}
		
		writer.close();
	}
	
	protected String getVertexLabel(V v) {
		return String.valueOf(pajekVertexIds.get(v));
	}
	
	protected String getVertexX(V v) {
		return ZERO;
	}
	
	protected String getVertexY(V v) {
		return ZERO;
	}
	
	protected String getVertexShape(V v) {
		return ZERO;
	}
	
	private class EmptyAttributes implements PajekAttributes<V, E> {

		public List<String> getEdgeAttributes() {
			return new ArrayList<String>();
		}

		public String getEdgeValue(E e, String attribute) {
			return null;
		}

		public List<String> getVertexAttributes() {
			return new ArrayList<String>();
		}

		public String getVertexValue(V v, String attribute) {
			return null;
		}
		
	}
}
