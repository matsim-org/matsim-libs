/* *********************************************************************** *
 * project: org.matsim.*
 * FloodlineGenerator.java
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

package playground.gregor.snapshots.postprocessor.processors;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.PriorityQueue;

import org.matsim.utils.StringUtils;
import org.matsim.utils.geometry.Coord;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.io.IOUtils;

public class FloodlineGenerator {

	
	PriorityQueue<FloodEvent> events;
	
	public FloodlineGenerator(String filename) {
//		this.flooded = new ArrayList<CoordI>();
		this.events = new PriorityQueue<FloodEvent>();
		try {
			parse(filename);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	private void parse(String filename) throws Exception {
		BufferedReader infile = IOUtils.getBufferedReader(filename);
	
		String line = infile.readLine();
		String [] tokline;
		while (line != null) {
			tokline =  StringUtils.explode(line, '\t', 4);
			double flooding = Double.parseDouble(tokline[2]);
			double time = Double.parseDouble(tokline[3]); 
			double x = Double.parseDouble(tokline[0]);
			double y = Double.parseDouble(tokline[1]);
			Coord c = new CoordImpl(x,y);
			FloodEvent e = new FloodEvent(flooding, time,c);
			this.events.add(e);
			line = infile.readLine();
		}
		
		
	}


	public Collection<FloodEvent> getFlooded(double time) {
		Collection<FloodEvent> flooded = new ArrayList<FloodEvent>();
		if (this.events.size() > 0) {

			while (this.events.size() > 0 && this.events.peek().time <= time) {
					FloodEvent e = this.events.poll();
					flooded.add(e);
			}
		}
		
		
		return flooded;
	}
	
	
	public Collection<Coord> getAllFlodded() {
		
		ArrayList<Coord> coords = new ArrayList<Coord>();
		
		for (FloodEvent e : this.events) {
			coords.add(e.coord);
		}
		
		return coords;
	}
	
	
	
	public class FloodEvent implements Comparable {

		private double time;
		private Coord coord;
		private double flooding;
		
		public FloodEvent(double flooding, double time, Coord c) {
			this.time = time;
			this.coord = c;
			this.flooding = flooding;
		}
		
		public int compareTo(Object o) {
			if (this.time < ((FloodEvent)o).time) {
				return -1;
			} 
			
			if (this.time > ((FloodEvent)o).time) {
				return 1;
			} 
			return 0;
		}

		public double getX() {
			return this.coord.getX();
		}
		public double getY() {
			return this.coord.getY();
		}		
		public double getFlooding() {
			return this.flooding;
		}
		
		
		
	}
	
}
