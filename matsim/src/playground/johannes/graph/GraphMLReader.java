/* *********************************************************************** *
 * project: org.matsim.*
 * GraphMLReader.java
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
package playground.johannes.graph;

import gnu.trove.TIntObjectHashMap;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author illenberger
 *
 */
public class GraphMLReader {
	
	private static final Logger logger = Logger.getLogger(GraphMLReader.class);

	private static final String ID_TAG = "id";
	
	private static final String SOURCE_TAG = "source";
	
	private static final String TARGET_TAG = "target";
	
	private TIntObjectHashMap<SparseVertex> vertexMappings;
	
	protected SparseGraph graph;
	
	private int numVertex;
	
	private int numEdge;
	
	public SparseGraph readGraph(String file) {
		XMLHandler handler = new XMLHandler();
		vertexMappings = new TIntObjectHashMap<SparseVertex>();
		numVertex = 0;
		numEdge = 0;
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
		SAXParser parser;
		try {
			parser = factory.newSAXParser();
		
		XMLReader reader = parser.getXMLReader();
		reader.setContentHandler(handler);
		// Ignore the DTD declaration
//		reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
//		reader.setFeature("http://xml.org/sax/features/validation", false);
		reader.parse(file);
		printProgress();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return graph;
	}
	
	protected SparseGraph newGraph(Attributes attrs) {
		return new SparseGraph();
	}
	
	protected SparseVertex newNode(Attributes attrs) {
		SparseVertex v = graph.addVertex();
		numVertex++;
		int id = Integer.parseInt(attrs.getValue(ID_TAG));
		vertexMappings.put(id, v);
		
		if(numVertex % 100000 == 0)
			printProgress();
		
		return v;
	}
	
	protected SparseEdge newEdge(Attributes attrs) {
		int id1 = Integer.parseInt(attrs.getValue(SOURCE_TAG));
		int id2 = Integer.parseInt(attrs.getValue(TARGET_TAG));
		SparseVertex v1 = vertexMappings.get(id1);
		SparseVertex v2 = vertexMappings.get(id2);
		if(v1 != null && v2 != null) {
			numEdge++;
			if(numEdge % 100000 == 0)
				printProgress();
			return graph.addEdge(v1, v2);
		} else {
			throw new IllegalArgumentException("A vertex must not be null!");
		}
	}
	
	private void printProgress() {
		logger.info(String.format("Loading graph... %1$s vertices, %2$s edges.", numVertex, numEdge));
	}
	
	private class XMLHandler extends DefaultHandler {

		@Override
		public void startElement(String uri, String localName, String name,
				Attributes atts) throws SAXException {
			if(NODE_TAG.equalsIgnoreCase(name)) {
				newNode(atts);
			} else if(EDGE_TAG.equalsIgnoreCase(name)) {
				newEdge(atts);
			} else if(GRAPH_TAG.equalsIgnoreCase(name)) { 
				graph = newGraph(atts);
			}
		}

		private static final String GRAPH_TAG = "graph";
		
		private static final String NODE_TAG = "node";
		
		private static final String EDGE_TAG = "edge";
		
//		@Override
//		public void endTag(String name, String content, Stack<String> context) {
//			// TODO Auto-generated method stub
//			
//		}
//
//		@Override
//		public void startTag(String name, Attributes atts, Stack<String> context) {
//			if(NODE_TAG.equalsIgnoreCase(name)) {
//				newNode(atts);
//			} else if(EDGE_TAG.equalsIgnoreCase(name)) {
//				newEdge(atts);
//			} else if(GRAPH_TAG.equalsIgnoreCase(name)) { 
//				newGraph(atts);
//			}
//		}
		
	}
	
	public static void main(String args[]) {
		Gbl.startMeasurement();
		SparseGraph g = new GraphMLReader().readGraph(args[0]);
		Gbl.printElapsedTime();
		System.out.println(g.toString());
		Gbl.printMemoryUsage();
	}
}
