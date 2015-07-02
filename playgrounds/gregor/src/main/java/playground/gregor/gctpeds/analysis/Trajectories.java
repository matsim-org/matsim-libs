/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.gregor.gctpeds.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class Trajectories {
	
	private static final Logger log = Logger.getLogger(Trajectories.class);

	List<Coordinate> coords = new ArrayList<Coordinate>();
	
	double minX = Double.POSITIVE_INFINITY;
	double minY = Double.POSITIVE_INFINITY;
	
	public void addCoordinate(Id<Person> id, double time, double x, double y, double vx, double vy) {
		Coordinate c = new Coordinate();
		c.id = Integer.parseInt(id.toString());
		c.time = time;
		c.x = x;
		if (x < minX) {
			minX = x;
			minY = y;
		}
//		if (y < minY) {
//		}
		c.y = y;
		c.vx = vx;
		c.vy = vy;
		coords.add(c);
	}
	
	public void dumpAsJuPedSimTrajectories(double offsetX,double offsetY,double fromTime, double toTime, String fileName) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(fileName)));
		List<Coordinate> padded = padCoords();
		Comparator<? super Coordinate> comp = new JuPedSimCoordinateComparator();
		Collections.sort(padded,comp);
//		System.out.println("#ID\tTIME\tX\tY\tZ");
		int id = -1;
		int mId = Integer.MIN_VALUE;
		bw.append("#ID\tTIME\tX\tY\tZ\n");
		for (Coordinate c : padded) {
			if (c.time < fromTime*10 || c.time >= toTime*10) {
				continue;
			}
			if (mId != c.id) {
				id++;
				mId = c.id;
			}
			
			StringBuffer bf = new StringBuffer();
			bf.append(id);
			bf.append('\t');
			bf.append(c.time);
			bf.append('\t');
			bf.append(c.x-offsetX);
			bf.append('\t');
			bf.append(c.y-offsetY);			
			bf.append('\t');
			bf.append('0');
			bf.append('\n');
//			System.out.println(bf.toString());
			bw.append(bf);
		}
		bw.close();
	}

	private List<Coordinate> padCoords() {
		List<Coordinate> paddedCoords = new ArrayList<>();
		Collections.sort(coords,new TimeComp());
		int dummyId = -1;
		for (Coordinate c : coords) {
			int ct = getFrameNr(c.time);
			c.time = ct;
			if (paddedCoords.size() > 0) {
				Coordinate prev = paddedCoords.get(paddedCoords.size()-1);
				dummyId--;
				for (int time = (int) (prev.time+1); time < ct; time++){
					Coordinate dummy = new Coordinate();
					dummy.id = dummyId;
					dummy.time = time;
					dummy.x = minX+0.1;
					dummy.y = minY+0.1;
					paddedCoords.add(dummy);
				}
			}
			paddedCoords.add(c);
		}
		return paddedCoords;
	}

	private int getFrameNr(double d) {
		return (int)Math.round((d*10));
	}

	@Override
	public String toString() {
		return Integer.toString(coords.size());
	}
	
	private static final class TimeComp implements Comparator<Coordinate> {

		@Override
		public int compare(Coordinate o1, Coordinate o2) {
			if (o1.time < o2.time) {
				return -1;
			}
			if (o1.time > o2.time) {
				return 1;
			}
			return 0;
		}
		
	}
	
	private static final class JuPedSimCoordinateComparator implements Comparator<Coordinate> {

		@Override
		public int compare(Coordinate o1, Coordinate o2) {
			if (o1.id < o2.id) {
				return -1;
			}
			if (o1.id > o2.id) {
				return 1;
			}
			if (o1.time < o2.time) {
				return -1;
			}
			if (o1.time > o2.time) {
				return 1;
			}
//			throw new RuntimeException("Found two coordinate with identical id and time.");
			log.warn("Found two coordinate with identical id and time." + o1.time);
			return 0;
		}
		
	}
	
	public static class Coordinate {

		public double vy;
		public double vx;
		public double y;
		public double x;
		public double time;
		public int id;
		
	}

}
