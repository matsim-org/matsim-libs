/* *********************************************************************** *
 * project: org.matsim.*
 * PajekVisWriter.java
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
package playground.johannes.graph.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.utils.collections.Tuple;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.io.IOUtils;

import playground.johannes.graph.Edge;
import playground.johannes.graph.Vertex;
import playground.johannes.socialnets.Ego;
import playground.johannes.socialnets.SocialNetwork;

/**
 * @author illenberger
 *
 */
public class PajekVisWriter {

	private static final Logger logger = Logger.getLogger(PajekVisWriter.class);
	
	private static final String NEW_LINE = "\r\n";
	
	private static final String WHITESPACE = " ";
	
	private static final String QUOTE = "\"";
	
	private static final String ZERO = "0";
	
	private static final String DEFAULT_EDGE_WHEIGHT = "1";
	
	private static final String COLOR = "c";
	
	private static final String FILL_COLOR = "ic";
	
//	private static final String BORDER_COLOR = "bc";
	
//	private static final String RED_COLOR = "Red";
	
	private static final String WHITE_COLOR = "White";
	
//	private static final String GREEN_COLOR = "Green";
	
	private static final String[] waveColor = new String[]{"Red","Orange","Yellow","Green"};
	
	public void write(SocialNetwork g, String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			/*
			 * Vertices...
			 */
			writer.write("*Vertices ");
			writer.write(String.valueOf(g.getVertices().size()));
			writer.write(NEW_LINE);
			
			Map<Ego, String> vertexIds = new HashMap<Ego, String>();
			int counter = 1;
			for(Ego v : g.getVertices()) {
				String id = v.getPerson().getId().toString();
				Coord c = v.getPerson().getSelectedPlan().getFirstActivity().getCoord();
				double x = c.getX();
				double y = c.getY();
				
				
				writer.write(String.valueOf(counter));
				writer.write(WHITESPACE);
				writer.write(QUOTE);
				writer.write(id);
				writer.write(QUOTE);
				writer.write(WHITESPACE);
				writer.write(String.valueOf(x));
				writer.write(WHITESPACE);
				writer.write(String.valueOf(y));
				writer.write(WHITESPACE);
				writer.write(ZERO);
				writer.write(WHITESPACE);
				writer.write(FILL_COLOR);
				writer.write(WHITESPACE);
				writer.write(getColor(v.getEdges().size()));
				
				writer.write(NEW_LINE);
				
				vertexIds.put(v, String.valueOf(counter));
				counter++;
			}
			/*
			 * Edges...
			 */
			writer.write("*Edges ");
			writer.write(String.valueOf(g.getEdges().size()));
			writer.write(NEW_LINE);
			
			for(Object e : g.getEdges()) {
				Tuple<Ego, Ego> t = (Tuple<Ego, Ego>) ((Edge)e).getVertices();				
				Vertex source = t.getFirst();
				Vertex target = t.getSecond();
				writer.write(vertexIds.get(source));
				
				writer.write(WHITESPACE);
				writer.write(vertexIds.get(target));
				writer.write(WHITESPACE);
				writer.write(DEFAULT_EDGE_WHEIGHT);
				writer.write(WHITESPACE);
				
				writer.write(COLOR);
				writer.write(WHITESPACE);
				writer.write("Black");
				writer.write(NEW_LINE);
			}
			
			writer.close();
			
		} catch (IOException e) {
			logger.fatal("Error during writing graph!", e);
		}
	}
	
	private String getColor(Integer wave) {
		if(wave == null)
			return WHITE_COLOR;
		else {
			int idx = (int)Math.floor(wave.intValue()/3.0);
			if(idx < waveColor.length)
				return waveColor[idx];
			else
				return waveColor[waveColor.length - 1];
		}
			
	}
}
