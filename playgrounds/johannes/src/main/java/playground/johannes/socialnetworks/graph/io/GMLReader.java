/* *********************************************************************** *
 * project: org.matsim.*
 * GMLReader.java
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
package playground.johannes.socialnetworks.graph.io;

import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.GraphBuilder;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.io.GraphMLWriter;


/**
 * @author illenberger
 *
 */
public class GMLReader {

	private static final Logger logger = Logger.getLogger(GMLReader.class);
	
	private static final String GRAPH_KEY = "graph";
	
	private static final String NODE_KEY = "node";
	
	private static final String EDGE_KEY = "edge";
	
	private static final String ID_KEY = "id";
	
	private static final String SOURCE_KEY = "source";
	
	private static final String TARGET_KEY = "target";
	
	public <G extends Graph, V extends Vertex, E extends Edge> Graph read(String file, GraphBuilder<G, V, E> builder) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		logger.info("Parsing GML file...");
		return build(builder, parse(reader));
	}
	
	private List<Attribute> parse(BufferedReader reader) throws IOException {
		List<Attribute> attributes = new ArrayList<Attribute>();
		StringBuilder builder = new StringBuilder();
		int character = -1;
		boolean whitespace = false;
		/*
		 * advance to the first key
		 */
		while((character = reader.read()) > -1) {
			if(!isWhitespace(character)) {
				break;
			}
		}
		/*
		 * parse data
		 */
		Attribute attribute = null;
		while(character > -1) {
			if(isWhitespace(character)) {
				whitespace = true;
			} else {
				if(whitespace) {
					/*
					 * previous key was a whitespace, so we have either a key or a value
					 */
					whitespace = false;
					/*
					 * if we have not already create an attribute it is a key
					 */
					if (attribute == null) {
						attribute = new Attribute();
						attribute.setKey(builder.toString());
					} else {
						if(!attribute.isNode) {
							/*
							 * this attribute is not a node, set the string value
							 */
							attribute.setValue(builder.toString());
						}
						attributes.add(attribute);
						attribute = null;
					}
					builder = new StringBuilder();
				}
				
				if(isListBeginning(character)) {
					/*
					 * this attribute is a node
					 */
					attribute.setIsNode(true);
					attribute.setValue(parse(reader));
					
				} else if(isListEnding(character)) {
					return attributes;				
					
				} else if(isQuotes(character)) {
					builder.append(parseText(reader));
					
				} else {
					builder.appendCodePoint(character);
				}
			}
			
			character = reader.read();
		}
		/*
		 * add the last attribute
		 */
		if(!attribute.isNode) {
			attribute.setValue(builder.toString());
		}
		attributes.add(attribute);
		
		return attributes;
	}
	
	private boolean isWhitespace(int character) {
		if(character == 32 || character == 10)
			return true;
		else
			return false;
	}
	
	private boolean isListBeginning(int character) {
		if(character == 91)
			return true;
		else
			return false;
	}
	
	private boolean isListEnding(int character) {
		if(character == 93)
			return true;
		else
			return false;
	}
	
	private boolean isQuotes(int character) {
		if(character == 34)
			return true;
		else
			return false;
	}
	
	private String parseText(BufferedReader reader) throws IOException {
		int character;
		StringBuilder builder = new StringBuilder();
		while((character = reader.read()) > -1) {
			if(!isQuotes(character)) {
				builder.appendCodePoint(character);
			} else
				break;
		}
		
		return builder.toString();
	}
	
	private <G extends Graph, V extends Vertex, E extends Edge> Graph build(GraphBuilder<G, V, E> builder, List<Attribute> data) {
		logger.info("Building graph...");
		G graph = null;
		int doubleEdges = 0;
		
		for(Attribute rootElements : data) {
			if(rootElements.getKey().equalsIgnoreCase(GRAPH_KEY)) {
				graph = builder.createGraph();
				TIntObjectHashMap<V> nodes = new TIntObjectHashMap<V>();
				List<Attribute> elements = rootElements.getAttributes();
				/*
				 * create vertices
				 */
				for(Attribute element : elements) {
					if(element.getKey().equalsIgnoreCase(NODE_KEY)) {
						List<Attribute> attributes = element.getAttributes();
						/*
						 * search for id value
						 */
						for(Attribute attribute : attributes) {
							if(attribute.getKey().equalsIgnoreCase(ID_KEY)) {
								V vertex = builder.addVertex(graph);
								nodes.put(Integer.parseInt(attribute.getValue()), vertex);
								
								if(graph.getVertices().size() % 1000 == 0)
									logger.info(String.format("Building graph... %1$s vertices.", graph.getVertices().size()));
								break;
							}
						}
					}
				}
				/*
				 * create edges
				 */
				for(Attribute element : elements) {
					if(element.getKey().equalsIgnoreCase(EDGE_KEY)) {
						List<Attribute> attributes = element.getAttributes();
						String source = null;
						String target = null;
						/*
						 * search for source and target values
						 */
						for(Attribute attribute : attributes) {
							if(attribute.getKey().equalsIgnoreCase(SOURCE_KEY)) {
								source = attribute.getValue();
							} else if(attribute.getKey().equalsIgnoreCase(TARGET_KEY)) {
								target = attribute.getValue();
							}
						}
						V v1 = nodes.get(Integer.parseInt(source));
						V v2 = nodes.get(Integer.parseInt(target));
						if(v1 != null && v2 != null) {
							if(builder.addEdge(graph, v1, v2) != null) {
								if(graph.getEdges().size() % 1000 == 0)
									logger.info(String.format("Building graph... %1$s edges.", graph.getEdges().size()));
							} else {
								doubleEdges++;
							}
						}
					}
				}
				
				break;
			}
		}
		
		logger.info(String.format("Built graph (%1$s vertices, %2$s edges).", graph.getVertices().size(), graph.getEdges().size()));
		if(doubleEdges > 0) {
			logger.warn(String.format("Skipped %1$s doubled edges.", doubleEdges));
		}
		return graph;
	}
	
	private static class Attribute {
		
		private String key;
		
		private Object value;
		
		private boolean isNode;
		
		public String getKey() {
			return key;
		}
		
		public String getValue() {
			if(isNode)
				return null;
			else
				return (String) value;
		}
		
		@SuppressWarnings("unchecked")
		public List<Attribute> getAttributes() {
			if(isNode)
				return (List<Attribute>) value;
			else
				return null;
		}
		
		public void setKey(String key) {
			this.key = key;
		}
		
		public void setValue(Object value) {
			this.value = value;
		}
		
		public void setIsNode(boolean isNode) {
			this.isNode = isNode;
		}
	}
	
	public static void main(String args[]) throws IOException {
		GMLReader reader = new GMLReader();
		Graph graph = reader.read("/Users/jillenberger/Downloads/polblogs/polblogs.gml", new SparseGraphBuilder());
		System.out.println(graph.getVertices().size() + " vertices");
		GraphMLWriter writer = new GraphMLWriter();
		writer.write(graph, "/Users/jillenberger/Downloads/polblogs/polblogs.graphml");
	}
}
