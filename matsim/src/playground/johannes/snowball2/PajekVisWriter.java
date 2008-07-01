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
package playground.johannes.snowball2;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.utils.io.IOUtils;

import playground.johannes.socialnets.UserDataKeys;
import edu.uci.ics.jung.graph.Edge;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.utils.Pair;

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
	
	public void write(SampledGraph g, String filename) {
		try {
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			/*
			 * Vertices...
			 */
			writer.write("*Vertices ");
			writer.write(String.valueOf(g.numVertices()));
			writer.write(NEW_LINE);
			
			Map<Vertex, String> vertexIds = new HashMap<Vertex, String>();
			int counter = 1;
			for(Object v : g.getVertices()) {
				String id = (String)((Vertex)v).getUserDatum(UserDataKeys.ID);
				double x = Double.parseDouble(((String)((Vertex)v).getUserDatum(UserDataKeys.X_COORD)));
				double y = Double.parseDouble(((String)((Vertex)v).getUserDatum(UserDataKeys.Y_COORD)));
				int wave = ((SampledVertex)v).getWaveDetected();
				
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
				writer.write(getColor(wave));
				
				writer.write(NEW_LINE);
				
				vertexIds.put((Vertex) v, String.valueOf(counter));
				counter++;
			}
			/*
			 * Edges...
			 */
			writer.write("*Edges ");
			writer.write(String.valueOf(g.numEdges()));
			writer.write(NEW_LINE);
			
			for(Object e : g.getEdges()) {
				int wave = ((SampledEdge)e).getWaveSampled();
				Pair pair = ((Edge)e).getEndpoints();
				Vertex source = (Vertex) pair.getFirst();
				Vertex target = (Vertex) pair.getSecond();
				writer.write(vertexIds.get(source));
				
				writer.write(WHITESPACE);
				writer.write(vertexIds.get(target));
				writer.write(WHITESPACE);
				writer.write(DEFAULT_EDGE_WHEIGHT);
				writer.write(WHITESPACE);
				
				writer.write(COLOR);
				writer.write(WHITESPACE);
				writer.write(getColor(wave));
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
			int idx = wave.intValue();
			if(idx < waveColor.length)
				return waveColor[idx];
			else
				return waveColor[waveColor.length - 1];
		}
			
	}
}
