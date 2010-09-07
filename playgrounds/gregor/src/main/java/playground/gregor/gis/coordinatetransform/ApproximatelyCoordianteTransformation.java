/* *********************************************************************** *
 * project: org.matsim.*
 * ApproximatelyCoordianteTransformation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.gis.coordinatetransform;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;

import com.vividsolutions.jts.geom.Envelope;

public class ApproximatelyCoordianteTransformation {

	
	private String lookupTableFile;
	
	
	private boolean init = false;


	private QuadTree<Tuple<Coord,Coord>> quadTree;
	
	double fetchRadius = 100;
	

	public ApproximatelyCoordianteTransformation(String lookupTableFile) {
		this.lookupTableFile = lookupTableFile;
	}
	
	
	public Coord getTransformed(Coord c) {
		if (!this.init) {
			init();
		}
		Collection<Tuple<Coord,Coord>> supportPoints = getSupportPoints(c);
		double lat = 0;
		double lon = 0;
		double distSum = 0;		
		for (Tuple<Coord,Coord> t : supportPoints) {
			double infl = 1 / getDist(t,c);
			lat += t.getFirst().getX() * infl;
			lon += t.getFirst().getY() * infl;
			distSum += infl;
		}

		lat /= distSum;
		lon /= distSum;
		
		return new CoordImpl(lat,lon);
	}

	
	private double getDist(Tuple<Coord, Coord> t, Coord c) {
		return Math.sqrt(Math.pow(t.getSecond().getX()-c.getX(),2) + Math.pow(t.getSecond().getY()-c.getY(),2));
	}

	

	private Collection<Tuple<Coord, Coord>> getSupportPoints(Coord c) {
		Collection<Tuple<Coord, Coord>> ret = null;
		while ((ret = this.quadTree.get(c.getX(), c.getY(), this.fetchRadius)).size() < 4) {
			this.fetchRadius += 10;
		}
		
		
		return ret;
	}


	private void init() {
		Envelope e = new Envelope();
		List<Tuple<Coord, Coord>> lines = null;
		try {
			lines = readFile(e);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} 
		this.quadTree = new QuadTree<Tuple<Coord, Coord>>(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
		for (Tuple<Coord, Coord> tuple : lines) {
			this.quadTree.put(tuple.getSecond().getX(), tuple.getSecond().getY(), tuple);
		}
		this.init = true;
	}


	private List<Tuple<Coord, Coord>> readFile(Envelope e) throws FileNotFoundException, IOException {
		List<Tuple<Coord,Coord>> ret = new ArrayList<Tuple<Coord,Coord>>();
		
		BufferedReader infile = IOUtils.getBufferedReader(this.lookupTableFile);
		String line = infile.readLine();
		while (line != null) {
			String [] tokLine = StringUtils.explode(line, '\t',4);
			double lon = Double.parseDouble(tokLine[0]);
			double lat = Double.parseDouble(tokLine[1]);
			Coord wgs84 = new CoordImpl(lon,lat);
			
			double x = Double.parseDouble(tokLine[2]);
			double y = Double.parseDouble(tokLine[3]);
			e.expandToInclude(x, y);
			Coord noIdea = new CoordImpl(x,y);
			Tuple<Coord,Coord> tuple = new Tuple<Coord, Coord>(wgs84, noIdea);
			ret.add(tuple);
			line = infile.readLine();
		}
		return ret;
	}
	
	
	public static void main(String [] args) {
		
		String f = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/de/prognose_2025/orig/netze/coordinateTransformationLookupTable.csv";
		ApproximatelyCoordianteTransformation transform = new ApproximatelyCoordianteTransformation(f);
		Coord sleepyHollowWgs84 = new CoordImpl(-4.632836,53.309088);
		Coord sleepyHollowNoIdea = new CoordImpl(-1005.17457059858,745.710112755592);
		Coord test = transform.getTransformed(sleepyHollowNoIdea);
		System.out.println("lat deviation:" + (sleepyHollowWgs84.getX() - test.getX()) + " long deviation:" + (sleepyHollowWgs84.getY() - test.getY()));
		

		
	}
}
