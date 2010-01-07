/* *********************************************************************** *
 * project: org.matsim.*
 * Population2SpatialGraph.java
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
package playground.johannes.socialnetworks.graph.spatial.io;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraphBuilder;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * @author illenberger
 *
 */
public class Population2SpatialGraph {
	
	private static final Logger logger = Logger.getLogger(Population2SpatialGraph.class);
	
	private static final String PERSON_TAG = "person";
	
//	private static final String PLAN_TAG = "plan";
	
	private static final String ACT_TAG = "act";
	
	private static final String X_KEY = "x";
	
	private static final String Y_KEY = "y";

	private SpatialSparseGraph graph;
	
	private SpatialSparseGraphBuilder builder;
	
	private int numVertex;
	
	private final GeometryFactory geometryFactory = new GeometryFactory();
	
	private final CoordinateReferenceSystem crs; 
	
	public Population2SpatialGraph(CoordinateReferenceSystem crs) {
		this.crs = crs;
//		geometryFactory = new GeometryFactory(new PrecisionModel(), SRID);
	}
	
	public SpatialSparseGraph read(String filename) {
		
		numVertex = 0;
		
		SAXParserFactory saxfactory = SAXParserFactory.newInstance();
		saxfactory.setValidating(false);
		saxfactory.setNamespaceAware(false);
		
		SAXParser parser;
		try {
			XMLHandler handler = new XMLHandler();
			parser = saxfactory.newSAXParser();

			XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(handler);
			
			
			builder = new SpatialSparseGraphBuilder(crs);
			graph = builder.createGraph();
			
			reader.parse(new InputSource(IOUtils.getBufferedReader(filename)));
			printProgress();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return graph;
	}
	
	private void addVertex(double x, double y) {
		if(builder.addVertex(graph, geometryFactory.createPoint(new Coordinate(x, y))) == null) {
			logger.warn("Failed to add vertex!");
		} else
			numVertex++;
		
		if(numVertex % 1000 == 0)
			printProgress();
	}
	
	private void printProgress() {
		logger.info(String.format("Loading graph... %1$s vertices.", numVertex));
	}
	
	private class XMLHandler extends DefaultHandler {
	
		private boolean ignore = false;
		
		@Override
		public void startElement(String uri, String localName, String name,
				Attributes atts) throws SAXException {
			if(!ignore && ACT_TAG.equalsIgnoreCase(name)) {	
				ignore = true;
				
				String x = atts.getValue(X_KEY);
				String y = atts.getValue(Y_KEY);
				if(x != null && y != null) {
					addVertex(Double.parseDouble(x), Double.parseDouble(y));
				} else {
					logger.warn("Cannot create vertex because either x or y coordinate is missing!");
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String name)
				throws SAXException {
			if(PERSON_TAG.equalsIgnoreCase(name))
				ignore = false;
		}
	}
}
