/* *********************************************************************** *
 * project: org.matsim.*
 * PajekColorizer.java
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.matsim.contrib.sna.graph.Edge;
import org.matsim.contrib.sna.graph.Vertex;


/**
 * @author illenberger
 *
 */
public abstract class PajekColorizer<V extends Vertex, E extends Edge> {

	private PajekColorTable colorTable;
	
	public PajekColorizer() {
		colorTable = new PajekColorTable();
	}
	
	public abstract String getVertexFillColor(V v);
	
	public abstract String getEdgeColor(E e);
	
	public String getColor(double value) {
		float red = 1;
		float green = 1;
		float blue = 1;
		
		if(value < 0) {
			red = 0;
			green = 0;
			blue = 0;
		} else if(value > 0 && value <= 0.25) {
			red = (float) (value/0.25);
			green = 1;
			blue = 0;
		} else if (value > 0.25 && value <= 0.5) {
			red = 1;
			green = (float) (1 - (value-0.25)/0.25);
			blue = 0;
		} else if (value > 0.5 && value <= 0.75) {
			red = 1;
			green = 0;
			blue = (float) ((value - 0.5)/0.25);
		} else if (value > 0.75 && value <= 1) {
			red = 0;
			green = 0;
			blue = 1;
		}
		
		return colorTable.getColor(red, green, blue);
	}

	private static class PajekColorTable {
		
		private static final String WSPACE = " ";
		
		private List<Color> colors = new LinkedList<Color>();
		
		public PajekColorTable() {
			try {
				InputStream stream = ClassLoader.getSystemResourceAsStream("playground/johannes/socialnetworks/graph/io/PajekColorTable");
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
				String line;
				String name = null;
				float red = 0;
				float green = 0;
				float blue = 0;
				int argument;
				while((line = reader.readLine()) != null) {
					argument = 0;
					for(String token : line.split(WSPACE)) {
						if(token.length() > 0) {
							argument++;
							if(argument == 1) {
								name = token;
							} else if (argument == 2) {
								red = Float.parseFloat(token);
							} else if (argument == 3) {
								green = Float.parseFloat(token);
							} else if (argument == 4) {
								blue = Float.parseFloat(token);
								break;
							}
						}
					}
	
					Color c = new Color();
					c.red = red;
					c.green = green;
					c.blue = blue;
					c.name = name;
					colors.add(c);
				}
				
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public String getColor(float r, float g, float b) {
			double delta_min = Double.MAX_VALUE;
			Color c = null;
			for(Color color : colors) {
				double delta_red = r - color.red;
				double delta_blue = b - color.blue;
				double delta_green = g - color.green;
				double delta = Math.sqrt(Math.pow(delta_red, 2) + Math.pow(delta_green, 2) + Math.pow(delta_blue, 2));
				if(delta < delta_min) {
					delta_min = delta;
					c = color;
				}
			}
			
			return c.name;
		}
		
		private class Color {
			
			private String name;
			
			private float red;
			
			private float green;
			
			private float blue;
		}
	}

}
